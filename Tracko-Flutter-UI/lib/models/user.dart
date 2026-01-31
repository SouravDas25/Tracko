import 'dart:async';

import 'package:tracko/models/account.dart';
import 'package:tracko/models/category.dart';
import 'package:tracko/models/chats.dart';
import 'package:tracko/models/split.dart';
import 'package:tracko/orm_stub.dart';
// import 'package:jaguar_orm/jaguar_orm.dart'; // Removed - migrating to plain sqflite
// import 'package:jaguar_query/jaguar_query.dart'; // Removed - migrating to plain sqflite

// ORM code generation removed

// The model
class User {
  int? id;
  String profilePic = '';
  String name = '';
  String phoneNo = '';
  String email = '';
  String fireBaseId = '';
  String globalId = '';
  List<Account> accounts = [];
  List<Category> categories = [];
  List<Split> splits = [];
  List<Chat> chats = [];

  User();

  User.make(this.id, this.name, this.phoneNo, this.email);

  @override
  String toString() {
    return "{$id , $name , $email , $phoneNo , $globalId, $fireBaseId}";
  }
}

@GenBean()
class UserBean extends Bean<User> {
  UserBean(dynamic adapter)
      : super(adapter is Adapter ? adapter : Adapter(adapter));

  final String tableName = 'users';

  @override
  AccountBean get accountBean => new AccountBean(adapter);

  @override
  CategoryBean get categoryBean => new CategoryBean(adapter);

  @override
  SplitBean get splitBean => SplitBean(adapter);

  @override
  ChatBean get chatBean => ChatBean(adapter);

  // Field accessors for query building
  _UserField get phoneNo => _UserField('phoneNo');

  User fromMap(Map<String, dynamic> m) {
    final u = User();
    u.id = (m['id'] as int?);
    u.profilePic = (m['profilePic'] as String?) ?? '';
    u.name = (m['name'] as String?) ?? '';
    u.phoneNo = (m['phoneNo'] as String?) ?? '';
    u.email = (m['email'] as String?) ?? '';
    u.fireBaseId = (m['fireBaseId'] as String?) ?? '';
    u.globalId = (m['globalId'] as String?) ?? '';
    return u;
  }

  Future<int?> upsert(User u) async {
    if (u.id != null) {
      await adapter.db.update(
        tableName,
        {
          'profilePic': u.profilePic,
          'name': u.name,
          'phoneNo': u.phoneNo,
          'email': u.email,
          'fireBaseId': u.fireBaseId,
          'globalId': u.globalId,
        },
        where: 'id = ?',
        whereArgs: [u.id],
      );
      return u.id;
    } else {
      final id = await adapter.db.insert(tableName, {
        'profilePic': u.profilePic,
        'name': u.name,
        'phoneNo': u.phoneNo,
        'email': u.email,
        'fireBaseId': u.fireBaseId,
        'globalId': u.globalId,
      });
      return id;
    }
  }

  Future<User?> find(int id) async {
    final rows = await adapter.db
        .rawQuery('SELECT * FROM $tableName WHERE id = ?', [id]);
    if (rows.isEmpty) return null;
    return fromMap(rows.first);
  }

  Future<User?> findOneWhere(_UserFieldCondition condition) async {
    final rows = await adapter.db
        .rawQuery('SELECT * FROM $tableName WHERE ${condition.toSql()}');
    if (rows.isEmpty) return null;
    return fromMap(rows.first);
  }

  Future<List<User>> getAll() async {
    final rows = await adapter.db.rawQuery('SELECT * FROM $tableName');
    return rows.map((m) => fromMap(m)).toList();
  }

  Future<void> remove(int id) async {
    await adapter.db.delete(tableName, where: 'id = ?', whereArgs: [id]);
  }
}

// Helper class for User field queries
class _UserField {
  final String name;
  _UserField(this.name);

  _UserFieldCondition eq(String value) {
    return _UserFieldCondition('$name = \'$value\'');
  }
}

class _UserFieldCondition {
  final String sql;
  _UserFieldCondition(this.sql);

  String toSql() => sql;
}
