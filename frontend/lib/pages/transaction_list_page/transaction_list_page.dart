import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/ConstantUtil.dart';
import 'package:tracko/Utils/SettingUtil.dart';
import 'package:tracko/component/PageWidget.dart';
import 'package:tracko/component/TimedList.dart';
import 'package:tracko/component/TransactionTile.dart';
import 'package:tracko/component/interfaces.dart';
import 'package:tracko/component/multi_select/multi_select.dart';
import 'package:tracko/controllers/AccountController.dart';
import 'package:tracko/controllers/TransactionController.dart';
import 'package:tracko/models/account.dart';
import 'package:tracko/models/transaction.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart' as DateFormatter;
import 'package:pull_to_refresh/pull_to_refresh.dart';

class TransactionListPage extends StatefulWidget {
  final List<int>? initialAccountIds;
  final bool showAccountFilter;
  final bool embedded;

  const TransactionListPage({
    Key? key,
    this.initialAccountIds,
    this.showAccountFilter = true,
    this.embedded = false,
  }) : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _AccountsPage();
  }
}

class _AccountsPage extends RefreshableState<TransactionListPage> {
  RefreshController refreshController = new RefreshController();
  List<Account> accounts = [];
  List<Transaction> transactions = [];
  List<dynamic> selections = [];
  double previousMonthAmount = 0.0;
  double totalAmount = 0.0;
  double incomeAmount = 0.0;
  double expenseAmount = 0.0;
  int currentPage = 1;
  int pageCount = 0;
  DateTime selectedMonth =
      SettingUtil.currentMonth; // For future month navigation

  _AccountsPage();

  @override
  asyncLoad() async {
    try {
      // Pre-seed selections from initialAccountIds if provided
      if (widget.initialAccountIds != null &&
          widget.initialAccountIds!.isNotEmpty) {
        selections = List<dynamic>.from(widget.initialAccountIds!);
      }
      await refresh();
      this.loadCompleteView();
    } catch (e) {
      if (mounted) {
        // If not logged in or session invalid, go to welcome/login flow.
        Navigator.pushReplacementNamed(context, '/welcome');
      }
    }
  }

  @override
  Future<void> refresh() async {
    try {
      accounts = await AccountController.getAllAccounts();
      await initTransactionData();
      if (this.mounted) {
        setState(() {
          refreshController.refreshCompleted();
        });
      }
    } catch (e) {
      // Likely unauthenticated; redirect to welcome/login.
      if (mounted) {
        Navigator.pushReplacementNamed(context, '/welcome');
      }
      rethrow;
    }
  }

  initTransactionData() async {
    transactions.clear();
    List<int> accountIds = [];
    if (selections != null && selections.length > 0) {
      int accountId;
      for (int i = 0; i < selections.length; i++) {
        accountId = int.parse(selections[i].toString());
        accountIds.add(accountId);
      }
    }
    // Load all transactions for the selected month (no pagination)
    transactions = await TransactionController.getTransactionsForSelectedMonth(
        accountIds: accountIds);

    // Keep pageCount for PageWidget display (will remove later)
    pageCount = 1;
    totalAmount = incomeAmount = expenseAmount = 0;
    incomeAmount = await TransactionController.getCurrentMonthIncome(
        accountIds: accountIds);
    expenseAmount = await TransactionController.getCurrentMonthExpense(
        accountIds: accountIds);

    final prevSummary = await TransactionController.getSummaryBetween(
      SettingUtil.previousMonth,
      SettingUtil.currentMonth,
      accountIds: accountIds,
    );
    previousMonthAmount = (prevSummary['netTotal'] as num?)?.toDouble() ?? 0.0;

    totalAmount = incomeAmount - expenseAmount + previousMonthAmount;
  }

  Widget _buildContent() {
    return SmartRefresher(
      controller: refreshController,
      enablePullDown: true,
      enablePullUp: false,
      onRefresh: () {
        refresh();
      },
      child: ListView(
        children: <Widget>[
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
                      "Last Month (${DateFormatter.DateFormat("MMM").format(SettingUtil.previousMonth)})",
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
                    (dynamic parent, Transaction transaction) {
                  parent.refresh();
                });
              },
            ),
          ),
          PageWidget(
            initialPage: currentPage,
            totalPage: pageCount,
            title: DateFormatter.DateFormat("MMM yyyy").format(selectedMonth),
            disableBack: false,
            disableNext: false,
            onBack: () {
              setState(() {
                selectedMonth =
                    DateTime.utc(selectedMonth.year, selectedMonth.month - 1);
              });
              SettingUtil.setSelectedMonth(selectedMonth);
              refresh();
            },
            onNext: () {
              setState(() {
                selectedMonth =
                    DateTime.utc(selectedMonth.year, selectedMonth.month + 1);
              });
              SettingUtil.setSelectedMonth(selectedMonth);
              refresh();
            },
            onChange: (BuildContext context, int pageNo) {
              currentPage = pageNo;
              this.refresh();
            },
          )
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
          refresh();
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
