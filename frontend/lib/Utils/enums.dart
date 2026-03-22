import 'package:flutter/material.dart';

class TransactionType {
  static const int DEBIT = 1;
  static const int CREDIT = 2;
  static const int TRANSFER = 3;

  static Color color(int type, {Brightness brightness = Brightness.light}) {
    final isDark = brightness == Brightness.dark;
    switch (type) {
      case DEBIT:
        return isDark ? Colors.redAccent : Colors.redAccent;
      case TRANSFER:
        return isDark ? Colors.lightBlueAccent : Colors.blueGrey;
      case CREDIT:
      default:
        return isDark ? Colors.tealAccent : Colors.teal;
    }
  }

  static String stringify(int type) {
    switch (type) {
      case DEBIT:
        return "DEBIT";
      case CREDIT:
        return "CREDIT";
      case TRANSFER:
        return "TRANSFER";
      default:
        return "UNKNOWN";
    }
  }

  static int inttify(String val) {
    final v = val.trim().toUpperCase();
    if (v == "DEBIT") {
      return TransactionType.DEBIT;
    }
    if (v == "TRANSFER") {
      return TransactionType.TRANSFER;
    }
    return TransactionType.CREDIT;
  }
}
