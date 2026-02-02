import 'package:tracko/Utils/SettingUtil.dart';
import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/pages/home_page/home_page.dart';
import 'package:tracko/pages/settings_page/settings_page.dart';
import 'package:tracko/pages/stats_page/stats_page.dart';
import 'package:tracko/pages/split_page/SplitPage.dart';
import 'package:tracko/pages/transaction_list_page/transaction_list_page.dart';
import 'package:tracko/pages/account_page/AccountPage.dart';
import 'package:tracko/pages/account_page/accounts_overview_page.dart';
import 'package:bottom_navy_bar/bottom_navy_bar.dart';
import 'package:flutter/foundation.dart';
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
    WidgetUtil.setHomeTab(this);
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
      icon: Icon(Icons.bar_chart),
      title: Text("Stats"),
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
    final double _width = MediaQuery.of(context).size.width;
    final bool isWide = _width >= 900;
    final bool isVeryWide = _width >= 1200;
    return Scaffold(
      bottomNavigationBar: isWide
          ? null
          : Container(
              decoration: BoxDecoration(
                color: Theme.of(context)
                        .bottomNavigationBarTheme
                        .backgroundColor ??
                    Theme.of(context).cardColor,
                border: const Border(
                  top: BorderSide(
                    color: Color(0x3DFFFFFF),
                    width: 0.5,
                  ),
                ),
              ),
              child: BottomNavyBar(
                iconSize: 22,
                selectedIndex: _selectedIndex,
                showElevation: true,
                itemCornerRadius: 8,
                curve: Curves.easeInOut,
                backgroundColor: Colors.transparent,
                onItemSelected: (int selectedPos) {
                  setState(() {
                    _selectedIndex = selectedPos;
                  });
                  tabController.animateTo(selectedPos);
                },
                items: navlist,
              ),
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
        centerTitle: true,
      ),
      body: isWide
          ? Row(
              children: [
                Builder(builder: (context) {
                  final railItems = [
                    {'icon': Icons.home, 'label': 'Home', 'tab': 2},
                    {
                      'icon': Icons.account_circle,
                      'label': 'Accounts',
                      'tab': 0
                    },
                    {'icon': Icons.call_split, 'label': 'Split', 'tab': 1},
                    {'icon': Icons.bar_chart, 'label': 'Stats', 'tab': 3},
                    {'icon': Icons.settings, 'label': 'Settings', 'tab': 4},
                  ];
                  int railSelected =
                      railItems.indexWhere((e) => e['tab'] == _selectedIndex);
                  if (railSelected < 0) railSelected = 0;
                  return NavigationRail(
                    selectedIndex: railSelected,
                    onDestinationSelected: (int idx) {
                      final targetTab = railItems[idx]['tab'] as int;
                      setState(() {
                        _selectedIndex = targetTab;
                      });
                      tabController.animateTo(targetTab);
                    },
                    // When extended is true, labelType must be null/none per API contract
                    labelType: isVeryWide
                        ? NavigationRailLabelType.none
                        : NavigationRailLabelType.all,
                    extended: isVeryWide,
                    destinations: railItems
                        .map((e) => NavigationRailDestination(
                              icon: Icon(e['icon'] as IconData),
                              label: Text(e['label'] as String),
                            ))
                        .toList(),
                  );
                }),
                const VerticalDivider(width: 1),
                Expanded(
                  child: TabBarView(
                      physics: NeverScrollableScrollPhysics(),
                      controller: tabController,
                      children: <Widget>[
                        AccountsOverviewPage(),
                        SplitPage(),
                        TransactionListPage(embedded: true),
                        StatsPage(),
                        SettingsPage(),
                      ]),
                ),
              ],
            )
          : TabBarView(
              physics: NeverScrollableScrollPhysics(),
              controller: tabController,
              children: <Widget>[
                  AccountsOverviewPage(),
                  SplitPage(),
                  TransactionListPage(embedded: true),
                  StatsPage(),
                  SettingsPage(),
                ]),
    );
  }
}
