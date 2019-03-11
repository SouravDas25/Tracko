import 'package:expense_manager/Utils/CommonUtil.dart';
import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/component/MenuDrawer.dart';
import 'package:expense_manager/component/PaddedText.dart';
import 'package:expense_manager/component/TransactionTile.dart';
import 'package:expense_manager/component/screen.dart';
import 'package:expense_manager/models/transaction.dart';
import 'package:expense_manager/pages/add_item_page/add_item.dart';
import 'package:flutter/material.dart';
import 'package:expense_manager/component/menu_bar.dart';
import "package:pull_to_refresh/pull_to_refresh.dart";
import 'package:sqflite/sqflite.dart';
import 'package:flutter_slidable/flutter_slidable.dart';

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
    print(transactions);
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
          Padding(
            padding: EdgeInsets.symmetric(vertical: 20, horizontal: 20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: <Widget>[
                Card(
                  child: Column(
                    children: <Widget>[
                      ListTile(
                        leading: PaddedText("Total Balance",
                            textAlign: TextAlign.left),
                      ),
                      PaddedText(
                        '₹ 24,560',
                        vertical: 15.0,
                        horizontal: 10.0,
                        style: TextStyle(fontSize: 35),
                        textAlign: TextAlign.center,
                      ),
                    ],
                  ),
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
                )
              ],
            ),
          )
        ],
      ),
    );
  }
}
