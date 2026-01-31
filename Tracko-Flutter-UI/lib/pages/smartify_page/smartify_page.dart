import 'package:tracko/component/FLushDialog.dart';
import 'package:tracko/controllers/TransactionController.dart';
import 'package:tracko/models/transaction.dart';
import 'package:flutter/material.dart';

/*
* Text Razor APi KEY - 6a72f94bb1182f454256c0591e99e75dcc630bdb8054f2fa05edd15b
* */

class SmartPage extends StatefulWidget {
  List<Transaction> get foundTransactions => <Transaction>[];

  SmartPage();

  @override
  State<StatefulWidget> createState() {
    return SmartifyState();
  }
}

class SmartifyState extends State<SmartPage> {
  static SmartifyState? that;

  static Function onUpdate = (onValue) {
    if (SmartifyState.that?.mounted ?? false)
      SmartifyState.that?.setState(() {});
  };

  SmartifyState() {
    SmartifyState.that = this;
  }

  void cancel() {
    setState(() {});
  }

  void initData() async {
    FlushDialog.flash(
        context, "Unsupported", "SMS scanning has been disabled.");
  }

  void deleteSingle(int index) {
    Transaction transaction = widget.foundTransactions[index];
    TransactionController.deleteById(transaction.id ?? 0);
    widget.foundTransactions.removeAt(index);
    setState(() {});
  }

  void complete() async {
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      children: <Widget>[
        Padding(
          padding: const EdgeInsets.all(20.0),
          child: Image.asset(
            "assets/images/scaning.png",
            scale: 2.0,
          ),
        ),
        Container(
          padding: EdgeInsets.symmetric(horizontal: 40, vertical: 80),
          child: ElevatedButton(
            style: ElevatedButton.styleFrom(
              padding: EdgeInsets.all(20.0),
              backgroundColor: Theme.of(context).primaryColor,
              foregroundColor: Colors.white,
            ),
            onPressed: () {
              initData();
            },
            child: Text(
              "SCAN (DISABLED)",
              style: TextStyle(fontSize: 22.0),
            ),
          ),
        )
      ],
    );
  }
}
