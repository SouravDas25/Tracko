import 'dart:io';
import 'dart:typed_data';

import 'package:tracko/Utils/ConstantUtil.dart';
import 'package:tracko/Utils/LocalNotificationUtil.dart';
import 'package:tracko/Utils/SettingUtil.dart';
import 'package:tracko/Utils/WidgetUtil.dart';
// import 'package:firebase_storage/firebase_storage.dart'; // Disabled - needs migration to current API

class BackupService {
  static bool restoreRequired = false;

  // TODO: Migrate to current Firebase Storage API (firebase_storage v11+)
  // static StorageReference _storageReference(String phoneNo) {
  //   final StorageReference storageRef =
  //       FirebaseStorage.instance.ref().child("backups/$phoneNo/basic.db");
  //   return storageRef;
  // }

  static _sendNotification(String title, String body) {
    LocalNotificationUtil()
      ..initialize()
      ..show(ConstantUtil.BACKUP_NOTIFICATION_ID, title, body);
  }

  static Future<void> isRestoreRequired() async {
    restoreRequired = false;
  }

  static Future<void> restoreDatabaseIfRequired(String phoneNo) async {
    if (restoreRequired) {
      await restoreDatabase(phoneNo);
    }
  }

  static Future<void> restoreDatabase(String phoneNo) async {
    // TODO: Implement with current Firebase Storage API
    print("Restore feature disabled - needs Firebase Storage migration");
    WidgetUtil.toast("Restore feature temporarily disabled");
  }

  static Future<bool> isBackupRequired(String phoneNo) async {
    // TODO: Implement with current Firebase Storage API
    return false; // Disabled until migration
  }

  static Future<void> backupDatabaseIfRequired(String phoneNo) async {
    bool isBackUpRequired = await isBackupRequired(phoneNo);
    bool isAutoBackUpEnabled = await SettingUtil.isAutoBackUpEnabled();
    if (isBackUpRequired && isAutoBackUpEnabled) {
      await backupDatabase(phoneNo);
    } else {
      print("Backup Not Required.");
    }
  }

  static Future<void> backupDatabase(String phoneNo) async {
    // TODO: Implement with current Firebase Storage API
    print("Backup feature disabled - needs Firebase Storage migration");
    _sendNotification("Backup", "Backup feature temporarily disabled");
    WidgetUtil.toast("Backup feature temporarily disabled");
  }

  static Future<bool> deleteBackUp(String phoneNo) async {
    // TODO: Implement with current Firebase Storage API
    print("Delete backup feature disabled - needs Firebase Storage migration");
    WidgetUtil.toast("Delete backup feature temporarily disabled");
    return false;
  }

  static Future<Uint8List> _readDatabaseFile() async {
    return Uint8List(0);
  }

  static Future<void> _writeDatabaseFile(Uint8List content, String path) async {
    File f = File(path);
    await f.writeAsBytes(content, flush: true);
  }
}
