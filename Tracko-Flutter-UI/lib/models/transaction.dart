import 'dart:async';

import 'package:tracko/Utils/enums.dart';
import 'package:tracko/dtos/TrackoContact.dart';
import 'package:tracko/models/account.dart';
import 'package:tracko/models/category.dart';
import 'package:tracko/models/split.dart';
import 'package:tracko/orm_stub.dart';
// import 'package:jaguar_orm/jaguar_orm.dart'; // Removed - migrating to plain sqflite
// import 'package:jaguar_query/jaguar_query.dart'; // Removed - migrating to plain sqflite
// import 'package:jaguar_query_sqflite/jaguar_query_sqflite.dart'; // Removed - migrating to plain sqflite

// ORM code generation removed

class Transaction {
  int? id;
  int transactionType = TransactionType.DEBIT;
  String name = '';
  String comments = '';
  DateTime date = DateTime.now();
  double amount = 0.0;
  int isCountable = 1;
  int accountId = 1;
  int categoryId = 1;
  List<Split> splits = [];
  Set<TrakoContact> contacts = {};
  Category? category;

  Transaction();

  Transaction.make(this.id, this.name, this.comments, this.date,
      this.amount, this.accountId, this.categoryId);

  @override
  String toString() {
    return 'Transaction{id: $id, transactionType: ${TransactionType.stringify(
        transactionType)}, name: $name, comments: $comments, date: $date, amount: $amount, accountId: $accountId, categoryId: $categoryId, category: ${category
        .toString()}}';
  }

  Transaction.defaultObject() {
    this.transactionType = TransactionType.DEBIT;
    this.comments = "";
    this.amount = 0.0;
    this.date = DateTime.now();
    this.name = "Item";
    this.accountId = Account.defaultAccountId();
    this.categoryId = Category.defaultCategoryId();
  }

}

@GenBean()
class TransactionBean extends Bean<Transaction> {
  final String tableName = 'transactions';

  TransactionBean(dynamic adapter)
      : super(adapter is Adapter ? adapter : Adapter(adapter));

  @override
  AccountBean get accountBean => AccountBean(adapter);

  @override
  CategoryBean get categoryBean => CategoryBean(adapter);

  @override
  SplitBean get splitBean => SplitBean(adapter);

  // Field accessors for SQL query building (used by SplitController)
  _Field get id => _Field('id');
  _Field get date => _Field('date');
  _Field get accountId => _Field('accountId');
  _Field get transactionType => _Field('transactionType');

  Transaction fromMap(Map<String, dynamic> m) {
    final t = Transaction();
    t.id = (m['id'] as int?);
    t.transactionType = (m['transactionType'] as int?) ?? 0;
    t.name = (m['name'] as String?) ?? '';
    t.comments = (m['comments'] as String?) ?? '';
    final dateRaw = m['date'];
    if (dateRaw is String) {
      t.date = DateTime.tryParse(dateRaw) ?? DateTime.now();
    } else if (dateRaw is int) {
      t.date = DateTime.fromMillisecondsSinceEpoch(dateRaw);
    }
    t.amount = (m['amount'] as num?)?.toDouble() ?? 0.0;
    t.isCountable = (m['isCountable'] as int?) ?? 1;
    t.accountId = (m['accountId'] as int?) ?? 0;
    t.categoryId = (m['categoryId'] as int?) ?? 0;
    return t;
  }

  Future<List<Transaction>> findByCategory(int categoryId) async {
    final rows = await adapter.db
        .rawQuery('SELECT * FROM $tableName WHERE categoryId = ?', [categoryId]);
    return rows.map((m) => fromMap(m)).toList();
  }

  Future<Transaction?> find(int transactionId) async {
    final rows = await adapter.db
        .rawQuery('SELECT * FROM $tableName WHERE id = ?', [transactionId]);
    if (rows.isEmpty) return null;
    return fromMap(rows.first);
  }

  // Finder support for query building
  _TransactionFinder get finder => _TransactionFinder(this);

  Future<List<Transaction>> findMany(_TransactionFinder finder) async {
    final sql = finder.toSql(tableName);
    final rows = await adapter.db.rawQuery(sql);
    return rows.map((m) => fromMap(m)).toList();
  }

  Future<int> remove(int transactionId) async {
    await adapter.db.delete(tableName, where: 'id = ?', whereArgs: [transactionId]);
    return transactionId;
  }

  Future<void> removeAll() async {
    await adapter.db.delete(tableName);
  }

  Future<int?> upsert(Transaction t) async {
    if (t.id != null) {
      await adapter.db.update(
        tableName,
        {
          'transactionType': t.transactionType,
          'name': t.name,
          'comments': t.comments,
          'date': t.date.toIso8601String(),
          'amount': t.amount,
          'isCountable': t.isCountable,
          'accountId': t.accountId,
          'categoryId': t.categoryId,
        },
        where: 'id = ?',
        whereArgs: [t.id],
      );
      return t.id;
    } else {
      final id = await adapter.db.insert(tableName, {
        'transactionType': t.transactionType,
        'name': t.name,
        'comments': t.comments,
        'date': t.date.toIso8601String(),
        'amount': t.amount,
        'isCountable': t.isCountable,
        'accountId': t.accountId,
        'categoryId': t.categoryId,
      });
      return id;
    }
  }

}

// Helper class for field name access in SQL queries
class _Field {
  final String name;
  _Field(this.name);

  _FieldCondition gtEq(DateTime value) {
    return _FieldCondition('$name >= \'${value.toIso8601String()}\'');
  }

  _FieldCondition lt(DateTime value) {
    return _FieldCondition('$name < \'${value.toIso8601String()}\'');
  }

  _FieldCondition eq(dynamic value) {
    if (value is String) {
      return _FieldCondition('$name = \'$value\'');
    }
    return _FieldCondition('$name = $value');
  }
}

class _FieldCondition {
  final String sql;
  _FieldCondition(this.sql);
}

// Simple finder for query building
class _TransactionFinder {
  final TransactionBean bean;
  final List<String> _conditions = [];
  int? _limit;
  String? _orderBy;

  _TransactionFinder(this.bean);

  _TransactionFinder where(_FieldCondition condition) {
    _conditions.add(condition.sql);
    return this;
  }

  _TransactionFinder limit(int count) {
    _limit = count;
    return this;
  }

  _TransactionFinder orderBy(String fieldName, {bool desc = true}) {
    _orderBy = '$fieldName ${desc ? 'DESC' : 'ASC'}';
    return this;
  }

  String toSql(String tableName) {
    String sql = 'SELECT * FROM $tableName';
    if (_conditions.isNotEmpty) {
      sql += ' WHERE ${_conditions.join(' AND ')}';
    }
    if (_orderBy != null) {
      sql += ' ORDER BY $_orderBy';
    }
    if (_limit != null) {
      sql += ' LIMIT $_limit';
    }
    return sql;
  }
}
