import 'package:tracko/component/TransactionTile.dart';
import 'package:tracko/models/transaction.dart';
import 'package:flutter/material.dart';

class SmartListView extends StatelessWidget {
  final List<Transaction> transactions;
  final Function deleteSingle;
  final Function complete;
  final Function cancel;

  State<StatefulWidget> parent;

  SmartListView(this.transactions, this.parent,
      {required this.deleteSingle,
      required this.complete,
      required this.cancel});

  List<Widget> generate(BuildContext context) {
    List<Widget> widgets = <Widget>[];
    int i = 0;
    for (i = 0; i < transactions.length; i++) {
      Transaction transaction = transactions[i];
      widgets.add(IgnorePointer(
        ignoring: false,
        child: TransactionTile(this.parent, transaction,
            (parent, Transaction transaction) {
          parent.setState(() {});
        }),
      ));
    }
    return widgets;
  }

  ElevatedButton get actionButton {
    return ElevatedButton(
      style: ElevatedButton.styleFrom(
        padding: EdgeInsets.all(20.0),
        backgroundColor: Colors.blueAccent,
        foregroundColor: Colors.white,
      ),
      onPressed: () {
        this.complete();
      },
      child: Text(
        "COMPLETE",
        style: TextStyle(fontSize: 20.0),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      children: this.generate(context)
        ..add(Padding(
          padding: const EdgeInsets.all(20.0),
          child: this.actionButton,
        )),
    );
  }
}
