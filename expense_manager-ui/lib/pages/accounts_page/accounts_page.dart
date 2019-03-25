import 'package:expense_manager/Utils/CommonUtil.dart';
import 'package:expense_manager/Utils/Database.dart';
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
  List<Account> accounts = new List(0);
  List<Transaction> transactions = new List(0);
  List<dynamic> selections = new List(0);
  double totalAmount = 0.0;

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
    Sqflite.Database db = await DatabaseUtil.getRawDatabase();

    String query = "SELECT * FROM transactions t";

    if (selections != null && selections.length > 0) {
      String param = "";
      for (int i = 0; i < selections.length; i++) {
        param += selections[i].toString();
        if (i + 1 != selections.length) {
          param += " ,";
        }
      }
      query += "WHERE t.account_id IN (" + param + ")";
    }
    query += " ORDER BY t.date DESC";
    var adapter = await DatabaseUtil.getAdapter();
    var tb = new TransactionBean(adapter);
//    print(query);
    transactions = (await db.rawQuery(query)).map((dynamic map) {
      return tb.fromMap(map);
    }).toList();
//    print(transactions);
//    await db.close();
    print(query);
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
                  shrinkWrap: true,
                  children: <Widget>[
                    ListTile(
                      trailing: Text(
                        CommonUtil.toCurrency(totalAmount),
                        style: TextStyle(fontSize: 18.0,fontWeight: FontWeight.bold),
                      ),
                      title: Text(
                        "Income",
                        style: TextStyle(fontSize: 18.0),
                      ),
                    ),
                    ListTile(
                      trailing: Text(
                        CommonUtil.toCurrency(totalAmount),
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
                        "Total Amount",
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
