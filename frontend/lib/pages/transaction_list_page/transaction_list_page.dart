import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/SettingUtil.dart';
import 'package:tracko/component/PageWidget.dart';
import 'package:tracko/component/PaddedText.dart';
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
  bool _isProgrammaticLoading = false;

  _TransactionListPageState();

  Future<void> _goToPreviousMonth() async {
    if (_isProgrammaticLoading) {
      AppLog.d('[TransactionListPage] skip prev month: already loading');
      return;
    }
    if (mounted) {
      setState(() {
        _isProgrammaticLoading = true;
        selectedMonth =
            DateTime.utc(selectedMonth.year, selectedMonth.month - 1);
      });
    } else {
      selectedMonth = DateTime.utc(selectedMonth.year, selectedMonth.month - 1);
    }
    SettingUtil.setSelectedMonth(selectedMonth);
    try {
      await refresh();
    } finally {
      if (mounted) {
        setState(() {
          _isProgrammaticLoading = false;
        });
      } else {
        _isProgrammaticLoading = false;
      }
    }
  }

  Future<void> _goToNextMonth() async {
    if (_isProgrammaticLoading) {
      AppLog.d('[TransactionListPage] skip next month: already loading');
      return;
    }
    if (mounted) {
      setState(() {
        _isProgrammaticLoading = true;
        selectedMonth =
            DateTime.utc(selectedMonth.year, selectedMonth.month + 1);
      });
    } else {
      selectedMonth = DateTime.utc(selectedMonth.year, selectedMonth.month + 1);
    }
    SettingUtil.setSelectedMonth(selectedMonth);
    try {
      await refresh();
    } finally {
      if (mounted) {
        setState(() {
          _isProgrammaticLoading = false;
        });
      } else {
        _isProgrammaticLoading = false;
      }
    }
  }

  Future<void> _selectMonth() async {
    if (_isProgrammaticLoading) {
      AppLog.d('[TransactionListPage] skip month picker: already loading');
      return;
    }
    final DateTime? picked = await showMonthPicker(
      context: context,
      initialDate: selectedMonth,
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
    );

    if (picked != null) {
      final next = DateTime.utc(picked.year, picked.month);
      if (next != selectedMonth) {
        if (mounted) {
          setState(() {
            _isProgrammaticLoading = true;
            selectedMonth = next;
          });
        } else {
          selectedMonth = next;
        }
        SettingUtil.setSelectedMonth(selectedMonth);
        try {
          await refresh();
        } finally {
          if (mounted) {
            setState(() {
              _isProgrammaticLoading = false;
            });
          } else {
            _isProgrammaticLoading = false;
          }
        }
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
        '[TransactionListPage] asyncLoad start embedded=${widget.embedded} initialAccountIds=${widget.initialAccountIds}');
    try {
      // Pre-seed selections from initialAccountIds if provided
      if (widget.initialAccountIds != null &&
          widget.initialAccountIds!.isNotEmpty) {
        selections = List<dynamic>.from(widget.initialAccountIds!);
      }

      selectedMonth = SettingUtil.currentMonth;
      AppLog.d('[TransactionListPage] selectedMonth=$selectedMonth');

      await refresh();
      this.loadCompleteView();
      AppLog.d('[TransactionListPage] asyncLoad complete');
    } catch (e) {
      AppLog.d('[TransactionListPage] asyncLoad error: $e');
      if (mounted) {
        // If not logged in or session invalid, go to welcome/login flow.
        Navigator.pushReplacementNamed(context, '/welcome');
      }
    }
  }

  @override
  Future<void> refresh() async {
    AppLog.d('[TransactionListPage] refresh start month=$selectedMonth');
    try {
      await initTransactionData();
      if (this.mounted) {
        setState(() {
          // Ensure any programmatic loader is cleared after successful data load
          if (_isProgrammaticLoading) {
            AppLog.d(
                '[TransactionListPage] clearing programmatic loader (success)');
          }
          _isProgrammaticLoading = false;
          refreshController.refreshCompleted();
        });
      } else {
        // If not mounted, still clear the flag to avoid stale state
        _isProgrammaticLoading = false;
      }
      AppLog.d(
          '[TransactionListPage] refresh complete txCount=${transactions.length}');
    } catch (e) {
      AppLog.d('[TransactionListPage] refresh error: $e');
      // Likely unauthenticated; redirect to welcome/login.
      if (mounted) {
        setState(() {
          if (_isProgrammaticLoading) {
            AppLog.d(
                '[TransactionListPage] clearing programmatic loader (error)');
          }
          _isProgrammaticLoading = false;
        });
        Navigator.pushReplacementNamed(context, '/welcome');
      } else {
        _isProgrammaticLoading = false;
      }
    }
  }

  initTransactionData() async {
    AppLog.d(
        '[TransactionListPage] initTransactionData start selections=$selections');
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
        '[TransactionListPage] parsed accountIds=$accountIds month=$selectedMonth');
    // Load all transactions for the selected month (no pagination)
    transactions = await TransactionController.getTransactionsForSelectedMonth(
        accountIds: accountIds, month: selectedMonth);
    AppLog.d(
        '[TransactionListPage] fetched transactions count=${transactions.length}');

    totalAmount = incomeAmount = expenseAmount = 0;

    final currentMonthDate =
        DateTime.utc(selectedMonth.year, selectedMonth.month);
    final nextMonthDate =
        DateTime.utc(selectedMonth.year, selectedMonth.month + 1);

    final summary = await TransactionController.getSummaryBetween(
      currentMonthDate,
      nextMonthDate,
      accountIds: accountIds,
    );

    incomeAmount = (summary['totalIncome'] as num?)?.toDouble() ?? 0.0;
    expenseAmount = (summary['totalExpense'] as num?)?.toDouble() ?? 0.0;
    previousMonthAmount = (summary['rolloverNet'] as num?)?.toDouble() ?? 0.0;
    totalAmount = (summary['netTotalWithRollover'] as num?)?.toDouble() ??
        (incomeAmount - expenseAmount + previousMonthAmount);

    AppLog.d(
        '[TransactionListPage] totals previous=$previousMonthAmount balance=$totalAmount');
  }

  Widget _buildContent() {
    return SmartRefresher(
      controller: refreshController,
      enablePullDown: true,
      enablePullUp: false,
      onRefresh: () async {
        await refresh();
      },
      child: CustomScrollView(
        slivers: <Widget>[
          SliverToBoxAdapter(
            child: Padding(
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
                      onPressed:
                          _isProgrammaticLoading ? null : _goToPreviousMonth,
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
                      onPressed: _isProgrammaticLoading ? null : _goToNextMonth,
                    ),
                  ),
                ),
              ),
            ),
          ),
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.only(top: 4.0),
              child: Card(
                elevation: 2,
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16.0),
                    side: BorderSide(
                        color:
                            Theme.of(context).dividerColor.withOpacity(0.1))),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
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
                        color:
                            Theme.of(context).dividerColor.withOpacity(0.075),
                      ),
                    ),
                    ListTile(
                      trailing: Text(
                        CommonUtil.toCurrency(totalAmount),
                        style: TextStyle(
                            fontSize: 18.0, fontWeight: FontWeight.w500),
                      ),
                      dense: true,
                      title: Text(
                        "Balance",
                        style: TextStyle(fontSize: 18.0),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
          if (_isProgrammaticLoading)
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.all(24.0),
                child: SizedBox(
                  height: 120,
                  child: Center(child: CircularProgressIndicator()),
                ),
              ),
            )
          else if (transactions.isEmpty)
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.all(32.0),
                child: Center(
                  child: Text(
                    "No transactions found for this month.",
                    style: TextStyle(color: Colors.grey, fontSize: 16),
                  ),
                ),
              ),
            )
          else
            SliverList(
              key: ValueKey(selectedMonth.toIso8601String()),
              delegate: SliverChildBuilderDelegate(
                (context, index) {
                  final tx = transactions[index];
                  final currentHuman =
                      CommonUtil.humanDate(tx.date).toUpperCase();
                  String? prevHuman;
                  if (index > 0) {
                    prevHuman =
                        CommonUtil.humanDate(transactions[index - 1].date)
                            .toUpperCase();
                  }

                  final List<Widget> children = [];
                  if (index == 0 || prevHuman != currentHuman) {
                    children.add(
                      PaddedText(
                        currentHuman,
                        horizontal: 10.0,
                        vertical: 10.0,
                      ),
                    );
                  }
                  children.add(
                    TransactionTile(this, tx, (parent, Transaction t) async {
                      await refresh();
                    }),
                  );

                  if (children.length == 1) {
                    return children.first;
                  }
                  return Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    mainAxisSize: MainAxisSize.min,
                    children: children,
                  );
                },
                childCount: transactions.length,
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
