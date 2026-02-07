import 'package:tracko/models/user.dart';

class Chat {
  Chat();

  int? id;
  int userId = 0;
  String userGlobalId = '';
  String chatGroupId = '';

  @override
  String toString() {
    return 'Chat{id: $id, userId: $userId, userGlobalId: $userGlobalId, chatGroupId: $chatGroupId}';
  }
}
