import 'package:flutter/material.dart';
import 'package:tracko/Utils/SettingUtil.dart';
import 'package:tracko/pages/transaction_list_page/daily_transaction_view.dart';
import 'package:tracko/pages/transaction_list_page/monthly_summary_view.dart';
import 'package:tracko/pages/transaction_list_page/yearly_summary_view.dart';

class TransactionListPage extends StatefulWidget {
  final List<int>? initialAccountIds;
  final bool embedded;

  const TransactionListPage({
    Key? key,
    this.initialAccountIds,
    this.embedded = false,
  }) : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _TransactionListPageState();
  }
}

class _TransactionListPageState extends State<TransactionListPage>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  int _selectedYear = DateTime.now().year;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
    _selectedYear = SettingUtil.currentMonth.year;
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  void didUpdateWidget(TransactionListPage oldWidget) {
    super.didUpdateWidget(oldWidget);
    // Sync selected year with global current month if it changes externally or via Daily view
    if (SettingUtil.currentMonth.year != _selectedYear) {
      setState(() {
        _selectedYear = SettingUtil.currentMonth.year;
      });
    }
  }

  void _navigateToMonth(int year, int month) {
    setState(() {
      _selectedYear = year;
    });
    SettingUtil.setSelectedMonth(DateTime.utc(year, month));
    _tabController.animateTo(0); // Daily Tab
  }

  void _navigateToYear(int year) {
    setState(() {
      _selectedYear = year;
    });
    _tabController.animateTo(1); // Monthly Tab
  }

  @override
  Widget build(BuildContext context) {
    final content = Column(
      children: [
        Container(
          color: Theme.of(context).appBarTheme.backgroundColor ??
              Theme.of(context).primaryColor,
          child: TabBar(
            controller: _tabController,
            labelColor: Colors.white,
            unselectedLabelColor: Colors.white60,
            indicatorColor: Colors.white,
            tabs: [
              Tab(text: "Daily"),
              Tab(text: "Monthly"),
              Tab(text: "Yearly"),
            ],
          ),
        ),
        Expanded(
          child: TabBarView(
            controller: _tabController,
            children: [
              DailyTransactionView(
                key: ValueKey(SettingUtil.currentMonth),
                initialAccountIds: widget.initialAccountIds,
                embedded: widget.embedded,
              ),
              MonthlySummaryView(
                year: _selectedYear,
                accountIds: widget.initialAccountIds,
                onMonthSelected: _navigateToMonth,
                onYearChanged: (year) {
                  setState(() {
                    _selectedYear = year;
                  });
                },
              ),
              YearlySummaryView(
                accountIds: widget.initialAccountIds,
                onYearSelected: _navigateToYear,
              ),
            ],
          ),
        ),
      ],
    );

    if (widget.embedded) {
      return content;
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('Transactions'),
        centerTitle: true,
        elevation: 0,
      ),
      body: content,
      floatingActionButton: FloatingActionButton(
        onPressed: () async {
          await Navigator.pushNamed(context, '/add_item');
          setState(() {});
        },
        child: const Icon(Icons.add),
      ),
    );
  }
}
