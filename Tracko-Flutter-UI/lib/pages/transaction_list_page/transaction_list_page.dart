import 'package:Tracko/Utils/CommonUtil.dart';
import 'package:Tracko/Utils/ConstantUtil.dart';
import 'package:Tracko/Utils/SettingUtil.dart';
import 'package:Tracko/component/PageWidget.dart';
import 'package:Tracko/component/TimedList.dart';
import 'package:Tracko/component/TransactionTile.dart';
import 'package:Tracko/component/interfaces.dart';
import 'package:Tracko/component/multi_select/multi_select.dart';
import 'package:Tracko/controllers/AccountController.dart';
import 'package:Tracko/controllers/TransactionController.dart';
import 'package:Tracko/models/account.dart';
import 'package:Tracko/models/transaction.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart' as DateFormatter;
import 'package:pull_to_refresh/pull_to_refresh.dart';

class TransactionListPage extends StatefulWidget {
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

  _AccountsPage();

  @override
  asyncLoad() async {
    await refresh();
    this.loadCompleteView();
  }

  @override
  Future<void> refresh() async {
    accounts = await AccountController.getAllAccounts();
//    print(accounts);
//    await adapter.close();
    await initTransactionData();
//    Future<void>.delayed(Duration(milliseconds: 5));
    if (this.mounted) {
      setState(() {
//      refreshController.sendBack(true, RefreshStatus.completed);
        refreshController.refreshCompleted();
      });
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
    pageCount = await TransactionController.totalTransactionCount(
        accountIds: accountIds);

    pageCount = (pageCount / ConstantUtil.NO_OF_RECORDS_PER_PAGE).ceil();
//    print(accountIds);
    transactions = await TransactionController.getTransaction(currentPage,
        accountIds: accountIds);
    totalAmount = incomeAmount = expenseAmount = 0;
    incomeAmount = await TransactionController.getCurrentMonthIncome(
        accountIds: accountIds);
    expenseAmount = await TransactionController.getCurrentMonthExpense(
        accountIds: accountIds);
    previousMonthAmount = await TransactionController.getPreviousMonthTotal();
    totalAmount = incomeAmount - expenseAmount + previousMonthAmount;
  }

  @override
  Widget completeWidget(BuildContext context) {
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
            padding: const EdgeInsets.all(4.0),
            child: MultiSelect(
              autovalidateMode: AutovalidateMode.disabled,
              titleText: 'Select multiple accounts',
              textField: 'name',
              valueField: 'id',
              required: false,
              filterable: true,
              value: null,
              change: (values) {
                selections = values;
              },
              open: () {},
              close: () {},
              onSaved: (values) async {
                selections = values;
//                print("selected $values ");
                await initTransactionData();
                setState(() {});
              },
              dataSource: accounts.map((Account account) {
                return {
                  "name": account.name,
                  "id": account.id,
                };
              }).toList(),
            ),
          ),
          Padding(
            padding: const EdgeInsets.only(top: 4.0),
            child: Card(
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(4.0),
                  side: new BorderSide(color: Colors.grey, width: 0.5)),
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
                      "Last Month (${DateFormatter.DateFormat("MMM").format(
                          SettingUtil.previousMonth)})",
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
  Widget fallbackWidget(BuildContext context) {
    return Center(
      child: Text("No Data Available."),
    );
  }
}
