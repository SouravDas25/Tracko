import 'package:flutter/material.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/AppLog.dart';
import 'package:tracko/component/amount_text.dart';
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
    final hintStyle = TextStyle(
      fontSize: 11,
      fontWeight: FontWeight.w600,
      color: Theme.of(context).hintColor,
    );

    return GestureDetector(
      onHorizontalDragEnd: (details) {
        final v = details.primaryVelocity;
        if (v == null) return;
        if (v < -300) _nextYear();
        if (v > 300) _previousYear();
      },
      child: SmartRefresher(
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
            SliverToBoxAdapter(
              child: Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                color: Theme.of(context).cardColor,
                child: Row(
                  children: [
                    Expanded(flex: 3, child: Text("Month", style: hintStyle)),
                    Expanded(
                        flex: 3,
                        child: Text("Income",
                            style: hintStyle, textAlign: TextAlign.right)),
                    Expanded(
                        flex: 3,
                        child: Text("Expense",
                            style: hintStyle, textAlign: TextAlign.right)),
                    Expanded(
                        flex: 3,
                        child: Text("Net",
                            style: hintStyle, textAlign: TextAlign.right)),
                  ],
                ),
              ),
            ),
            SliverToBoxAdapter(
              child: Divider(
                  height: 0.5,
                  thickness: 0.5,
                  color: Theme.of(context).dividerColor.withOpacity(0.2)),
            ),
            SliverList(
              delegate: SliverChildBuilderDelegate(
                (context, index) => _buildMonthRow(_summaries[index], index),
                childCount: _summaries.length,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMonthRow(TransactionPeriodSummary summary, int index) {
    final monthName =
        DateFormat('MMM').format(DateTime(summary.year, summary.month!));

    return Material(
      color: index.isEven
          ? Colors.transparent
          : Theme.of(context).cardColor.withOpacity(0.4),
      child: InkWell(
        onTap: () => widget.onMonthSelected(summary.year, summary.month!),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          decoration: BoxDecoration(
            border: Border(
              bottom: BorderSide(
                color: Theme.of(context).dividerColor.withOpacity(0.06),
                width: 0.5,
              ),
            ),
          ),
          child: Row(
            children: [
              Expanded(
                flex: 3,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(monthName,
                        style: TextStyle(
                            fontSize: 14, fontWeight: FontWeight.w600)),
                    SizedBox(height: 1),
                    Text("${summary.count} txns",
                        style: TextStyle(
                            fontSize: 10, color: Theme.of(context).hintColor)),
                  ],
                ),
              ),
              Expanded(
                flex: 3,
                child: AmountText(
                  amount: summary.income,
                  color: Colors.green,
                  fontSize: 13,
                  textAlign: TextAlign.right,
                ),
              ),
              Expanded(
                flex: 3,
                child: AmountText(
                  amount: summary.expense,
                  color: Colors.red,
                  fontSize: 13,
                  textAlign: TextAlign.right,
                ),
              ),
              Expanded(
                flex: 3,
                child: AmountText(
                  amount: summary.netTotal,
                  color: summary.netTotal >= 0 ? Colors.green : Colors.red,
                  fontSize: 13,
                  textAlign: TextAlign.right,
                ),
              ),
            ],
          ),
        ),
      ),
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
      height: 44,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          IconButton(
            icon: Icon(Icons.chevron_left, color: Colors.white, size: 22),
            onPressed: onPrevious,
          ),
          Text(
            "$year",
            style: TextStyle(
              fontSize: 17,
              fontWeight: FontWeight.bold,
              color: Colors.white,
            ),
          ),
          IconButton(
            icon: Icon(Icons.chevron_right, color: Colors.white, size: 22),
            onPressed: onNext,
          ),
        ],
      ),
    );
  }

  @override
  double get maxExtent => 44;

  @override
  double get minExtent => 44;

  @override
  bool shouldRebuild(_StickyYearHeaderDelegate oldDelegate) {
    return year != oldDelegate.year;
  }
}
