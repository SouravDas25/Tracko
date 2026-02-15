import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../config/api_config.dart';
import '../Utils/ServerUtil.dart';
import '../Utils/WidgetUtil.dart';
import 'SessionService.dart';

class ApiClient {
  static final ApiClient _instance = ApiClient._internal();
  factory ApiClient() => _instance;

  late final Dio _dio;
  final _storage = const FlutterSecureStorage();
  static bool _isAutoSigningOut = false;
  static bool _suppressAuthHeader = false;

  Future<String?> _readToken() async {
    if (kIsWeb) {
      final prefs = await SharedPreferences.getInstance();
      return prefs.getString('jwt_token');
    }
    return _storage.read(key: 'jwt_token');
  }

  Future<void> _writeToken(String token) async {
    if (kIsWeb) {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('jwt_token', token);
      return;
    }
    await _storage.write(key: 'jwt_token', value: token);
  }

  static void resetAuthSuppression() {
    _suppressAuthHeader = false;
  }

  ApiClient._internal() {
    _dio = Dio(
      BaseOptions(
        baseUrl: ApiConfig.baseUrl,
        connectTimeout: const Duration(seconds: 25),
        receiveTimeout: const Duration(seconds: 25),
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json',
        },
      ),
    );

    _dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) async {
        if (_suppressAuthHeader) {
          handler.next(options);
          return;
        }
        var token = await _readToken();
        if (token == null || token.isEmpty) {
          // Backward-compat: older parts of the app store auth token here.
          token = ServerUtil.authJwtToken;
          if (token != null && token.isNotEmpty) {
            // Keep both auth systems in sync.
            await _writeToken(token);
          }
        }
        if (token != null && token.isNotEmpty) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        handler.next(options);
      },
      onResponse: (response, handler) {
        // Unwrap ApiResponse { result, message }
        final data = response.data;
        if (data is Map && data.containsKey('result')) {
          response.data = data['result'];
        }
        handler.next(response);
      },
      onError: (DioException err, handler) async {
        final code = err.response?.statusCode;
        if ((code == 401 || code == 403) && !_isAutoSigningOut) {
          _isAutoSigningOut = true;
          _suppressAuthHeader = true;
          try {
            // Perform a full logout to remove any persisted tokens and
            // reset in-memory/session state. This prevents the login
            // page from immediately redirecting back to home due to a
            // lingering token, which can cause a navigation loop.
            await SessionService.logout();
          } catch (_) {
            // ignore
          }

          try {
            final state = WidgetUtil.globalHomeTabState;
            if (state != null && state.mounted) {
              Navigator.of(state.context).pushNamedAndRemoveUntil(
                '/login',
                (route) => false,
              );
            }
          } catch (_) {
            // ignore
          } finally {
            _isAutoSigningOut = false;
          }
        }
        handler.next(err);
      },
    ));
  }

  Dio get dio => _dio;

  Future<T> get<T>(String path, {Map<String, dynamic>? query}) async {
    final res = await _dio.get(path, queryParameters: query);
    return res.data as T;
  }

  Future<T> post<T>(String path, {dynamic data}) async {
    final res = await _dio.post(path, data: data);
    return res.data as T;
  }

  Future<T> put<T>(String path, {dynamic data}) async {
    final res = await _dio.put(path, data: data);
    return res.data as T;
  }

  Future<T> patch<T>(String path, {dynamic data}) async {
    final res = await _dio.patch(path, data: data);
    return res.data as T;
  }

  Future<T> delete<T>(String path) async {
    final res = await _dio.delete(path);
    return res.data as T;
  }
}
