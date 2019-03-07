
import 'package:timeago/timeago.dart' as timeago;

class CommonUtil {

  static String humanDate(var date){
    if(date.runtimeType == String){
      date = DateTime.parse(date);
    }
    return timeago.format(date);
  }


}