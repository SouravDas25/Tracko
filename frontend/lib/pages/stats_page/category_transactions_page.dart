import 'package:flutter/material.dart';
import 'package:tracko/Utils/SettingUtil.dart';
import 'package:tracko/models/category.dart';
import 'package:tracko/pages/transaction_list_page/daily_transaction_view.dart';
import 'package:tracko/pages/transaction_list_page/monthly_summary_view.dart';
import 'package:tracko/pages/transaction_list_page/yearly_summary_view.dart';

/// A detailed view displaying transactions for a specific category with 3 levels of granularity:
/// 1. Daily: Detailed transactions for a specific month ([DailyTransactionView]).
/// 2. Monthly: Aggregated summaries for each month in a specific year ([MonthlySummaryView]).
/// 3. Yearly: Aggregated summaries for all years ([YearlySummaryView]).
class CategoryTransactionsPage extends StatefulWidget {
  final Category category;
  final DateTime startDate;
  final DateTime endDate;
  final String
      range; // 'all', 'thisMonth', 'lastMonth', etc. (not fully used in tab view but kept for compat)
  final int transactionType; // DEBIT or CREDIT
  final int? accountId;
  final String? accountName;

  const CategoryTransactionsPage({
    super.key,
    required this.category,
    required this.startDate,
    required this.endDate,
    required this.range,
    required this.transactionType,
    this.accountId,
    this.accountName,
  });

  @override
  State<CategoryTransactionsPage> createState() =>
      _CategoryTransactionsPageState();
}

class _CategoryTransactionsPageState extends State<CategoryTransactionsPage>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  late DateTime _currentDate;
  int _selectedYear = DateTime.now().year;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 3, vsync: this);
    _currentDate = widget.startDate;
    _selectedYear = widget.startDate.year;
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  void _navigateToMonth(int year, int month) {
    setState(() {
      _selectedYear = year;
      _currentDate = DateTime.utc(year, month);
    });
    SettingUtil.setSelectedMonth(DateTime.utc(year, month));
    _tabController.animateTo(0);
  }

  void _navigateToYear(int year) {
    setState(() {
      _selectedYear = year;
    });
    _tabController.animateTo(1);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.accountName != null
            ? "${widget.accountName} - ${widget.category.name}"
            : widget.category.name),
        centerTitle: true,
        elevation: 0,
      ),
      body: Column(
        children: [
          Container(
            color: Theme.of(context).appBarTheme.backgroundColor ??
                Theme.of(context).primaryColor,
            child: TabBar(
              controller: _tabController,
              labelColor: Colors.white,
              unselectedLabelColor: Colors.white60,
              indicatorColor: Colors.white,
              tabs: const [
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
                  initialAccountIds:
                      widget.accountId != null ? [widget.accountId!] : null,
                  categoryId: widget.category.id,
                  transactionType: widget.transactionType,
                  embedded: true,
                  initialMonth: _currentDate,
                  onMonthChanged: (date) {
                    setState(() {
                      _currentDate = date;
                      if (_selectedYear != date.year) {
                        _selectedYear = date.year;
                      }
                    });
                  },
                ),
                MonthlySummaryView(
                  year: _selectedYear,
                  accountIds:
                      widget.accountId != null ? [widget.accountId!] : null,
                  categoryId: widget.category.id,
                  onMonthSelected: _navigateToMonth,
                  onYearChanged: (year) {
                    setState(() {
                      _selectedYear = year;
                    });
                  },
                ),
                YearlySummaryView(
                  accountIds:
                      widget.accountId != null ? [widget.accountId!] : null,
                  categoryId: widget.category.id,
                  onYearSelected: _navigateToYear,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
