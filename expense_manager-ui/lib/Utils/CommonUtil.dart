
import 'package:expense_manager/Utils/enums.dart';
import 'package:flutter/material.dart';
import 'package:timeago/timeago.dart' as timeago;
import 'package:intl/intl.dart';

const double billion = 1000000000;
const double million = 1000000;

class CommonUtil {

  static String humanDate(var date){
    if(date.runtimeType == String){
      date = DateTime.parse(date);
    }
    return timeago.format(date);
  }

  static String toCurrency(double amount){
    NumberFormat formatCurrency;
    String tail = "";
    double absAmount = amount.abs();
    if(amount == null){
      amount = 0;
    }
    if(absAmount >= million && absAmount < billion){
      amount = amount/million;
      formatCurrency =  NumberFormat.currency(
        decimalDigits: 0,
        symbol: '₹ ',
      );
      tail = " million";
    }
    else if(absAmount >= billion){
      amount = amount/billion;
      formatCurrency =  NumberFormat.currency(
        decimalDigits: 0,
        symbol: '₹ ',
      );
      tail = " billion";
    }
    else {
      formatCurrency =  NumberFormat.currency(
        decimalDigits: amount - amount.round() > 0.0 ? 2 : 0,
        symbol: '₹ ',
      );
    }
    return formatCurrency.format(amount) + tail;
  }

  static String toImageUrl(String name){
    return "https://ui-avatars.com/api/?name=$name";
  }

  static String toSign(int type){
    return type == TransactionType.DEBIT ? " - " : " + ";
  }

  static Color toTypeColor(int type){
    return type == TransactionType.DEBIT ? Colors.red : Colors.green ;
  }

}