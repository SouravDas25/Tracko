import 'package:tracko/env.dart';

class ConstantUtil {
  static const int SMARTIFY_NOTIFICATION_ID = 124;
  static const int BACKUP_NOTIFICATION_ID = 125;

  static const int MAX_SMS_SCAN_COUNT = 1000;
  static const int MAX_MONTH_SCAN_LIMIT = 4;

  static const String CURRENCY_SYMBOL = "₹ ";
  static const String RUPEE_SIGN = '₹ ';

  static const List<String> CURRENCIES = [
    'INR',
    'USD',
    'EUR',
    'GBP',
    'JPY',
    'CAD',
    'AUD',
    'CHF',
    'CNY',
    'NZD'
  ];

  static const Map<String, String> currencyToSymbol = {
    'INR': '₹',
    'USD': '\$',
    'EUR': '€',
    'GBP': '£',
    'JPY': '¥',
    'CAD': 'C\$',
    'AUD': 'A\$',
    'CHF': 'CHF',
    'CNY': '¥',
    'NZD': 'NZ\$',
  };

  static String getCurrencySymbol(String currencyCode) {
    return currencyToSymbol[currencyCode] ?? currencyCode;
  }

  static const bool DISABLE_SCANNING_MONTH_LIMIT = true;

  static const String AUTO_BACK_UP_KEY = "autoBackUp";
  static const String AUTO_BACK_UP_TIMESTAMP_KEY = "autoBackUpTimeStamp";
  static const String LAST_READ_SMS_ID = "lastReadSmsId";

  static const int NO_OF_RECORDS_PER_PAGE = 25;

  static const String version = Environment.appVersion;
}
