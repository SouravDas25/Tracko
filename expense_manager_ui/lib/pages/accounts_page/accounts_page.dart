import 'package:expense_manager/Utils/CommonUtil.dart';
import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/Utils/enums.dart';
import 'package:expense_manager/component/TransactionTile.dart';
import 'package:expense_manager/component/multi_select/multi_select.dart';
import 'package:expense_manager/models/account.dart';
import 'package:expense_manager/models/transaction.dart';
import 'package:flutter/material.dart';
import 'package:pull_to_refresh/pull_to_refresh.dart';
import 'package:sqflite/sqflite.dart' as Sqflite;

class AccountsPage extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return _AccountsPage();
  }
}

class _AccountsPage extends State<AccountsPage> {
  RefreshController refreshController = new RefreshController();
  List<Account> accounts = new List();
  List<Transaction> transactions = new List();
  List<dynamic> selections = new List();
  double totalAmount = 0.0;
  double incomeAmount = 0.0;
  double expenseAmount = 0.0;

  _AccountsPage();

  @override
  void initState() {
    super.initState();
    initAccountData();
  }

  void initAccountData() async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    AccountBean accountBean = new AccountBean(adapter);
    accounts = await accountBean.getAll();
    print(accounts);
//    await adapter.close();
    await initTransactionData();
    Future<void>.delayed(Duration(milliseconds: 5));
    setState(() {
      refreshController.sendBack(true, RefreshStatus.completed);
    });
  }

  initTransactionData() async {
    var adapter = await DatabaseUtil.getAdapter();
    TransactionBean transactionBean = new TransactionBean(adapter);
    transactions.clear();
    if (selections != null && selections.length > 0) {
      int accountId;
      for (int i = 0; i < selections.length; i++) {
        accountId = int.parse(selections[i].toString());
        List<Transaction> ts = await transactionBean.findByAccount(accountId);
        transactions.addAll(ts);
      }
    }
    else {
      transactions = await transactionBean.getAll();
    }
    transactions.sort((a,b)=> b.date.compareTo(a.date));
    totalAmount = incomeAmount = expenseAmount = 0;
    transactions.forEach((Transaction element) {
      if(element.transactionType == TransactionType.CREDIT)
        incomeAmount += element.amount;
      else
        expenseAmount += element.amount;
    });
    totalAmount = incomeAmount - expenseAmount;
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: SmartRefresher(
        controller: refreshController,
        enablePullDown: true,
        enablePullUp: false,
        onRefresh: (bool up) {
          if (up) {
            initAccountData();
          }
        },
        child: ListView(
          children: <Widget>[
            MultiSelect(
              autovalidate: false,
              titleText: 'Select multiple accounts',
              textField: 'name',
              valueField: 'id',
              required: false,
              filterable: true,
              value: null,
              onSaved: (values) {
                selections = values;
//                print("selected $values ");
                initTransactionData();
              },
              dataSource: accounts.map((Account account) {
                return {
                  "name": account.name,
                  "id": account.id,
                };
              }).toList(),
            ),
            Padding(
              padding: const EdgeInsets.only(top: 8.0),
              child: Card(
                child: ListView(
                  primary: false,
                  shrinkWrap: true,
                  children: <Widget>[
                    ListTile(
                      trailing: Text(
                        CommonUtil.toCurrency(incomeAmount),
                        style: TextStyle(fontSize: 18.0,fontWeight: FontWeight.bold),
                      ),
                      title: Text(
                        "Income",
                        style: TextStyle(fontSize: 18.0),
                      ),
                    ),
                    ListTile(
                      trailing: Text(
                        CommonUtil.toCurrency(expenseAmount),
                        style: TextStyle(fontSize: 18.0,fontWeight: FontWeight.bold),
                      ),
                      title: Text(
                        "Expense",
                        style: TextStyle(fontSize: 18.0),
                      ),
                    ),
                    ListTile(
                      trailing: Text(
                        CommonUtil.toCurrency(totalAmount),
                        style: TextStyle(fontSize: 18.0,fontWeight: FontWeight.bold),
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
              padding: EdgeInsets.symmetric(vertical: 10.0),
              child: ListView(
                primary: false,
                shrinkWrap: true,
                children: transactions.map((Transaction transaction) {
                  return TransactionTile(transaction);
                }).toList(),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
