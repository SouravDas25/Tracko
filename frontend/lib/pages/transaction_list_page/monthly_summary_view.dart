import 'package:flutter/material.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/AppLog.dart';
import 'package:tracko/component/interfaces.dart';
import 'package:tracko/controllers/TransactionController.dart';
import 'package:tracko/models/transaction_period_summary.dart';
import 'package:pull_to_refresh/pull_to_refresh.dart';
import 'package:intl/intl.dart';

/// A view that displays a list of aggregated transaction summaries for each month in a specific year.
///
/// It includes:
/// - A sticky year navigation header for switching years.
/// - A scrollable, pull-to-refresh list of monthly summaries.
/// - Tapping on a month navigates the user to the `DailyTransactionView` for that specific month.
class MonthlySummaryView extends StatefulWidget {
  final int year;
  final List<int>? accountIds;
  final int? categoryId;
  final Function(int year, int month) onMonthSelected;
  final Function(int year) onYearChanged;

  const MonthlySummaryView({
    Key? key,
    required this.year,
    this.accountIds,
    this.categoryId,
    required this.onMonthSelected,
    required this.onYearChanged,
  }) : super(key: key);

  @override
  _MonthlySummaryViewState createState() => _MonthlySummaryViewState();
}

class _MonthlySummaryViewState extends RefreshableState<MonthlySummaryView> {
  RefreshController _refreshController = RefreshController();
  List<TransactionPeriodSummary> _summaries = [];
  bool _isLoading = false;
  late int _selectedYear;

  @override
  void initState() {
    _selectedYear = widget.year;
    super.initState();
  }

  @override
  void didUpdateWidget(MonthlySummaryView oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.year != oldWidget.year) {
      _selectedYear = widget.year;
      refresh();
    }
  }

  @override
  Future<void> refresh() async {
    if (_isLoading) return;
    try {
      if (mounted) setState(() => _isLoading = true);
      AppLog.d(
          '[MonthlySummaryView] refreshing for year=$_selectedYear accountIds=${widget.accountIds} categoryId=${widget.categoryId}');

      final summaries = await TransactionController.getMonthlySummaries(
        _selectedYear,
        accountIds: widget.accountIds,
        categoryId: widget.categoryId,
      );

      AppLog.d('[MonthlySummaryView] loaded ${summaries.length} summaries');

      if (mounted) {
        setState(() {
          _summaries = summaries;
          _isLoading = false;
        });
        _refreshController.refreshCompleted();
      }
    } catch (e) {
      AppLog.d('[MonthlySummaryView] refresh error: $e');
      if (mounted) setState(() => _isLoading = false);
      _refreshController.refreshFailed();
    }
  }

  @override
  asyncLoad() async {
    await refresh();
    loadCompleteView();
  }

  void _previousYear() {
    setState(() {
      _selectedYear--;
    });
    widget.onYearChanged(_selectedYear);
    refresh();
  }

  void _nextYear() {
    setState(() {
      _selectedYear++;
    });
    widget.onYearChanged(_selectedYear);
    refresh();
  }

  @override
  Widget completeWidget(BuildContext context) {
    return SmartRefresher(
      controller: _refreshController,
      enablePullDown: true,
      onRefresh: refresh,
      child: CustomScrollView(
        slivers: [
          SliverPersistentHeader(
            pinned: true,
            delegate: _StickyYearHeaderDelegate(
              context: context,
              year: _selectedYear,
              onPrevious: _previousYear,
              onNext: _nextYear,
            ),
          ),
          SliverPadding(
            padding: EdgeInsets.all(16),
            sliver: SliverList(
              delegate: SliverChildBuilderDelegate(
                (context, index) {
                  final summary = _summaries[index];
                  return _buildMonthItem(summary);
                },
                childCount: _summaries.length,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMonthItem(TransactionPeriodSummary summary) {
    final monthName =
        DateFormat('MMMM').format(DateTime(summary.year, summary.month!));

    return GestureDetector(
      onTap: () => widget.onMonthSelected(summary.year, summary.month!),
      child: Container(
        margin: EdgeInsets.only(bottom: 12),
        padding: EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Theme.of(context).cardColor,
          borderRadius: BorderRadius.circular(16),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.05),
              blurRadius: 10,
              offset: Offset(0, 4),
            ),
          ],
        ),
        child: Column(
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  monthName,
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                Container(
                  padding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: Theme.of(context).primaryColor.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    "${summary.count} txns",
                    style: TextStyle(
                      fontSize: 12,
                      color: Theme.of(context).primaryColor,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ],
            ),
            Divider(height: 24),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                _buildSummaryValue("Income", summary.income, Colors.green),
                _buildSummaryValue("Expense", summary.expense, Colors.red),
                _buildSummaryValue("Net", summary.netTotal,
                    summary.netTotal >= 0 ? Colors.green : Colors.red),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSummaryValue(String label, double amount, Color color) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: TextStyle(
            fontSize: 12,
            color: Theme.of(context).hintColor,
          ),
        ),
        SizedBox(height: 4),
        Text(
          CommonUtil.toCurrency(amount),
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.bold,
            color: color,
          ),
        ),
      ],
    );
  }

  @override
  Widget fallbackWidget(BuildContext context) {
    return Center(child: Text("No Data"));
  }
}

class _StickyYearHeaderDelegate extends SliverPersistentHeaderDelegate {
  final BuildContext context;
  final int year;
  final VoidCallback onPrevious;
  final VoidCallback onNext;

  _StickyYearHeaderDelegate({
    required this.context,
    required this.year,
    required this.onPrevious,
    required this.onNext,
  });

  @override
  Widget build(
      BuildContext context, double shrinkOffset, bool overlapsContent) {
    return Container(
      color: Theme.of(context).appBarTheme.backgroundColor ??
          Theme.of(context).primaryColor,
      height: 56,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          IconButton(
            icon: Icon(Icons.chevron_left, color: Colors.white),
            onPressed: onPrevious,
          ),
          Text(
            "$year",
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          IconButton(
            icon: Icon(Icons.chevron_right, color: Colors.white),
            onPressed: onNext,
          ),
        ],
      ),
    );
  }

  @override
  double get maxExtent => 56;

  @override
  double get minExtent => 56;

  @override
  bool shouldRebuild(_StickyYearHeaderDelegate oldDelegate) {
    return year != oldDelegate.year;
  }
}
