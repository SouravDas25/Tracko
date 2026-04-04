import 'package:flutter/material.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/component/amount_text.dart';
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
    final hintStyle = TextStyle(
      fontSize: 11,
      fontWeight: FontWeight.w600,
      color: Theme.of(context).hintColor,
    );

    return SmartRefresher(
      controller: _refreshController,
      enablePullDown: true,
      onRefresh: refresh,
      child: ListView(
        padding: EdgeInsets.zero,
        children: [
          // Table header
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
            color: Theme.of(context).cardColor,
            child: Row(
              children: [
                Expanded(flex: 3, child: Text("Year", style: hintStyle)),
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
          Divider(
              height: 0.5,
              thickness: 0.5,
              color: Theme.of(context).dividerColor.withOpacity(0.2)),
          // Data rows
          for (int i = 0; i < _summaries.length; i++)
            _buildYearRow(_summaries[i], i),
        ],
      ),
    );
  }

  Widget _buildYearRow(TransactionPeriodSummary summary, int index) {
    return Material(
      color: index.isEven
          ? Colors.transparent
          : Theme.of(context).cardColor.withOpacity(0.4),
      child: InkWell(
        onTap: () => widget.onYearSelected(summary.year),
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
                    Text(
                      "${summary.year}",
                      style:
                          TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
                    ),
                    SizedBox(height: 1),
                    Text(
                      "${summary.count} txns",
                      style: TextStyle(
                          fontSize: 10, color: Theme.of(context).hintColor),
                    ),
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
