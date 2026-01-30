import 'package:Tracko/Utils/SmartUtil.dart';
import 'package:Tracko/controllers/SmsController.dart';
import 'package:Tracko/pages/smartify_page/smartify_page.dart';
import 'package:Tracko/services/SmsScanningService.dart';
import 'package:flutter/material.dart';

class AutoScanMsgController {
  static Future onSelectNotification(TabController tabController) async {
    print("Scanning ..");
    SmartUtil.setAutoEnable();
    tabController.animateTo(3);
  }

  static giveNotificationIfNewMessages(TabController tabController) async {
    bool b = await SmsController.isNewSmsPresent();
    if (b) {
      SmsScanningService.scan(callback: SmartifyState.onUpdate)
          .then((value) => SmartifyState.onUpdate(value));
//      LocalNotificationUtil()
//        ..initialize(onSelect: (String payload) {
//          onSelectNotification(tabController);
//        })
//        ..show(ConstantUtil.SMARTIFY_NOTIFICATION_ID, "New Messages Detected!",
//            "New Messages has been detected, scan to find Expenses.");
    }
  }
}
