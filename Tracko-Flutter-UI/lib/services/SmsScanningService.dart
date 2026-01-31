import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/ConstantUtil.dart';
import 'package:tracko/Utils/LocalNotificationUtil.dart';
import 'package:tracko/Utils/ServerUtil.dart';
import 'package:tracko/Utils/SettingUtil.dart';
import 'package:tracko/controllers/SmsController.dart';
import 'package:tracko/controllers/TransactionController.dart';
import 'package:tracko/models/transaction.dart';
import 'package:flutter/foundation.dart';

enum ScanningStatus { NOT_RUNNING, RUNNING, COMPLETED }

class SmsScanningService {
  static List<Transaction> possibleTransactions = <Transaction>[];
  static dynamic currentSMS;
  static ScanningStatus? status;
  static double currentProgress = 0.0;
  static bool isCancelRequested = false;

  static bool isRunning() {
    return SmsScanningService.status == ScanningStatus.RUNNING;
  }

  static bool isCompleted() {
    return SmsScanningService.status == ScanningStatus.COMPLETED;
  }

  static void reset() {
    status = ScanningStatus.NOT_RUNNING;
    possibleTransactions.clear();
    currentProgress = 0;
    currentSMS = null;
    isCancelRequested = false;
  }

  static bool isScanningAllowed(dynamic msg) {
    if (ConstantUtil.DISABLE_SCANNING_MONTH_LIMIT) {
      try {
        int msgMonth = currentSMS?.date?.month ?? SettingUtil.currentMonth.month;
        int msgYear = currentSMS?.date?.year ?? SettingUtil.currentMonth.year;
        int diff = SettingUtil.currentMonth.month - msgMonth;
        int ydiff = SettingUtil.currentMonth.year - msgYear;
        if (diff.abs() <= ConstantUtil.MAX_MONTH_SCAN_LIMIT && ydiff.abs() == 0)
          return true;
      } catch (e) {
        return false;
      }
    }
    try {
      int msgMonth = currentSMS?.date?.month ?? SettingUtil.currentMonth.month;
      int msgYear = currentSMS?.date?.year ?? SettingUtil.currentMonth.year;
      if (msgMonth == SettingUtil.currentMonth.month && msgYear == SettingUtil.currentMonth.year) return true;
    } catch (e) {
      return false;
    }
    return false;
  }

  static Future<Transaction?> processMsg(dynamic msg) async {
    currentSMS = msg;

    if (!isScanningAllowed(currentSMS)) {
//      WidgetUtil.toast("Only scanning this months SMS.");
      return null;
    }
    try { print(msg?.id); } catch (_) {}
    Transaction? transaction;
    transaction = await ServerUtil.extractSmsData(msg);
    currentSMS = null;
    if (transaction != null && transaction.amount > 0) {
      possibleTransactions.insert(0, transaction);
      print(transaction);
      TransactionController.saveTransaction(transaction);
      return transaction;
    }

    return null;
  }

  static _sendNotification(String title, String body) {
    LocalNotificationUtil()
      ..initialize()
      ..show(ConstantUtil.SMARTIFY_NOTIFICATION_ID, title, body);
  }

  static saveLastReadSms(dynamic msg) async {
    return SmsController.setSmsCheckpoint(msg);
  }

  static void stopScan() {
    isCancelRequested = true;
  }

  static double progress() {
    return currentProgress;
  }

  static Future<int> _activateScanning({Function? callback}) async {
    List<dynamic> messages = await SmsController.getLatestSMS();
    int count = 0;
    if (messages.length > 3)
      _sendNotification(
          "Scanning Started", "Checking sms for recent transactions.");
    // messages[0] is the latest message
    if (messages.length > 0) {
      // start reading from the oldest msg
      for (var msg in messages.reversed) {
        // as this service is async check if cancel is requested
        if (isCancelRequested) {
          SmsScanningService.reset();
          return count;
        }
        Transaction? transaction = await processMsg(msg);
        await saveLastReadSms(msg);
        try {
          callback?.call(transaction);
        } catch (e) {
          print(e);
        }
        count++;
        currentProgress = (count / messages.length);
        // as this service is async check if cancel is requested on the last scan item
        if (isCancelRequested) {
          SmsScanningService.reset();
          return count;
        }
      }
    }
    status = ScanningStatus.COMPLETED;
    return count;
  }

  static Future<int> scan({Function? callback}) async {
    if (kIsWeb || defaultTargetPlatform != TargetPlatform.android) {
      SmsScanningService.reset();
      return 0;
    }
    if (status == ScanningStatus.RUNNING) {
      throw Exception("Service already Running");
    }
    bool b = await CommonUtil.getSmsPermission();
    if (!b) {
      throw Exception("SMS Permissions are required.");
    }
    SmsScanningService.reset();
    status = ScanningStatus.RUNNING;
//    _sendNotification("Scanning Started", "Checking sms for recent transactions.");
    int count = await _activateScanning(callback: callback);
    if (status == ScanningStatus.NOT_RUNNING) {
      _sendNotification(
          "Scan Canceled", "User intentionally interrupted sms scanning.");
    } else {
      _sendNotification("Scan Completed",
          count > 0 ? "Scan completed successfully." : "No sms found to scan.");
    }

    return count;
  }
}
