import 'package:Tracko/Utils/SettingUtil.dart';
import 'package:Tracko/Utils/WidgetUtil.dart';
import 'package:Tracko/controllers/AutoScanMsgController.dart';
import 'package:Tracko/pages/home_page/home_page.dart';
import 'package:Tracko/pages/settings_page/settings_page.dart';
import 'package:Tracko/pages/smartify_page/smartify_page.dart';
import 'package:Tracko/pages/split_page/SplitPage.dart';
import 'package:Tracko/pages/transaction_list_page/transaction_list_page.dart';
import 'package:Tracko/services/BackupService.dart';
import 'package:Tracko/services/SessionService.dart';
import 'package:bottom_navy_bar/bottom_navy_bar.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

const double tabIconSize = 30.0;

class HomeTab extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return new _HomeTab();
  }
}

class _HomeTab extends State<HomeTab> with SingleTickerProviderStateMixin {
  late TabController tabController;

  int _selectedIndex = 2;

  @override
  initState() {
    super.initState();
    tabController = TabController(length: 5, vsync: this, initialIndex: 2);
    tabController.addListener(() {
      _selectedIndex = tabController.index;
    });
    afterLoggingIn();
  }

  afterLoggingIn() {
    AutoScanMsgController.giveNotificationIfNewMessages(tabController);
    WidgetUtil.setHomeTab(this);
    BackupService.backupDatabaseIfRequired(
        SessionService
            .currentUser()
            .phoneNo);
  }

  @override
  dispose() {
    tabController.dispose();
    super.dispose();
  }

  final navlist = [
    BottomNavyBarItem(
      icon: Icon(Icons.account_circle),
      title: Text("Accounts"),
      activeColor: Colors.red,
    ),
    BottomNavyBarItem(
      icon: Icon(Icons.call_split),
      title: Text("Split"),
      activeColor: Colors.orange,
    ),
    BottomNavyBarItem(
      icon: Icon(Icons.home),
      title: Text("Home"),
      activeColor: Colors.teal,
    ),
    BottomNavyBarItem(
      icon: Icon(Icons.wb_incandescent),
      title: Text("Smartify"),
      activeColor: Colors.blue,
    ),
    BottomNavyBarItem(
      icon: Icon(Icons.settings),
      title: Text("Settings"),
      activeColor: Colors.blueGrey,
    ),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      bottomNavigationBar: BottomNavyBar(
        iconSize: 30,
        selectedIndex: _selectedIndex,
        showElevation: true,
        onItemSelected: (int selectedPos) {
          setState(() {
            _selectedIndex = selectedPos;
          });
          tabController.animateTo(selectedPos);
        },
        items: navlist,
      ),
      appBar: AppBar(
        leading: IconButton(
            iconSize: 35.0,
            icon: Icon(Icons.add),
            onPressed: () {
              Navigator.pushNamed(context, "/add_item");
            }),
        actions: <Widget>[
          IconButton(
            tooltip: "Current Month",
            icon: Text(
              DateFormat("MMM").format(SettingUtil.currentMonth),
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
            onPressed: () {},
          )
        ],
        title: Text("Trako"),
        backgroundColor: navlist[_selectedIndex].activeColor,
        centerTitle: true,
      ),
      body: TabBarView(
          physics: NeverScrollableScrollPhysics(),
          controller: tabController,
          children: <Widget>[
            TransactionListPage(),
            SplitPage(),
            HomePage(),
            SmartPage(),
            SettingsPage(),
          ]),
    );
  }
}
