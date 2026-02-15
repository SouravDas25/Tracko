import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../config/api_config.dart';
import 'api_client.dart';

class AuthService {
  final _api = ApiClient();
  final _storage = const FlutterSecureStorage();

  Future<void> _writeToken(String token) async {
    if (kIsWeb) {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('jwt_token', token);
      return;
    }
    await _storage.write(key: 'jwt_token', value: token);
  }

  Future<String?> _readToken() async {
    if (kIsWeb) {
      final prefs = await SharedPreferences.getInstance();
      return prefs.getString('jwt_token');
    }
    return _storage.read(key: 'jwt_token');
  }

  Future<void> _deleteToken() async {
    if (kIsWeb) {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove('jwt_token');
      return;
    }
    await _storage.delete(key: 'jwt_token');
  }

  Future<String> signUp({
    required String phoneNo,
    required String name,
    String? email,
    String? profilePic,
    String? baseCurrency,
  }) async {
    final res =
        await _api.post<Map<String, dynamic>>(ApiConfig.authSignUp, data: {
      'phoneNo': phoneNo,
      'name': name,
      'email': email,
      'profilePic': profilePic,
      'baseCurrency': baseCurrency ?? 'INR',
    });
    // If backend sends token in body/header, store it when available
    final token = res['token'] ?? res['jwtToken'];
    if (token is String) {
      await _writeToken(token);
      ApiClient.resetAuthSuppression();
    }
    return (res['id'] ?? res['userId'] ?? '').toString();
  }

  Future<String?> signIn({
    required String phoneNo,
  }) async {
    final res =
        await _api.post<Map<String, dynamic>>(ApiConfig.authLogin, data: {
      'phoneNo': phoneNo,
    });
    final token = res['token'] ?? res['jwtToken'];
    if (token is String) {
      await _writeToken(token);
      ApiClient.resetAuthSuppression();
      return token;
    }
    return null;
  }

  Future<String?> signInBasic({
    required String username,
    required String password,
  }) async {
    final res = await _api.post<Map<String, dynamic>>(
      ApiConfig.authLoginBasic,
      data: {
        'username': username,
        'password': password,
      },
    );
    final token = res['token'] ?? res['jwtToken'];
    if (token is String) {
      await _writeToken(token);
      ApiClient.resetAuthSuppression();
      return token;
    }
    return null;
  }

  Future<bool> isLoggedIn() async => (await _readToken()) != null;
  Future<void> logout() async => _deleteToken();
  Future<String?> getToken() async => _readToken();
}
