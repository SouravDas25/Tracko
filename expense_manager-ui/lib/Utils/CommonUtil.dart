
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


}