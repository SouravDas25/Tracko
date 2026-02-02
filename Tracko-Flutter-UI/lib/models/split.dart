import 'package:tracko/models/transaction.dart';
import 'package:tracko/models/user.dart';
import 'package:tracko/models/contact.dart';
import 'package:tracko/orm_stub.dart';
// import 'package:jaguar_orm/jaguar_orm.dart'; // Removed - migrating to plain sqflite

// ORM code generation removed

class Split {
  int? id;
  int transactionId = 0;
  int userId = 0;
  int? contactId;
  double amount = 0.0;
  int isSettled = 0;
  DateTime settledAt = DateTime.now();
  Transaction? transaction;
  Contact? contact;

  Split();

  @override
  String toString() {
    return 'Split{id: $id, transactionId: $transactionId, userId: $userId, contactId: $contactId, amount: $amount, isSettled: $isSettled}';
  }
}

@GenBean()
class SplitBean extends Bean<Split> {
  SplitBean(dynamic adapter)
      : super(adapter is Adapter ? adapter : Adapter(adapter));

  final String tableName = 'splits';

  @override
  TransactionBean get transactionBean => TransactionBean(adapter);

  @override
  UserBean get userBean => UserBean(adapter);

  // Field accessors for SQL query building (used by SplitController)
  _Field get id => _Field('id');
  _Field get userId => _Field('userId');
  _Field get transactionId => _Field('transactionId');
  _Field get amount => _Field('amount');
  _Field get isSettled => _Field('isSettled');
  _Field get settledAt => _Field('settledAt');

  Split fromMap(Map<String, dynamic> map) {
    final s = Split();
    s.id = (map['id'] as int?);
    s.userId = (map['userId'] as int?) ?? 0;
    s.transactionId = (map['transactionId'] as int?) ?? 0;
    s.contactId = (map['contactId'] as int?);
    s.amount = (map['amount'] as num?)?.toDouble() ?? 0.0;
    s.isSettled = (map['isSettled'] as int?) ?? 0;
    final settledAtRaw = map['settledAt'];
    if (settledAtRaw is String) {
      s.settledAt = DateTime.tryParse(settledAtRaw) ?? DateTime.now();
    } else if (settledAtRaw is int) {
      s.settledAt = DateTime.fromMillisecondsSinceEpoch(settledAtRaw);
    }
    return s;
  }

  Future<Split?> find(int? id) async {
    if (id == null) return null;
    final rows = await adapter.db
        .rawQuery('SELECT * FROM $tableName WHERE id = ?', [id]);
    if (rows.isEmpty) return null;
    return fromMap(rows.first);
  }

  Future<List<Split>> findByUser(int userId) async {
    final rows = await adapter.db
        .rawQuery('SELECT * FROM $tableName WHERE userId = ?', [userId]);
    return rows.map((m) => fromMap(m)).toList();
  }

  Future<void> update(Split s) async {
    await adapter.db.update(
      tableName,
      {
        'userId': s.userId,
        'transactionId': s.transactionId,
        'contactId': s.contactId,
        'amount': s.amount,
        'isSettled': s.isSettled,
        'settledAt': s.settledAt.toIso8601String(),
      },
      where: 'id = ?',
      whereArgs: [s.id],
    );
  }

  Future<void> removeByTransaction(int transactionId) async {
    await adapter.db.delete(
      tableName,
      where: 'transactionId = ?',
      whereArgs: [transactionId],
    );
  }

  Future<int?> insert(Split s) async {
    final id = await adapter.db.insert(tableName, {
      'userId': s.userId,
      'transactionId': s.transactionId,
      'contactId': s.contactId,
      'amount': s.amount,
      'isSettled': s.isSettled,
      'settledAt': s.settledAt.toIso8601String(),
    });
    return id;
  }

  Future<List<Split>> findByTransaction(int? transactionId) async {
    if (transactionId == null) return [];
    final rows = await adapter.db.rawQuery(
        'SELECT * FROM $tableName WHERE transactionId = ?', [transactionId]);
    return rows.map((m) => fromMap(m)).toList();
  }

  // Finder support for query building
  _Finder get finder => _Finder(this);

  Future<List<Split>> findMany(_Finder finder) async {
    final rows = await adapter.db
        .rawQuery('SELECT * FROM $tableName WHERE ${finder.toSql()}');
    return rows.map((m) => fromMap(m)).toList();
  }
}

// Helper class for field name access in SQL queries
class _Field {
  final String name;
  _Field(this.name);
}

// Simple finder for query building
class _Finder {
  final SplitBean bean;
  final List<String> _conditions = [];

  _Finder(this.bean);

  _Finder eq(String fieldName, dynamic value) {
    _conditions.add('$fieldName = $value');
    return this;
  }

  String toSql() {
    return _conditions.join(' AND ');
  }
}
