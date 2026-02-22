import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ApiConfig {
  // Development
  static const String devBaseUrl = 'http://localhost:8080';

  // Production
  static const String prodBaseUrl = 'https://your-production-url.com';

  // Toggle via build-time flag: --dart-define=IS_PRODUCTION=true
  static const bool isProduction =
      bool.fromEnvironment('IS_PRODUCTION', defaultValue: false);

  static String? _dynamicBaseUrl;

  static String get baseUrl {
    String url;
    if (_dynamicBaseUrl != null && _dynamicBaseUrl!.isNotEmpty) {
      url = _dynamicBaseUrl!;
    } else if (kIsWeb) {
      url = isProduction ? '' : devBaseUrl;
    } else {
      url = isProduction ? prodBaseUrl : devBaseUrl;
    }

    // Defensive: Remove trailing slash if present
    if (url.endsWith('/')) {
      return url.substring(0, url.length - 1);
    }
    return url;
  }

  static Future<void> loadBaseUrl() async {
    if (kIsWeb)
      return; // Web usually doesn't need dynamic host configuration in this context
    final prefs = await SharedPreferences.getInstance();
    String? url = prefs.getString('api_base_url');
    if (url != null && url.endsWith('/')) {
      url = url.substring(0, url.length - 1);
    }
    _dynamicBaseUrl = url;
  }

  static Future<void> setBaseUrl(String url) async {
    _dynamicBaseUrl = url;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('api_base_url', url);
  }

  static Future<void> reset() async {
    _dynamicBaseUrl = null;
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('api_base_url');
  }

  static String? lastAttemptedUrl;

  static bool get isConfigured =>
      kIsWeb || (_dynamicBaseUrl != null && _dynamicBaseUrl!.isNotEmpty);

  // API Endpoints
  static const String authLogin = '/api/oauth/token';
  static const String authLoginBasic = '/api/login';
  static const String authSignUp = '/api/signUp';
  static const String accounts = '/api/accounts';
  static const String accountBalances = '/api/accounts/balances';
  static const String categories = '/api/categories';
  static const String transactions = '/api/transactions';
  static const String transfers = '/api/transfers';
  static const String stats = '/api/stats';
  static const String splits = '/api/splits';
  static const String contacts = '/api/contacts';
  static const String jsonStore = '/api/json-store';
  static const String users = '/api/user';
  static const String userCurrencies = '/api/user-currencies';
  static const String exchangeRates = '/api/exchange-rates';
  static const String recurringTransactions = '/api/recurring-transactions';
  static const String health = '/api/health';
}
