import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../config/api_config.dart';
import 'api_client.dart';

class AuthService {
  final ApiClient _api;
  final _storage = const FlutterSecureStorage();

  AuthService({required ApiClient api}) : _api = api;

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

  Future<String?> signIn({
    required String phoneNo,
    required String password,
  }) async {
    final res =
        await _api.post<Map<String, dynamic>>(ApiConfig.authLogin, data: {
      'phoneNo': phoneNo,
      'password': password,
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
