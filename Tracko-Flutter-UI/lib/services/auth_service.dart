import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../config/api_config.dart';
import 'api_client.dart';

class AuthService {
  final _api = ApiClient();
  final _storage = const FlutterSecureStorage();

  Future<String> signUp({
    required String phoneNo,
    required String firebaseUuid,
    required String name,
    String? email,
    String? profilePic,
  }) async {
    final res = await _api.post<Map<String, dynamic>>(ApiConfig.authSignUp, data: {
      'phoneNo': phoneNo,
      'uuid': firebaseUuid, // backend maps to fireBaseId
      'name': name,
      'email': email,
      'profilePic': profilePic,
    });
    // If backend sends token in body/header, store it when available
    final token = res['token'] ?? res['jwtToken'];
    if (token is String) {
      await _storage.write(key: 'jwt_token', value: token);
    }
    return (res['id'] ?? res['userId'] ?? '').toString();
  }

  Future<String?> signIn({
    required String phoneNo,
    required String firebaseUuid,
  }) async {
    final res = await _api.post<Map<String, dynamic>>(ApiConfig.authLogin, data: {
      'phoneNo': phoneNo,
      'uuid': firebaseUuid,
    });
    final token = res['token'] ?? res['jwtToken'];
    if (token is String) {
      await _storage.write(key: 'jwt_token', value: token);
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
      await _storage.write(key: 'jwt_token', value: token);
      return token;
    }
    return null;
  }

  Future<bool> isLoggedIn() async => (await _storage.read(key: 'jwt_token')) != null;
  Future<void> logout() async => _storage.delete(key: 'jwt_token');
  Future<String?> getToken() async => _storage.read(key: 'jwt_token');
}
