import 'package:expense_manager/Utils/CommonUtil.dart';
import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/component/HomePieChart.dart';
import 'package:expense_manager/component/PaddedText.dart';
import 'package:expense_manager/component/TransactionTile.dart';
import 'package:expense_manager/models/TransactionFacade.dart';
import 'package:expense_manager/models/transaction.dart';
import 'package:flutter/material.dart';
import 'package:jaguar_query_sqflite/jaguar_query_sqflite.dart';
import "package:pull_to_refresh/pull_to_refresh.dart";
import 'package:sqflite/sqflite.dart' as Sqflite;

class HomePage extends StatefulWidget {
  HomePage({Key key}) : super(key: key);

  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage>
    with SingleTickerProviderStateMixin {
  List<Transaction> transactions = new List(0);
  bool refreshIndicator = true;
  RefreshController refreshController = new RefreshController();
  double totalAmount = 0.0;

  @override
  initState() {
    super.initState();
    initData();
  }

  initData() async {
//    refreshController.sendBack(true, RefreshStatus.refreshing);

    var adapter = await DatabaseUtil.getAdapter();
    TransactionBean transactionBean = new TransactionBean(adapter);
    Find query = transactionBean.finder.limit(5).orderBy(transactionBean.date.name);
    transactions = await transactionBean.findMany(query);
    print(transactions);
    totalAmount = await TransactionFacade.getCurrentAmount();
//    await db.close();
    setState(() {
      refreshController.sendBack(refreshIndicator, RefreshStatus.completed);
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
          Card(
            child: Column(
              children: <Widget>[
                ListTile(
                  leading: PaddedText("Total Balance",
                      textAlign: TextAlign.left),
                ),
                PaddedText(
                  CommonUtil.toCurrency(totalAmount),
                  vertical: 15.0,
                  horizontal: 10.0,
                  style: TextStyle(fontSize: 35),
                  textAlign: TextAlign.center,
                ),
              ],
            ),
          ),
          Card(
            child: CategoryChart(),
          ),
          PaddedText(
            "RECENT TRANSACTION",
            horizontal: 10.0,
            vertical: 10.0,
          ),
          ListView(
            primary: false,
            shrinkWrap: true,
            children: transactions.map((Transaction transaction) {
              return TransactionTile(transaction);
            }).toList(),
          ),
        ],
      ),
    );
  }
}
