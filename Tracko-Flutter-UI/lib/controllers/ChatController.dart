import 'package:Tracko/Utils/DatabaseUtil.dart';
import 'package:Tracko/Utils/ServerUtil.dart';
import 'package:Tracko/models/chats.dart';
import 'package:Tracko/models/user.dart';
import 'package:Tracko/services/SessionService.dart';

class ChatController {
  static getGroupId(int userId) async {}

  static Future<List<Chat>> getAllSharedTransactions() async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    ChatBean chatBean = ChatBean(adapter);
    return await chatBean.getAll();
  }

  static createChatGroup(User user) async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    ChatBean chatBean = ChatBean(adapter);
    User current = SessionService.currentUser();
    List<Chat> chats = await chatBean.findByUser(user.id);

    if (chats.length <= 0) {
      String chatGroupId = await ServerUtil.createChatGroup(current, user) ?? '';
      Chat chat = new Chat();
      chat.userId = user.id ?? 0;
      chat.chatGroupId = chatGroupId;
      chat.id = await chatBean.insert(chat);
    }
  }
}
