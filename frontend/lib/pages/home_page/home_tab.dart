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
  // Mapping of bottom bar item positions (compact layout) to TabController indices.
  // We omit the 'Split' tab (index 2) on small screens to satisfy BottomNavyBar's
  // 2..5 item constraint while keeping 6 tabs available on wide screens via NavigationRail.
  // Order maps compact bottom bar positions -> TabController indices.
  // Keep Home (tab index 3) centered at bottom bar position 2.
  final List<int> _navTabIndices = const [0, 1, 2, 3, 4];

  @override
  initState() {
    super.initState();
    tabController = TabController(length: 5, vsync: this, initialIndex: 2);
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

  final navlist = [
    BottomNavyBarItem(
      icon: Icon(Icons.account_circle),
      title: Text("Accounts"),
      activeColor: Colors.red,
    ),
    BottomNavyBarItem(
      icon: Icon(Icons.monetization_on),
      title: Text("Budget"),
      activeColor: Colors.green,
    ),
    BottomNavyBarItem(
      icon: Icon(Icons.home),
      title: Text("Home"),
      activeColor: Colors.teal,
    ),
    BottomNavyBarItem(
      icon: Icon(Icons.call_split),
      title: Text("Split"),
      activeColor: Colors.orange,
    ),
    BottomNavyBarItem(
      icon: Icon(Icons.bar_chart),
      title: Text("Stats"),
      activeColor: Colors.blue,
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
                // Convert current tab index to the compact bottom bar's item index.
                selectedIndex: (() {
                  final idx = _navTabIndices.indexOf(_selectedIndex);
                  return idx >= 0 ? idx : 0;
                })(),
                showElevation: true,
                itemCornerRadius: 8,
                curve: Curves.easeInOut,
                backgroundColor: Colors.transparent,
                onItemSelected: (int selectedPos) {
                  setState(() {
                    // Map from bottom bar position to actual tab index
                    _selectedIndex = _navTabIndices[selectedPos];
                  });
                  tabController.animateTo(_selectedIndex);
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
                    {
                      'icon': Icons.monetization_on,
                      'label': 'Budget',
                      'tab': 1
                    },
                    {'icon': Icons.home, 'label': 'Home', 'tab': 2},
                    {'icon': Icons.call_split, 'label': 'Split', 'tab': 3},
                    {'icon': Icons.bar_chart, 'label': 'Stats', 'tab': 4},
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
                        BudgetPage(),
                        TransactionListPage(embedded: true),
                        SplitPage(),
                        StatsPage(),
                      ]),
                ),
              ],
            )
          : TabBarView(
              physics: NeverScrollableScrollPhysics(),
              controller: tabController,
              children: <Widget>[
                  AccountsOverviewPage(),
                  BudgetPage(),
                  TransactionListPage(embedded: true),
                  SplitPage(),
                  StatsPage(),
                ]),
    );
  }
}
