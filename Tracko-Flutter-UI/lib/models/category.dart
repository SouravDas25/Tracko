import 'dart:async';

import 'package:tracko/models/transaction.dart';
import 'package:tracko/orm_stub.dart';
import 'package:tracko/models/user.dart';
// import 'package:jaguar_orm/jaguar_orm.dart'; // Removed - migrating to plain sqflite
// import 'package:jaguar_query/jaguar_query.dart'; // Removed - migrating to plain sqflite

// ORM code generation removed

class Category {
  int? id;
  String name = '';
  int? userId;
  List<Transaction> transactions = [];

  Category();

  Category.make(this.id, this.name, this.userId);

  @override
  String toString() {
    return 'Category{id: $id, name: $name, userId: $userId, transactions: $transactions}';
  }

  static int defaultCategoryId() {
    return 1;
  }
}

@GenBean()
class CategoryBean extends Bean<Category> {
  CategoryBean(dynamic adapter)
      : super(adapter is Adapter ? adapter : Adapter(adapter));

  final String tableName = 'categories';

  @override
  TransactionBean get transactionBean => new TransactionBean(adapter);

  @override
  UserBean get userBean => new UserBean(adapter);

  Future<List<Category>> getAll() async {
    final rows = await adapter.db
        .rawQuery('SELECT id, name, userId FROM $tableName ORDER BY id ASC');
    return rows.map((m) {
      final c = Category();
      c.id = (m['id'] as int?);
      c.name = (m['name'] as String?) ?? '';
      c.userId = (m['userId'] as int?);
      return c;
    }).toList();
  }

  Future<Category> find(int id) async {
    final rows = await adapter.db
        .rawQuery('SELECT id, name, userId FROM $tableName WHERE id = ?', [id]);
    if (rows.isEmpty) {
      final c = Category();
      c.id = id;
      c.name = '';
      c.userId = 1;
      return c;
    }
    final m = rows.first;
    final c = Category();
    c.id = (m['id'] as int?);
    c.name = (m['name'] as String?) ?? '';
    c.userId = (m['userId'] as int?);
    return c;
  }

  Future<int?> insert(Category c) async {
    final id = await adapter.db.insert(tableName, {
      'name': c.name,
      'userId': c.userId ?? 1,
    });
    return id;
  }

  Future<void> remove(int id) async {
    await adapter.db.delete(tableName, where: 'id = ?', whereArgs: [id]);
  }
}
