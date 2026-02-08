import 'package:flutter/foundation.dart';

class ApiConfig {
  // Development
  static const String devBaseUrl = 'http://localhost:8080';

  // Production
  static const String prodBaseUrl = 'https://your-production-url.com';

  // Toggle via build-time flag: --dart-define=IS_PRODUCTION=true
  static const bool isProduction =
      bool.fromEnvironment('IS_PRODUCTION', defaultValue: false);

  static String get baseUrl {
    if (kIsWeb) {
      return isProduction ? '' : devBaseUrl;
    }
    return isProduction ? prodBaseUrl : devBaseUrl;
  }

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
}
