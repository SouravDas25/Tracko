import 'package:Tracko/Utils/ConstantUtil.dart';
import 'package:Tracko/Utils/JsonStore.dart';
import 'package:Tracko/Utils/WidgetUtil.dart';

class SmsController {
  static setSmsCheckpoint(dynamic msg) async {
    try {
      await JsonStore.put(
          ConstantUtil.LAST_READ_SMS_ID, msg?.id?.toString() ?? '0');
    } catch (_) {}
  }


  static getLastSmsCheckpoint() async {
    String value = await JsonStore.get(ConstantUtil.LAST_READ_SMS_ID) ?? '';
    int start = value == null ? 0 : int.parse(value);
    return start;
  }

  static Future<List<dynamic>> getLatestSMS({int? givenCount}) async {
    // SMS feature disabled on this build. Return empty set to skip scanning.
    return [];
  }

  static Future<bool> isNewSmsPresent() async {
    // SMS feature disabled; report no new SMS.
    return false;
  }

  static Future<List<dynamic>> getSMS(int count) async {
    return [];
  }
}
