import 'package:tracko/Utils/enums.dart';
import 'package:tracko/exceptions/CategoryIsUsedByTransactionExceptions.dart';
import 'package:tracko/models/category.dart';
import 'package:tracko/models/transaction.dart';
import 'package:tracko/Utils/ChartUtil.dart';
import 'package:tracko/repositories/category_repository.dart';
import 'package:tracko/repositories/transaction_repository.dart';
import 'package:tracko/services/SessionService.dart';
import 'package:tracko/Utils/SettingUtil.dart';
import 'package:dio/dio.dart';
import 'package:tracko/di/di.dart';

class CategoryController {
  static Future<List<Category>> getAllCategories() async {
    final repo = sl<CategoryRepository>();
    return await repo.getAll();
  }

  static Future<Category> findById(int id) async {
    final repo = sl<CategoryRepository>();
    return await repo.getById(id);
  }

  static Future<Category> getDefaultCategory() async {
    final repo = sl<CategoryRepository>();
    return await repo.getById(1);
  }

  static Future<List<ChartEntry>> getPieChartData() async {
    final txRepo = sl<TransactionRepository>();
    final catRepo = sl<CategoryRepository>();

    final begin = SettingUtil.currentMonth;

    // Backend authorizes from JWT; userId param is not required by API but retained in signature.
    final txs = await txRepo.getAll(
      month: begin.month,
      year: begin.year,
      page: 0,
      size: 2000,
      expand: false,
    );

    final byCategory = <int, double>{};
    for (final t in txs) {
      // Pie chart should represent expenses by category.
      if (t.transactionType != TransactionType.DEBIT) continue;
      final cid = t.categoryId;
      if (cid == 0) continue;
      byCategory[cid] = (byCategory[cid] ?? 0.0) + (t.amount);
    }

    if (byCategory.isEmpty) return <ChartEntry>[];

    final categories = await catRepo.getAll();
    final names = <int, String>{};
    for (final c in categories) {
      final id = c.id ?? 0;
      if (id != 0) names[id] = c.name;
    }

    final data = <ChartEntry>[];
    byCategory.forEach((cid, amount) {
      if (amount <= 0) return;
      data.add(ChartEntry(cid, names[cid] ?? 'Category $cid', amount.round()));
    });

    data.sort((a, b) => b.value.compareTo(a.value));
    return data.length > 4 ? data.sublist(0, 4) : data;
  }

  static deleteCategory(int id) async {
    final repo = sl<CategoryRepository>();
    try {
      await repo.delete(id);
    } on DioException catch (e) {
      final status = e.response?.statusCode ?? 0;
      if (status == 400 || status == 409) {
        throw CategoryIsUsedByTransactionExceptions();
      }
      rethrow;
    }
  }

  static Future<Category> findOrCreateByName(String name) async {
    final repo = sl<CategoryRepository>();
    return await repo.findOrCreateByName(name);
  }
}
