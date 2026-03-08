import 'package:flutter/material.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/component/interfaces.dart';
import 'package:tracko/controllers/TransactionController.dart';
import 'package:tracko/models/transaction_period_summary.dart';
import 'package:pull_to_refresh/pull_to_refresh.dart';

/// A view that displays a list of aggregated transaction summaries for all active years.
///
/// It includes:
/// - A scrollable, pull-to-refresh list of yearly summaries.
/// - Tapping on a year navigates the user to the `MonthlySummaryView` for that specific year.
class YearlySummaryView extends StatefulWidget {
  final List<int>? accountIds;
  final int? categoryId;
  final Function(int year) onYearSelected;

  const YearlySummaryView({
    Key? key,
    this.accountIds,
    this.categoryId,
    required this.onYearSelected,
  }) : super(key: key);

  @override
  _YearlySummaryViewState createState() => _YearlySummaryViewState();
}

class _YearlySummaryViewState extends RefreshableState<YearlySummaryView> {
  RefreshController _refreshController = RefreshController();
  List<TransactionPeriodSummary> _summaries = [];
  bool _isLoading = false;

  @override
  Future<void> refresh() async {
    if (_isLoading) return;
    try {
      if (mounted) setState(() => _isLoading = true);

      final summaries = await TransactionController.getYearlySummaries(
        accountIds: widget.accountIds,
        categoryId: widget.categoryId,
      );

      if (mounted) {
        setState(() {
          _summaries = summaries;
          _isLoading = false;
        });
        _refreshController.refreshCompleted();
      }
    } catch (e) {
      if (mounted) setState(() => _isLoading = false);
      _refreshController.refreshFailed();
    }
  }

  @override
  asyncLoad() async {
    await refresh();
    loadCompleteView();
  }

  @override
  Widget completeWidget(BuildContext context) {
    return SmartRefresher(
      controller: _refreshController,
      enablePullDown: true,
      onRefresh: refresh,
      child: ListView.builder(
        padding: EdgeInsets.all(16),
        itemCount: _summaries.length,
        itemBuilder: (context, index) {
          final summary = _summaries[index];
          return _buildYearItem(summary);
        },
      ),
    );
  }

  Widget _buildYearItem(TransactionPeriodSummary summary) {
    return GestureDetector(
      onTap: () => widget.onYearSelected(summary.year),
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
                  "${summary.year}",
                  style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                Container(
                  padding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: Theme.of(context).appBarTheme.backgroundColor ??
                        Theme.of(context).primaryColor,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    "${summary.count} txns",
                    style: TextStyle(
                      fontSize: 12,
                      color: Theme.of(context).appBarTheme.backgroundColor ??
                          Theme.of(context).primaryColor,
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
