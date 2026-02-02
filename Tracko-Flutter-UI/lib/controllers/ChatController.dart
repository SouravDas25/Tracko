import 'package:tracko/Utils/ServerUtil.dart';
import 'package:tracko/models/chats.dart';
import 'package:tracko/models/user.dart';
import 'package:tracko/services/SessionService.dart';

class ChatController {
  static getGroupId(int userId) async {}

  static Future<List<Chat>> getAllSharedTransactions() async {
    final current = SessionService.currentUser();
    if (current.globalId.isEmpty) {
      final globalId = await ServerUtil.getGlobalAccountId(current.phoneNo);
      if (globalId != null) current.globalId = globalId;
    }
    if (current.globalId.isEmpty) return <Chat>[];

    final res = await ServerUtil.getGroupsByUser(int.parse(current.globalId));
    if (res == null) return <Chat>[];

    final chats = <Chat>[];
    for (final u in res.userList.userList) {
      if (u.phoneNo == current.phoneNo) continue;
      final c = Chat();
      c.userGlobalId = u.id;
      c.userId = int.tryParse(u.id) ?? 0;
      c.chatGroupId = res.chatGroupResponse.id;
      chats.add(c);
    }
    return chats;
  }

  static createChatGroup(User user) async {
    User current = SessionService.currentUser();
    await ServerUtil.createChatGroup(current, user);
  }
}
