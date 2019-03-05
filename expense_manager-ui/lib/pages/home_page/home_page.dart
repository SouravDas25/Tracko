import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/component/MenuDrawer.dart';
import 'package:expense_manager/component/PaddedText.dart';
import 'package:expense_manager/component/screen.dart';
import 'package:expense_manager/models/transaction.dart';
import 'package:flutter/material.dart';
import 'package:expense_manager/component/menu_bar.dart';

class HomePage extends StatefulWidget {
  HomePage({Key key}) : super(key: key);

  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  List<Transaction> transactions = new List(0);

  _HomePageState() {
    initData();
  }

  initData() async {
    var adapter = await Database.getAdapter();
    await adapter.connect();
    TransactionBean transactionBean = TransactionBean(adapter);
    transactions = await transactionBean.getAll();
    print(transactions);
    await TransactionBean.preLoadMappings(transactions,adapter);
    print(transactions);
    await adapter.close();
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text("Expense Manager"),
        centerTitle: true,
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          Navigator.pushNamed(context, "/add_item");
        },
        child: Icon(Icons.add),
      ),
      body: SingleChildScrollView(
        child: Padding(
          padding: EdgeInsets.symmetric(vertical: 20, horizontal: 20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: <Widget>[
              Card(
                child: Column(
                  children: <Widget>[
                    PaddedText("Total Balance",
                        horizontal: 0.0,
                        vertical: 12.0,
                        textAlign: TextAlign.left),
                    PaddedText(
                      '₹ 24,560',
                      vertical: 20.0,
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
                children: transactions.map((Transaction transaction) {
                  return ListTile(
                    trailing: Text("₹ " + transaction.amount.toString()),
                    title: Text(transaction.category.name),
                    subtitle: Text(transaction.date.toIso8601String()));
                }).toList(),
              )
            ],
          ),
        ),
      ),
      drawer: MenuDrawer(),
    );
  }
}
