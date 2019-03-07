import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/models/account.dart';
import 'package:flutter/material.dart';
import 'package:pull_to_refresh/pull_to_refresh.dart';
import 'package:sqflite/sqflite.dart';

class AccountsPage extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return _AccountsPage();
  }
}

class _AccountsPage extends State<AccountsPage> {
  RefreshController refreshController = new RefreshController();
  List<Account> accounts = new List(0);

  _AccountsPage() {
    initData();
  }

  void initData() async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    AccountBean accountBean = new AccountBean(adapter);
    accounts = await accountBean.getAll();
    print(accounts);
    await adapter.close();
    setState(() {
      refreshController.sendBack(true, RefreshStatus.completed);
    });
  }

  @override
  Widget build(BuildContext context) {
    return SmartRefresher(
      controller: refreshController,
      enablePullDown: true,
      enablePullUp: false,
      onRefresh: (bool up) {
        if (up) {
          initData();
        }
      },
      child: ListView(
        children: <Widget>[
          Padding(
            padding: EdgeInsets.symmetric(vertical: 20.0),
            child: ListView(
              primary: false,
              shrinkWrap: true,
              children: accounts.map((Account account) {
                return Card(
                    child: ListTile(
                  trailing: Text(
                    "₹ 240.00",
                    style: TextStyle(fontWeight: FontWeight.w600, fontSize: 20),
                  ),
                  title: Text(
                    account.name,
                    style: TextStyle(fontWeight: FontWeight.w500, fontSize: 20),
                  ),
                  subtitle: Text(""),
                ));
              }).toList(),
            ),
          ),
        ],
      ),
    );
  }
}
