import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/SettingUtil.dart';
import 'package:tracko/component/PageWidget.dart';
import 'package:tracko/component/TimedList.dart';
import 'package:tracko/component/TransactionTile.dart';
import 'package:tracko/component/interfaces.dart';
import 'package:tracko/component/month_picker_dialog.dart';
import 'package:tracko/controllers/TransactionController.dart';
import 'package:tracko/models/transaction.dart';
import 'package:tracko/Utils/AppLog.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart' as DateFormatter;
import 'package:pull_to_refresh/pull_to_refresh.dart';

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

class _TransactionListPageState extends RefreshableState<TransactionListPage> {
  RefreshController refreshController = new RefreshController();
  List<Transaction> transactions = [];
  List<dynamic> selections = [];
  double previousMonthAmount = 0.0;
  double totalAmount = 0.0;
  double incomeAmount = 0.0;
  double expenseAmount = 0.0;
  DateTime selectedMonth = SettingUtil.currentMonth;

  _TransactionListPageState();

  Future<void> _goToPreviousMonth() async {
    setState(() {
      selectedMonth = DateTime.utc(selectedMonth.year, selectedMonth.month - 1);
    });
    SettingUtil.setSelectedMonth(selectedMonth);
    await refresh();
  }

  Future<void> _goToNextMonth() async {
    setState(() {
      selectedMonth = DateTime.utc(selectedMonth.year, selectedMonth.month + 1);
    });
    SettingUtil.setSelectedMonth(selectedMonth);
    await refresh();
  }

  Future<void> _selectMonth() async {
    final DateTime? picked = await showMonthPicker(
      context: context,
      initialDate: selectedMonth,
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
    );

    if (picked != null) {
      final next = DateTime.utc(picked.year, picked.month);
      if (next != selectedMonth) {
        setState(() {
          selectedMonth = next;
        });
        SettingUtil.setSelectedMonth(selectedMonth);
        await refresh();
      }
    }
  }

  @override
  void dispose() {
    refreshController.dispose();
    super.dispose();
  }

  @override
  asyncLoad() async {
    AppLog.d(
        '[TRACE][TransactionListPage] asyncLoad start embedded=${widget.embedded} initialAccountIds=${widget.initialAccountIds}');
    try {
      // Pre-seed selections from initialAccountIds if provided
      if (widget.initialAccountIds != null &&
          widget.initialAccountIds!.isNotEmpty) {
        selections = List<dynamic>.from(widget.initialAccountIds!);
      }

      selectedMonth = SettingUtil.currentMonth;
      AppLog.d('[TRACE][TransactionListPage] selectedMonth=$selectedMonth');

      await refresh();
      this.loadCompleteView();
      AppLog.d('[TRACE][TransactionListPage] asyncLoad complete');
    } catch (e) {
      AppLog.d('[TRACE][TransactionListPage] asyncLoad error: $e');
      if (mounted) {
        // If not logged in or session invalid, go to welcome/login flow.
        Navigator.pushReplacementNamed(context, '/welcome');
      }
    }
  }

  @override
  Future<void> refresh() async {
    AppLog.d('[TRACE][TransactionListPage] refresh start month=$selectedMonth');
    try {
      await initTransactionData();
      if (this.mounted) {
        setState(() {
          refreshController.refreshCompleted();
        });
      }
      AppLog.d(
          '[TRACE][TransactionListPage] refresh complete txCount=${transactions.length}');
    } catch (e) {
      AppLog.d('[TRACE][TransactionListPage] refresh error: $e');
      // Likely unauthenticated; redirect to welcome/login.
      if (mounted) {
        Navigator.pushReplacementNamed(context, '/welcome');
      }
    }
  }

  initTransactionData() async {
    AppLog.d(
        '[TRACE][TransactionListPage] initTransactionData start selections=$selections');
    transactions.clear();
    List<int> accountIds = [];
    if (selections != null && selections.length > 0) {
      int accountId;
      for (int i = 0; i < selections.length; i++) {
        accountId = int.parse(selections[i].toString());
        accountIds.add(accountId);
      }
    }
    AppLog.d(
        '[TRACE][TransactionListPage] parsed accountIds=$accountIds month=$selectedMonth');
    // Load all transactions for the selected month (no pagination)
    transactions = await TransactionController.getTransactionsForSelectedMonth(
        accountIds: accountIds, month: selectedMonth);
    AppLog.d(
        '[TRACE][TransactionListPage] fetched transactions count=${transactions.length}');

    totalAmount = incomeAmount = expenseAmount = 0;

    incomeAmount = await TransactionController.getMonthIncome(selectedMonth,
        accountIds: accountIds);
    expenseAmount = await TransactionController.getMonthExpense(selectedMonth,
        accountIds: accountIds);
    AppLog.d(
        '[TRACE][TransactionListPage] summary income=$incomeAmount expense=$expenseAmount');

    // Calculate previous month relative to selectedMonth
    final prevMonthDate =
        DateTime.utc(selectedMonth.year, selectedMonth.month - 1);
    final currentMonthDate =
        DateTime.utc(selectedMonth.year, selectedMonth.month);

    final prevSummary = await TransactionController.getSummaryBetween(
      prevMonthDate,
      currentMonthDate,
      accountIds: accountIds,
    );
    previousMonthAmount = (prevSummary['netTotal'] as num?)?.toDouble() ?? 0.0;

    totalAmount = incomeAmount - expenseAmount + previousMonthAmount;
    AppLog.d(
        '[TRACE][TransactionListPage] totals previous=$previousMonthAmount balance=$totalAmount');
  }

  Widget _buildContent() {
    return SmartRefresher(
      controller: refreshController,
      enablePullDown: true,
      enablePullUp: false,
      onRefresh: () async {
        await refresh();
      },
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        children: <Widget>[
          Padding(
            padding: const EdgeInsets.only(top: 4.0),
            child: DefaultTextStyle.merge(
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w600,
              ),
              child: Card(
                child: ListTile(
                  leading: IconButton(
                    icon: const Icon(Icons.arrow_back),
                    onPressed: _goToPreviousMonth,
                  ),
                  title: GestureDetector(
                    onTap: _selectMonth,
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          DateFormatter.DateFormat('MMMM yyyy')
                              .format(selectedMonth),
                          style: const TextStyle(fontSize: 18),
                        ),
                        const Icon(Icons.arrow_drop_down),
                      ],
                    ),
                  ),
                  trailing: IconButton(
                    icon: const Icon(Icons.arrow_forward),
                    onPressed: _goToNextMonth,
                  ),
                ),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(top: 4.0),
            child: Card(
              elevation: 2,
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16.0),
                  side: BorderSide(
                      color: Theme.of(context).dividerColor.withOpacity(0.1))),
              child: ListView(
                primary: false,
                shrinkWrap: true,
                children: <Widget>[
                  ListTile(
                    trailing: Text(
                      CommonUtil.toCurrency(previousMonthAmount),
                      style: TextStyle(
                          fontSize: 18.0, fontWeight: FontWeight.w500),
                    ),
                    dense: true,
                    title: Text(
                      "Last Month (${DateFormatter.DateFormat("MMM").format(DateTime.utc(selectedMonth.year, selectedMonth.month - 1))})",
                      style: TextStyle(fontSize: 18.0),
                    ),
                  ),
                  ListTile(
                    trailing: Text(
                      CommonUtil.toCurrency(incomeAmount),
                      style: TextStyle(
                          fontSize: 18.0, fontWeight: FontWeight.w500),
                    ),
                    dense: true,
                    title: Text(
                      "Income",
                      style: TextStyle(fontSize: 18.0),
                    ),
                  ),
                  ListTile(
                    trailing: Text(
                      CommonUtil.toCurrency(expenseAmount),
                      style: TextStyle(
                          fontSize: 18.0, fontWeight: FontWeight.w500),
                    ),
                    dense: true,
                    title: Text(
                      "Expense",
                      style: TextStyle(fontSize: 18.0),
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 15.0, vertical: 0.0),
                    child: Container(
                      height: 0.5,
                      color: Colors.black,
                    ),
                  ),
                  ListTile(
                    trailing: Text(
                      CommonUtil.toCurrency(totalAmount),
                      style: TextStyle(
                          fontSize: 18.0, fontWeight: FontWeight.w500),
                    ),
                    title: Text(
                      "Balance",
                      style: TextStyle(fontSize: 18.0),
                    ),
                  )
                ],
              ),
            ),
          ),
          if (transactions.isEmpty)
            Padding(
              padding: const EdgeInsets.all(32.0),
              child: Center(
                child: Text(
                  "No transactions found for this month.",
                  style: TextStyle(color: Colors.grey, fontSize: 16),
                ),
              ),
            )
          else
            Padding(
              padding: EdgeInsets.symmetric(vertical: 4.0),
              child: TimedList(
                itemCount: transactions.length,
                timeField: (int index) {
                  Transaction transaction = transactions[index];
                  return transaction.date;
                },
                itemBuilder: (BuildContext context, int index) {
                  Transaction transaction = transactions[index];
                  return TransactionTile(this, transaction,
                      (dynamic parent, Transaction transaction) async {
                    await parent.refresh();
                  });
                },
              ),
            ),
        ],
      ),
    );
  }

  @override
  Widget completeWidget(BuildContext context) {
    if (widget.embedded) {
      return _buildContent();
    }
    return Scaffold(
      appBar: AppBar(
        title: const Text('Transactions'),
        centerTitle: true,
      ),
      body: _buildContent(),
      floatingActionButton: FloatingActionButton(
        onPressed: () async {
          await Navigator.pushNamed(context, '/add_item');
          await refresh();
        },
        child: const Icon(Icons.add),
      ),
    );
  }

  @override
  Widget fallbackWidget(BuildContext context) {
    return Center(
      child: Text("No Data Available."),
    );
  }
}
