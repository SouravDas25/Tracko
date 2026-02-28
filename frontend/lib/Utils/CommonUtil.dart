import 'package:tracko/Utils/enums.dart';
import 'package:tracko/services/SessionService.dart';
import 'package:tracko/di/di.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:timeago/timeago.dart' as timeago;

const double billion = 1000000000;
const double million = 1000000;

class CommonUtil {
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

  static String humanDate(var date, {format}) {
    if (date.runtimeType == String) {
      if (format != null) {
        DateFormat dfrmt = new DateFormat(format);
        date = dfrmt.parse(date);
      } else {
        date = DateTime.parse(date);
      }
    }
    // Format as "Jan 15, 2026"
    return DateFormat('MMM dd, yyyy').format(date);
  }

  static String toCurrency(double amount, {String? currencyCode}) {
    if (amount == null) {
      amount = 0;
    }
    String symbol = sl<SessionService>().currentCurrencySymbol;
    String code = currencyCode ?? 'INR';
    if (currencyCode != null) {
      symbol = getCurrencySymbol(currencyCode);
    }
    double absAmount = amount.abs();
    bool isINR = code == 'INR';
    String tail = "";
    NumberFormat formatCurrency;

    bool isJPYorCNY = code == 'JPY' || code == 'CNY';
    bool isNegative = amount < 0;
    double displayAmount = amount.abs();

    if (isINR) {
      if (absAmount >= 10000000) {
        displayAmount = absAmount / 10000000;
        tail = " crore";
      } else if (absAmount >= 100000) {
        displayAmount = absAmount / 100000;
        tail = " lakh";
      }
      bool hasFraction = (displayAmount * 100).round() % 100 != 0;
      formatCurrency = NumberFormat.currency(
        locale: 'en_IN',
        decimalDigits: hasFraction ? 2 : 0,
        symbol: symbol,
      );
    } else {
      if (absAmount >= million && absAmount < billion) {
        displayAmount = absAmount / million;
        tail = " million";
      } else if (absAmount >= billion) {
        displayAmount = absAmount / billion;
        tail = " billion";
      }
      int decimalDigits =
          isJPYorCNY ? 0 : ((displayAmount * 100).round() % 100 != 0 ? 2 : 0);
      formatCurrency = NumberFormat.currency(
        locale: 'en_US',
        decimalDigits: decimalDigits,
        symbol: symbol,
      );
    }
    String formatted = formatCurrency.format(displayAmount) + tail;
    // Place negative sign after symbol (e.g., ₹-9.88 lakh)
    if (isNegative) {
      if (formatted.startsWith(symbol)) {
        formatted = symbol + '-' + formatted.substring(symbol.length);
      } else if (formatted.startsWith('¥')) {
        // For JPY/CNY
        formatted = '¥-' + formatted.substring(1);
      } else {
        formatted = '-' + formatted;
      }
    }
    return formatted;
  }

  static String toSign(int type) {
    if (type == TransactionType.DEBIT) return " - ";
    if (type == TransactionType.TRANSFER) return " ";
    return " + ";
  }

  static Color toTypeColor(int type) {
    if (type == TransactionType.DEBIT) return Colors.red;
    if (type == TransactionType.CREDIT) return Colors.green;
    return Colors.grey;
  }

  static getInitials(string) {
    var names = string.split(' '),
        initials = names[0].substring(0, 1).toUpperCase();

    if (names.length > 1) {
      String lastname = names[names.length - 1].trim();
      if (lastname.length > 0) {
        lastname = lastname.substring(0, 1);
        RegExp exp = new RegExp(r"[a-zA-Z]+");
        if (exp.hasMatch(lastname)) initials += lastname.toLowerCase();
      }
    }
    return initials;
  }

  static bool isDigit(int rune) => rune ^ 0x30 <= 9;

  static String charAt(String subject, int position) {
    if (subject is! String ||
        subject.length <= position ||
        subject.length + position < 0) {
      return '';
    }

    int _realPosition = position < 0 ? subject.length + position : position;

    return subject[_realPosition];
  }

  static String extractPhoneNumber(String s) {
    try {
      int i;
      int len = s.length;
//    int number = 0;
      String sb = "";
      for (i = 0; i < len; i++) {
        int c = int.tryParse(charAt(s, i)) ?? 0;
        //      print(c);
        if (c != null) {
          sb += c.toString();
        }
      }
      return sb.substring(sb.length - 10, sb.length);
    } catch (e) {
      print(e);
    }
    return '';
  }

  static Future<bool> getSmsPermission() async {
    // TODO: Update to use permission_handler 11.x API
    // Map<Permission, PermissionStatus> permissions =
    // await [Permission.sms].request();
    // return permissions[Permission.sms] == PermissionStatus.granted;
    return false; // Stub for now
  }

  static Future<bool> getContactsPermission() async {
    // TODO: Update to use permission_handler 11.x API
    // Map<Permission, PermissionStatus> permissions =
    // await [Permission.contacts].request();
    // return permissions[Permission.contacts] == PermissionStatus.granted;
    return false; // Stub for now
  }
}
