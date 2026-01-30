import 'package:Tracko/models/user.dart';
import 'package:Tracko/orm_stub.dart';
import 'package:sqflite/sqflite.dart';
// import 'package:jaguar_orm/jaguar_orm.dart'; // Removed - migrating to plain sqflite

// ORM code generation removed

class Chat {
  Chat();

  int? id;
  int userId = 0;
  String chatGroupId = '';

  @override
  String toString() {
    return 'Chat{id: $id, userId: $userId, chatGroupId: $chatGroupId}';
  }
}

@GenBean()
class ChatBean extends Bean<Chat> {
  ChatBean(dynamic adapter)
      : super(adapter is Adapter ? adapter : Adapter(adapter as Database));

  final String tableName = 'chats';

  @override
  UserBean get userBean => UserBean(adapter);

  Future<List<Chat>> getAll() async {
    final rows = await adapter.db.rawQuery('SELECT * FROM $tableName');
    return rows.map((m) {
      final c = Chat();
      c.id = (m['id'] as int?);
      c.userId = (m['userId'] as int?) ?? 0;
      c.chatGroupId = (m['chatGroupId'] as String?) ?? '';
      return c;
    }).toList();
  }

  Future<List<Chat>> findByUser(int? userId) async {
    final rows = await adapter.db
        .rawQuery('SELECT * FROM $tableName WHERE userId = ?', [userId ?? 0]);
    return rows.map((m) {
      final c = Chat();
      c.id = (m['id'] as int?);
      c.userId = (m['userId'] as int?) ?? 0;
      c.chatGroupId = (m['chatGroupId'] as String?) ?? '';
      return c;
    }).toList();
  }

  Future<int?> insert(Chat c) async {
    final id = await adapter.db.insert(tableName, {
      'userId': c.userId,
      'chatGroupId': c.chatGroupId,
    });
    return id;
  }
}
