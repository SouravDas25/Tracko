import 'package:tracko/Utils/enums.dart';
import 'package:tracko/exceptions/CategoryIsUsedByTransactionExceptions.dart';
import 'package:tracko/models/category.dart';
import 'package:tracko/models/transaction.dart';
import 'package:tracko/scratch/ChartUtil.dart';
import 'package:tracko/repositories/category_repository.dart';
import 'package:dio/dio.dart';

class CategoryController {
  static Future<List<Category>> getAllCategories() async {
    final repo = CategoryRepository();
    return await repo.getAll();
  }

  static Future<Category> findById(int id) async {
    final repo = CategoryRepository();
    return await repo.getById(id);
  }

  static Future<Category> getDefaultCategory() async {
    final repo = CategoryRepository();
    return await repo.getById(1);
  }

  static Future<List<ChartEntry>> getPieChartData() async {
    List<ChartEntry> data = [];
//    print("create Data Called.");
    final repo = CategoryRepository();
    List<Category> categories = await repo.getAll();
    for (Category category in categories) {
      // TODO: Replace with backend totals when Transaction repository is added
      double amount = 0.0;
//      print("amount : "+amount.toString());
      if (amount > 0.0) {
        data.add(ChartEntry(category.id ?? 0, category.name, amount.toInt()));
      }
    }
    data = data.reversed.toList();
//    print("data : " + (data.toString()));
    return data.length > 4 ? data.sublist(0, 4) : data;
  }

  static deleteCategory(int id) async {
    final repo = CategoryRepository();
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
    final repo = CategoryRepository();
    return await repo.findOrCreateByName(name);
  }
}
