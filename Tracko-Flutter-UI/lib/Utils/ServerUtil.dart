import 'dart:convert' as convert;

import 'package:Tracko/Utils/DestinationUtil.dart';
import 'package:Tracko/controllers/TransactionController.dart';
import 'package:Tracko/dtos/ChatGroups.dart';
import 'package:Tracko/dtos/ChatMessagesResponses.dart';
import 'package:Tracko/dtos/GlobalAccountResponse.dart';
import 'package:Tracko/models/transaction.dart';
import 'package:Tracko/models/user.dart';
import 'package:http/http.dart' as http;

class ServerUtil {
  static String? authJwtToken;

  static get authHeader => {"Authorization": "Bearer $authJwtToken"};

  static Future<String?> getGlobalAccountId(String phoneNumber) async {
    GlobalAccountResponse? user = await getGlobalAccount(phoneNumber);
    if (user != null) {
      return user.id;
    }
    return null;
  }

  static Future<String?> getAuthToken(User user) async {
    var url = Uri.parse(DestinationUtil.javaBackend() + "/api/oauth/token");
    var headers = {"Content-Type": "application/json"};
    var body = {"phoneNo": user.phoneNo, "firebaseUuid": user.fireBaseId};
    String data = convert.jsonEncode(body);
    var response = await http.post(url, headers: headers, body: data);
    if (response.statusCode == 200) {
      var jsonResponse = convert.jsonDecode(response.body);
      String? token = jsonResponse["token"];
      if (token != null && token.length > 1) {
        ServerUtil.authJwtToken = token;
      }
      return token;
    }
    return null;
  }

  static Future<String?> signUp(User user) async {
    var url = Uri.parse(DestinationUtil.javaBackend() + "api/signUp");
    var requestBody = {
      "phoneNo": user.phoneNo,
      "uuid": user.fireBaseId,
      "isShadow": 0
    };
    String data = convert.jsonEncode(requestBody);
    print(data);
//    data = Uri.encodeQueryComponent(data);
    var header = {"Content-Type": "application/json"};
    var response = await http.post(
      url,
      body: data,
      headers: header,
    );
    print(response.headers);
    print(response.body);
    if (response.statusCode == 200) {
      var token = response.headers['jwt-token'];
      if (token != null && token.length > 1) {
        ServerUtil.authJwtToken = token;
      }
      var jsonResponse = convert.jsonDecode(response.body);
      return jsonResponse["result"];
    }
    return null;
  }

  static Future<GlobalAccountResponse?> getGlobalAccount(
      String phoneNumber) async {
    var url = Uri.parse(DestinationUtil.javaBackend() +
        "/api/user/byPhoneNo?phone_no=" +
        Uri.encodeQueryComponent(phoneNumber));
    var response = await http.get(url, headers: authHeader);
    if (response.statusCode == 200) {
      var jsonResponse = convert.jsonDecode(response.body);
      var result = jsonResponse["result"];
      if (result == null) {
        return null;
      }
      return GlobalAccountResponse.fromJson(result[0]);
    }
    return null;
  }

  static Future<String?> createGlobalAccount(User user,
      {String? accountName}) async {
    var url = Uri.parse(DestinationUtil.javaBackend() + "api/account/create");
    var requestBody = {
      "name": user.name,
      "phoneNo": user.phoneNo,
      "email": user.email,
      "profilePic": user.profilePic,
      "uuid": user.fireBaseId,
      "isShadow": 0
    };
    String data = convert.jsonEncode(requestBody);
//    data = Uri.encodeQueryComponent(data);
    var header = {"Content-Type": "application/json"};
    header.addAll(authHeader);
    var response = await http.post(
      url,
      body: data,
      headers: header,
    );
    print(response.body);
    if (response.statusCode == 200) {
      var jsonResponse = convert.jsonDecode(response.body);
      return jsonResponse["result"];
    }
    return null;
  }

  static Future<Transaction?> extractSmsData(dynamic message) async {
//    String urlPart = "apis/";
    String urlPart = "api/dialog";
    var url = Uri.parse(DestinationUtil.pythonBackend() + urlPart);
    Map<String, String> body = new Map();
    body['text'] = message?.body?.toString() ?? '';
    body['address'] = message?.address?.toString() ?? '';
    body['date'] = (message?.dateSent ?? message?.date ?? DateTime.now()).toString();
    String data = convert.jsonEncode(body);
//    data = Uri.encodeQueryComponent(data);
    var header = {"Content-Type": "application/json"};
    header.addAll(authHeader);
    var response = await http.post(
      url,
      body: data,
      headers: header,
    );

    if (response.statusCode == 200) {
      var jsonResponse = convert.jsonDecode(response.body);
//      print(jsonResponse);
      Transaction transaction =
      await TransactionController.fromJson(jsonResponse);
      if (transaction != null) {
        return transaction;
      }
      return null;
    } else {
      print("/dialog call failing with ${response.statusCode}.");
      return null;
    }
  }

  static Future<GroupByUserResponse?> getGroupsByUser(int userGlobalId) async {
    var url = Uri.parse(DestinationUtil.javaBackend() + "api/chat/groups/$userGlobalId");
    var response = await http.get(url, headers: authHeader);
//    print(response.body);
    if (response.statusCode == 200) {
      var jsonResponse = convert.jsonDecode(response.body);
      return GroupByUserResponse.fromJson(jsonResponse['result']);
    }
    return null;
  }

  static Future<List<ChatMessagesResponses>?> getChatMessages(String groudId,
      String currentUserGlobalId) async {
    var url = Uri.parse(DestinationUtil.javaBackend() + "api/chat/messages/$groudId");
    var requestBody = {"currentUserGlobalId": currentUserGlobalId};
    var headers = {"Content-Type": "application/json"};
    headers.addAll(authHeader);
    String data = convert.jsonEncode(requestBody);
    var response = await http.post(url, body: data, headers: headers);
//    print(response.body);
    if (response.statusCode == 200) {
      var jsonResponse = convert.jsonDecode(response.body);
//      print(response.body);
      List<ChatMessagesResponses> chats = [];
      for (var obj in jsonResponse['result']) {
        chats.add(ChatMessagesResponses.fromJson(obj));
      }
      return chats;
    }
    return null;
  }

  static Future<bool> sendMessage(String currentUserGlobalId, String groupId,
      String message) async {
    var url = Uri.parse(DestinationUtil.javaBackend() + "/api/chat/send");
    var requestBody = {
      "sender": currentUserGlobalId,
      "chatGroupAddress": groupId,
      "message": message
    };
    var headers = {"Content-Type": "application/json"};
    headers.addAll(authHeader);
    String data = convert.jsonEncode(requestBody);
    var response = await http.post(url, body: data, headers: headers);
//    print(response.body);
    if (response.statusCode == 200) {
      var jsonResponse = convert.jsonDecode(response.body);
      return jsonResponse['result'] == "SUCCESS";
    }
    return false;
  }

  static Future<bool> updateGlobalUser(User user,
      {bool isShadow = false}) async {
    var url = Uri.parse(DestinationUtil.javaBackend() + "/api/user/save");
    var requestBody = {
      "id": user.globalId,
      "name": user.name,
      "phoneNo": user.phoneNo,
      "email": user.email,
      "profilePic": user.profilePic,
      "uuid": user.fireBaseId,
      "isShadow": isShadow ? 1 : 0
    };
    String data = convert.jsonEncode(requestBody);
//    data = Uri.encodeQueryComponent(data);
    var headers = {"Content-Type": "application/json"};
    headers.addAll(authHeader);
    var response = await http.post(
      url,
      body: data,
      headers: headers,
    );
    print(response.body);
    if (response.statusCode == 200) {
      var jsonResponse = convert.jsonDecode(response.body);
      return jsonResponse['result'] == user.globalId;
    }
    return false;
  }

  static Future<String?> createChatGroup(User currentUser,
      User otherUser) async {
    var url = Uri.parse(DestinationUtil.javaBackend() + "/api/chat/create");
    var requestBody = {
      "name": otherUser.name,
      "users": [otherUser.phoneNo, currentUser.phoneNo]
    };
    String data = convert.jsonEncode(requestBody);
    var headers = {"Content-Type": "application/json"};
    headers.addAll(authHeader);
    var response = await http.post(
      url,
      body: data,
      headers: headers,
    );
    if (response.statusCode == 200) {
      var jsonResponse = convert.jsonDecode(response.body);
      return jsonResponse['result'];
    }
    return null;
  }
}
