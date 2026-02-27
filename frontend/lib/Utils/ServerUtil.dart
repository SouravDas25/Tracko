import 'dart:convert' as convert;

import 'package:tracko/Utils/DestinationUtil.dart';
import 'package:tracko/controllers/TransactionController.dart';
import 'package:tracko/dtos/GlobalAccountResponse.dart';
import 'package:tracko/models/transaction.dart';
import 'package:tracko/models/user.dart';
import 'package:tracko/services/api_client.dart';
import 'package:dio/dio.dart';

class ServerUtil {
  static String? authJwtToken;

  static get authHeader {
    final token = authJwtToken;
    if (token == null || token.isEmpty) {
      return <String, String>{};
    }
    return <String, String>{"Authorization": "Bearer $token"};
  }

  static Future<String?> getGlobalAccountId(String phoneNumber) async {
    GlobalAccountResponse? user = await getGlobalAccount(phoneNumber);
    if (user != null) {
      return user.id;
    }
    return null;
  }

  static Future<String?> getAuthToken(User user) async {
    var url = Uri.parse(DestinationUtil.javaBackend() + "api/oauth/token");
    var headers = {"Content-Type": "application/json"};
    var body = {"phoneNo": user.phoneNo, "password": user.password};
    String data = convert.jsonEncode(body);
    final dio = ApiClient().dio;
    final response =
        await dio.postUri(url, data: data, options: Options(headers: headers));
    if (response.statusCode == 200) {
      final jsonResponse = response.data is String
          ? convert.jsonDecode(response.data as String)
          : response.data as Map<String, dynamic>;
      String? token = jsonResponse["token"];
      if (token != null && token.length > 1) {
        ServerUtil.authJwtToken = token;
      }
      return token;
    }
    return null;
  }

  static Future<GlobalAccountResponse?> getGlobalAccount(
      String phoneNumber) async {
    var url = Uri.parse(DestinationUtil.javaBackend() +
        "api/user/byPhoneNo?phone_no=" +
        Uri.encodeQueryComponent(phoneNumber));
    final dio = ApiClient().dio;
    var response = await dio.getUri(url, options: Options(headers: authHeader));
    if (response.statusCode == 200) {
      final jsonResponse = response.data is String
          ? convert.jsonDecode(response.data as String)
          : response.data as Map<String, dynamic>;
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
      "isShadow": 0
    };
    String data = convert.jsonEncode(requestBody);
//    data = Uri.encodeQueryComponent(data);
    var header = {"Content-Type": "application/json"};
    header.addAll(authHeader);
    final dio = ApiClient().dio;
    final response =
        await dio.postUri(url, data: data, options: Options(headers: header));
    print(response.data);
    if (response.statusCode == 200) {
      final jsonResponse = response.data is String
          ? convert.jsonDecode(response.data as String)
          : response.data as Map<String, dynamic>;
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
    body['date'] =
        (message?.dateSent ?? message?.date ?? DateTime.now()).toString();
    String data = convert.jsonEncode(body);
//    data = Uri.encodeQueryComponent(data);
    var header = {"Content-Type": "application/json"};
    header.addAll(authHeader);
    final dio = ApiClient().dio;
    final response =
        await dio.postUri(url, data: data, options: Options(headers: header));

    if (response.statusCode == 200) {
      final jsonResponse = response.data is String
          ? convert.jsonDecode(response.data as String)
          : response.data as Map<String, dynamic>;
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

  static Future<bool> updateGlobalUser(User user,
      {bool isShadow = false}) async {
    var url = Uri.parse(DestinationUtil.javaBackend() + "api/user/save");
    var requestBody = {
      "id": user.globalId,
      "name": user.name,
      "phoneNo": user.phoneNo,
      "email": user.email,
      "profilePic": user.profilePic,
      "isShadow": isShadow ? 1 : 0
    };
    String data = convert.jsonEncode(requestBody);
//    data = Uri.encodeQueryComponent(data);
    var headers = {"Content-Type": "application/json"};
    headers.addAll(authHeader);
    final dio = ApiClient().dio;
    final response =
        await dio.postUri(url, data: data, options: Options(headers: headers));
    print(response.data);
    if (response.statusCode == 200) {
      final jsonResponse = response.data is String
          ? convert.jsonDecode(response.data as String)
          : response.data as Map<String, dynamic>;
      return jsonResponse['result'] == user.globalId;
    }
    return false;
  }
}
