import 'package:Tracko/Utils/ConstantUtil.dart';
import 'package:Tracko/Utils/JsonStore.dart';
import 'package:Tracko/Utils/WidgetUtil.dart';
import 'package:shared_preferences/shared_preferences.dart';

class SettingUtil {
  static getStore() {
    return SharedPreferences.getInstance();
  }

  static DateTime _selectedMonth = DateTime.now();

  static DateTime get previousMonth {
    return DateTime.utc(_selectedMonth.year, _selectedMonth.month - 1);
  }

  static DateTime get currentMonth {
    return DateTime.utc(_selectedMonth.year, _selectedMonth.month);
  }

  static DateTime get nextMonth {
    return DateTime.utc(_selectedMonth.year, _selectedMonth.month + 1);
  }

  static setSelectedMonth(DateTime month) {
    if (month == null) return;
    _selectedMonth = month;
    if (WidgetUtil.globalHomeTabState != null &&
        WidgetUtil.globalHomeTabState?.mounted == true)
      WidgetUtil.globalHomeTabState?.setState(() {});
  }

  static Future<bool> isAutoBackUpEnabled() async {
    bool isKeyPresent = await JsonStore.has(ConstantUtil.AUTO_BACK_UP_KEY) ?? false;
    if (!isKeyPresent) return true;
    String val = await JsonStore.get(ConstantUtil.AUTO_BACK_UP_KEY) ?? '1';
    return val == "1" ? true : false;
  }

  static Future<void> updateAutoBackup(bool enabled) async {
    String val = "0";
    if (enabled) val = "1";
    await JsonStore.put(ConstantUtil.AUTO_BACK_UP_KEY, val);
  }

  static DateTime? _lastBackedUpTime;

  static Future<DateTime?> getLastBackedUpTime() async {
    bool hasTimestamp = await JsonStore.has(ConstantUtil.AUTO_BACK_UP_TIMESTAMP_KEY) ?? false;
    if (hasTimestamp) {
      String val = await JsonStore.get(ConstantUtil.AUTO_BACK_UP_TIMESTAMP_KEY) ?? '';
      if (val.isNotEmpty) {
        _lastBackedUpTime =
            DateTime.fromMillisecondsSinceEpoch(int.parse(val), isUtc: true);
      }
    }
    return _lastBackedUpTime;
  }

  static Future<void> updateLatestBackedUpTime() async {
    _lastBackedUpTime = DateTime.now().toUtc();
    await JsonStore.put(ConstantUtil.AUTO_BACK_UP_TIMESTAMP_KEY,
        _lastBackedUpTime?.millisecondsSinceEpoch.toString() ?? '0');
  }
}
