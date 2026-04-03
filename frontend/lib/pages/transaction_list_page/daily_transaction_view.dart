import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/SettingUtil.dart';
import 'package:tracko/component/amount_text.dart';
import 'package:tracko/component/smart_transaction_list.dart';
import 'package:tracko/component/interfaces.dart';
import 'package:tracko/controllers/TransactionController.dart';
import 'package:tracko/Utils/AppLog.dart';
import 'package:flutter/material.dart';

/// A view that displays a paginated list of individual transactions for a selected month.
///
/// It uses [SmartTransactionList] to manage fetching, pagination, and sticky month headers.
class DailyTransactionView extends StatefulWidget {
  final List<int>? initialAccountIds;
  final int? categoryId;
  final int? transactionType;
  final bool embedded;
  final DateTime? initialMonth;
  final Function(DateTime)? onMonthChanged;

  const DailyTransactionView({
    Key? key,
    this.initialAccountIds,
    this.categoryId,
    this.transactionType,
    this.embedded = false,
    this.initialMonth,
    this.onMonthChanged,
  }) : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _DailyTransactionViewState();
  }
}

class _DailyTransactionViewState extends State<DailyTransactionView> {
  double previousMonthAmount = 0.0;
  double totalAmount = 0.0;
  double incomeAmount = 0.0;
  double expenseAmount = 0.0;
  late DateTime selectedMonth;

  @override
  void initState() {
    super.initState();
    selectedMonth = widget.initialMonth ?? SettingUtil.currentMonth;
    _loadTotals();
  }

  @override
  void didUpdateWidget(covariant DailyTransactionView oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.initialMonth != null &&
        widget.initialMonth != oldWidget.initialMonth) {
      if (selectedMonth != widget.initialMonth) {
        selectedMonth = widget.initialMonth!;
        _loadTotals();
      }
    } else if (widget.initialMonth == null &&
        SettingUtil.currentMonth != selectedMonth) {
      selectedMonth = SettingUtil.currentMonth;
      _loadTotals();
    }
  }

  Future<void> _loadTotals() async {
    final currentMonthDate =
        DateTime.utc(selectedMonth.year, selectedMonth.month);
    final nextMonthDate =
        DateTime.utc(selectedMonth.year, selectedMonth.month + 1);

    final summary = await TransactionController.getSummaryBetween(
      currentMonthDate,
      nextMonthDate,
      accountIds: widget.initialAccountIds,
      categoryId: widget.categoryId,
    );

    if (mounted) {
      setState(() {
        incomeAmount = (summary['totalIncome'] as num?)?.toDouble() ?? 0.0;
        expenseAmount = (summary['totalExpense'] as num?)?.toDouble() ?? 0.0;
        previousMonthAmount =
            (summary['rolloverNet'] as num?)?.toDouble() ?? 0.0;
        totalAmount = (summary['netTotalWithRollover'] as num?)?.toDouble() ??
            (incomeAmount - expenseAmount + previousMonthAmount);
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return SmartTransactionList(
      accountIds: widget.initialAccountIds,
      categoryId: widget.categoryId,
      transactionType: widget.transactionType,
      viewMode: TransactionViewMode.monthlyNavigation,
      initialMonth: selectedMonth,
      onMonthChanged: (newMonth) {
        selectedMonth = newMonth;
        _loadTotals();
      },
      headerWidget: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12.0, vertical: 6.0),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [
                Theme.of(context).cardColor,
                Theme.of(context).cardColor.withOpacity(0.95),
              ],
            ),
            borderRadius: BorderRadius.circular(16),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.03),
                blurRadius: 10,
                offset: const Offset(0, 4),
              ),
            ],
            border: Border.all(
                color: Theme.of(context).dividerColor.withOpacity(0.05)),
          ),
          child: Column(
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    "Total Balance",
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w500,
                      color: Theme.of(context).hintColor,
                    ),
                  ),
                  Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                    decoration: BoxDecoration(
                      color: Theme.of(context).primaryColor.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(
                      "Net",
                      style: TextStyle(
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                        color: Theme.of(context).primaryColor,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 4),
              Align(
                alignment: Alignment.centerLeft,
                child: AmountText(
                  amount: totalAmount,
                  color: Theme.of(context).textTheme.bodyLarge?.color ?? Colors.white,
                  fontSize: 21,
                ),
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: _buildSummaryItem(
                      context,
                      "Income",
                      incomeAmount,
                      Colors.green.shade500,
                      Icons.arrow_downward_rounded,
                    ),
                  ),
                  Container(
                    width: 1,
                    height: 28,
                    color: Theme.of(context).dividerColor.withOpacity(0.2),
                  ),
                  Expanded(
                    child: _buildSummaryItem(
                      context,
                      "Expense",
                      expenseAmount,
                      Colors.red.shade400,
                      Icons.arrow_upward_rounded,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildSummaryItem(BuildContext context, String label, double amount,
      Color color, IconData icon) {
    return Column(
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(3),
              decoration: BoxDecoration(
                color: color.withOpacity(0.1),
                shape: BoxShape.circle,
              ),
              child: Icon(icon, size: 10, color: color),
            ),
            const SizedBox(width: 4),
            Text(
              label,
              style: TextStyle(
                fontSize: 11,
                fontWeight: FontWeight.w500,
                color: Theme.of(context).hintColor,
              ),
            ),
          ],
        ),
        const SizedBox(height: 2),
        AmountText(
          amount: amount,
          color: color,
          fontSize: 11,
        ),
      ],
    );
  }
}
