
import 'package:expense_manager/Utils/enums.dart';
import 'package:flutter/material.dart';
import 'package:timeago/timeago.dart' as timeago;
import 'package:intl/intl.dart';


class CommonUtil {

  static String humanDate(var date){
    if(date.runtimeType == String){
      date = DateTime.parse(date);
    }
    return timeago.format(date);
  }

  static NumberFormat formatCurrency;

  static String toCurrency(double amount){
    if(amount == null){
      amount = 0;
    }
    if(formatCurrency == null){
      formatCurrency = new NumberFormat("#,##0.00", "en_INR");;
    }
    return '₹ ' + formatCurrency.format(amount);
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