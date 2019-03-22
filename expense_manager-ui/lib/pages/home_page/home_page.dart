import 'package:expense_manager/Utils/CommonUtil.dart';
import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/component/HomePieChart.dart';
import 'package:expense_manager/component/PaddedText.dart';
import 'package:expense_manager/component/TransactionTile.dart';
import 'package:flutter/material.dart';
import "package:pull_to_refresh/pull_to_refresh.dart";
import 'package:sqflite/sqflite.dart';

class HomePage extends StatefulWidget {
  HomePage({Key key}) : super(key: key);

  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage>
    with SingleTickerProviderStateMixin {
  List<dynamic> transactions = new List(0);
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
    Database db = await DatabaseUtil.getRawDatabase();
    String query = "SELECT t.*, c.name AS category_name from transactions t"
        " JOIN categories c ON t.category_id = c.id"
        " LIMIT 5";
    transactions = (await db.rawQuery(query)).toList();
    var tmp =
        (await db.rawQuery("SELECT SUM(amount) AS amount from transactions"))
            .toList();
//    print(tmp);
    totalAmount = tmp[0]['amount'];
    await db.close();
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
            child: CategoryPieChart(),
          ),
          PaddedText(
            "RECENT TRANSACTION",
            horizontal: 10.0,
            vertical: 10.0,
          ),
          ListView(
            primary: false,
            shrinkWrap: true,
            children: transactions.map((dynamic transaction) {
              return TransactionTile(transaction);
            }).toList(),
          ),
        ],
      ),
    );
  }
}
