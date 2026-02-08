import 'package:tracko/Utils/SmartUtil.dart';
import 'package:flutter/material.dart';

class AutoScanMsgController {
  static Future onSelectNotification(TabController tabController) async {
    print("Scanning ..");
    SmartUtil.setAutoEnable();
    tabController.animateTo(3);
  }

  static giveNotificationIfNewMessages(TabController tabController) async {
    return;
  }
}
