class ChatMessagesResponses {
  late String id, groupId, userId, message;
  late DateTime createdAt;
  late bool isRead;

  ChatMessagesResponses.fromJson(dynamic jsonResponse) {
    this.id = jsonResponse['id'];
    this.groupId = jsonResponse['groupId'];
    this.userId = jsonResponse['userId'];
    this.message = jsonResponse['message'];
    this.createdAt = jsonResponse['createdAt'];
    this.isRead = jsonResponse['isRead'] == 0 ? false : true;
  }
}
