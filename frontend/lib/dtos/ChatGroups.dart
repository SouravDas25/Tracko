import 'package:tracko/dtos/GlobalAccountResponse.dart';

class ChatGroupResponse {
  late String id;
  late String name;
  late DateTime createdAt;

  ChatGroupResponse.fromJson(dynamic jsonResponse) {
    this.id = jsonResponse['id'];
    this.name = jsonResponse['name'];
    this.createdAt = jsonResponse['createdAt'];
  }
}

class UserList {
  List<GlobalAccountResponse> userList = [];

  UserList.fromJson(List<dynamic> jsonResponse) {
    for (var obj in jsonResponse) {
      userList.add(GlobalAccountResponse.fromJson(obj));
    }
  }
}

class GroupByUserResponse {
  late ChatGroupResponse chatGroupResponse;
  late UserList userList;

  GroupByUserResponse.fromJson(dynamic jsonResponse) {
    this.userList = UserList.fromJson(jsonResponse['userList']);
    this.chatGroupResponse =
        ChatGroupResponse.fromJson(jsonResponse['chatGroup']);
  }
}
