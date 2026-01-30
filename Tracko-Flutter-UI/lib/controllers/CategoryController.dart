import 'package:Tracko/Utils/DatabaseUtil.dart';
import 'package:Tracko/Utils/enums.dart';
import 'package:Tracko/exceptions/CategoryIsUsedByTransactionExceptions.dart';
import 'package:Tracko/models/category.dart';
import 'package:Tracko/models/transaction.dart';
import 'package:Tracko/scratch/ChartUtil.dart';

class CategoryController {
  static Future<List<Category>> getAllCategories() async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    return await CategoryBean(adapter).getAll();
  }

  static Future<Category> findById(int id) async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    return await CategoryBean(adapter).find(id);
  }

  static Future<Category> getDefaultCategory() async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    return await CategoryBean(adapter).find(1);
  }

  static Future<List<ChartEntry>> getPieChartData() async {
    List<ChartEntry> data = [];
//    print("create Data Called.");
    var adapter = await DatabaseUtil.getAdapter();
    CategoryBean categoryBean = new CategoryBean(adapter);
    TransactionBean transactionBean = new TransactionBean(adapter);
    List<Category> categories = await categoryBean.getAll();
    for (Category category in categories) {
      var tmp = await transactionBean.findByCategory(category.id ?? 0);
      tmp.retainWhere(
              (element) => element.transactionType == TransactionType.DEBIT);
      double amount = tmp.fold(0.0,
              (double previous, Transaction element) =>
          previous + element.amount);
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
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    CategoryBean categoryBean = CategoryBean(adapter);
    TransactionBean transactionBean = TransactionBean(adapter);
    List<Transaction> transactions = await transactionBean.findByCategory(id);
    if (transactions.length > 0) {
      throw CategoryIsUsedByTransactionExceptions();
    }
    await categoryBean.remove(id);
  }

  static Future<Category> findOrCreateByName(String name) async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    CategoryBean categoryBean = new CategoryBean(adapter);
    List<Category> categories = await categoryBean.getAll();
    Category? category;
    for (Category cat in categories) {
      if (cat.name.toLowerCase().compareTo(name.toLowerCase()) == 0) {
        category = cat;
      }
    }
    if (category == null || category.id == null) {
      category = new Category();
      category.name = name;
      category.userId = 1;
      category.id = await categoryBean.insert(category);
    }
    return category;
  }
}
