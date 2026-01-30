import 'package:Tracko/Utils/enums.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:timeago/timeago.dart' as timeago;

const double billion = 1000000000;
const double million = 1000000;

class CommonUtil {
  static String rupeeSign = '₹ ';

  static String humanDate(var date, {format}) {
    if (date.runtimeType == String) {
      if (format != null) {
        DateFormat dfrmt = new DateFormat(format);
        date = dfrmt.parse(date);
      } else {
        date = DateTime.parse(date);
      }
    }
    return timeago.format(date);
  }

  static String toCurrency(double amount) {
    NumberFormat formatCurrency;
    String tail = "";
    if (amount == null) {
      amount = 0;
    }
    double absAmount = amount.abs();
    if (absAmount >= million && absAmount < billion) {
      amount = amount / million;
      formatCurrency = NumberFormat.currency(
        decimalDigits: 0,
        symbol: rupeeSign,
      );
      tail = " million";
    } else if (absAmount >= billion) {
      amount = amount / billion;
      formatCurrency = NumberFormat.currency(
        decimalDigits: 0,
        symbol: rupeeSign,
      );
      tail = " billion";
    } else {
      formatCurrency = NumberFormat.currency(
        decimalDigits: amount - amount.round() > 0.0 ? 2 : 0,
        symbol: rupeeSign,
      );
    }
    return formatCurrency.format(amount) + tail;
  }

  static String toSign(int type) {
    return type == TransactionType.DEBIT ? " - " : " + ";
  }

  static Color toTypeColor(int type) {
    return type == TransactionType.DEBIT ? Colors.red : Colors.green;
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
