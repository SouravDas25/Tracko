import 'package:tracko/Utils/SettingUtil.dart';
import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/pages/budget_page/budget_page.dart';
import 'package:tracko/pages/home_page/home_page.dart';
import 'package:tracko/pages/settings_page/settings_page.dart';
import 'package:tracko/pages/stats_page/stats_page.dart';
import 'package:tracko/pages/split_page/SplitPage.dart';
import 'package:tracko/pages/transaction_list_page/transaction_list_page.dart';
import 'package:tracko/pages/account_page/AccountPage.dart';
import 'package:tracko/pages/account_page/accounts_overview_page.dart';
import 'package:tracko/pages/analytics_page/analytics_page.dart';
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

  int _selectedIndex = 3;
  // Keep Home (tab index 3) centered.

  @override
  initState() {
    super.initState();
    tabController = TabController(length: 6, vsync: this, initialIndex: 3);
    tabController.addListener(() {
      // Keep the tab index as the source of truth
      setState(() {
        _selectedIndex = tabController.index;
      });
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

  final destinations = const [
    NavigationDestination(
      icon: Icon(Icons.account_circle),
      label: "Accounts",
    ),
    NavigationDestination(
      icon: Icon(Icons.call_split),
      label: "Split",
    ),
    NavigationDestination(
      icon: Icon(Icons.monetization_on),
      label: "Budget",
    ),
    NavigationDestination(
      icon: Icon(Icons.home),
      label: "Home",
    ),
    NavigationDestination(
      icon: Icon(Icons.bar_chart),
      label: "Stats",
    ),
    NavigationDestination(
      icon: Icon(Icons.analytics),
      label: "Analytics",
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
          : NavigationBar(
              selectedIndex: _selectedIndex,
              onDestinationSelected: (int index) {
                setState(() {
                  _selectedIndex = index;
                });
                tabController.animateTo(index);
              },
              destinations: destinations,
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
          ),
          IconButton(
            tooltip: "Stats",
            icon: const Icon(Icons.bar_chart),
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (_) => StatsPage(
                    initialDate: SettingUtil.currentMonth,
                    showAppBar: true,
                  ),
                ),
              );
            },
          ),
          IconButton(
            tooltip: "Settings",
            icon: Icon(Icons.settings),
            onPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(builder: (_) => SettingsPage()),
              );
            },
          ),
        ],
        title: Text("Trako"),
        centerTitle: true,
      ),
      body: isWide
          ? Row(
              children: [
                Builder(builder: (context) {
                  final railItems = [
                    {
                      'icon': Icons.account_circle,
                      'label': 'Accounts',
                      'tab': 0
                    },
                    {'icon': Icons.call_split, 'label': 'Split', 'tab': 1},
                    {
                      'icon': Icons.monetization_on,
                      'label': 'Budget',
                      'tab': 2
                    },
                    {'icon': Icons.home, 'label': 'Home', 'tab': 3},
                    {'icon': Icons.bar_chart, 'label': 'Stats', 'tab': 4},
                    {'icon': Icons.analytics, 'label': 'Analytics', 'tab': 5},
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
                        BudgetPage(),
                        TransactionListPage(embedded: true),
                        StatsPage(),
                        AnalyticsPage(showAppBar: false),
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
                  BudgetPage(),
                  TransactionListPage(embedded: true),
                  StatsPage(),
                  AnalyticsPage(showAppBar: false),
                ]),
    );
  }
}
