import 'package:expense_manager/pages/accounts_page/accounts_page.dart';
import 'package:expense_manager/pages/home_page/home_page.dart';
import 'package:expense_manager/pages/settings_page/settings_page.dart';
import 'package:expense_manager/pages/smartify_page/smartify_page.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

class HomeTab extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return new _HomeTab();
  }
}

class _HomeTab extends State<HomeTab> with SingleTickerProviderStateMixin {
  TabController tabController;

  @override
  initState() {
    super.initState();
    tabController = TabController(length: 4, vsync: this);
    tabController.index = 1;
  }

  @override
  dispose() {
    tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      bottomNavigationBar: TabBar(
          controller: tabController,
          labelColor: Colors.black,
          tabs: <Widget>[
            Tab(
              icon: Icon(Icons.account_circle),
              text: "Accounts",
            ),
            Tab(
              icon: Icon(Icons.home),
              text: "Home",
            ),
            Tab(
              icon: Icon(Icons.wb_incandescent),
              text: "Smartify",
            ),
            Tab(
              icon: Icon(Icons.settings),
              text: "Settings",
            )
          ]),
      appBar: AppBar(
        actions: <Widget>[
          IconButton(
            tooltip: "Current Month",
            icon: Text(DateFormat("MMM").format(DateTime.now()),style: TextStyle(fontWeight: FontWeight.bold),),
            onPressed: () {},
          )
        ],
        title: Text("Expense Manager"),
        centerTitle: true,
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          Navigator.pushNamed(context, "/add_item");
        },
        child: Icon(Icons.add),
      ),
      body: TabBarView(controller: tabController, children: <Widget>[
        AccountsPage(),
        HomePage(),
        SmartPage(),
        SettingsPage(),
      ]),
    );
  }
}
