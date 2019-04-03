import 'package:expense_manager/Utils/CommonUtil.dart';
import 'package:expense_manager/Utils/enums.dart';
import 'package:expense_manager/models/transaction.dart';
import 'package:expense_manager/pages/smart_add_item/smart_add_item.dart';
import 'package:flutter/material.dart';
import 'package:flutter_slidable/flutter_slidable.dart';

class SmartListView extends StatelessWidget {
  final List<Transaction> transactions;
  final ScanningStatus scanning;
  final Function dismissAll;
  final Function dismissSingle;
  final Function saveAll;

  SmartListView(this.transactions, this.scanning,
      {this.dismissAll, this.dismissSingle, this.saveAll});

  List<Widget> generate(BuildContext context) {
    List<Widget> widgets = List<Widget>();
    int i = 0;
    for (i = 0; i < transactions.length; i++) {
      int index = i;
      Transaction transaction = transactions[i];
      widgets.add(IgnorePointer(
        ignoring: this.scanning == ScanningStatus.RUNNING ? true : false,
        child: Slidable(
          child: Card(
            child: ListTile(
              onTap: () {
                Navigator.of(context).push(MaterialPageRoute(
                    builder: (context) => SmartAddItemPage(transaction)));
              },
//              contentPadding: EdgeInsets.all(10.0),
              leading: CircleAvatar(
                backgroundColor: Colors.transparent,
                backgroundImage: NetworkImage(transaction.logo),
              ),
              title: Text(
                transaction.name,
                style: TextStyle(fontSize: 20.0),
              ),
              subtitle: Text(
                CommonUtil.humanDate(transaction.date),
              ),
              trailing: Text(
                CommonUtil.toCurrency(transaction.amount),
                style: TextStyle(fontWeight: FontWeight.w600, fontSize: 20),
              ),
            ),
          ),
          delegate: new SlidableScrollDelegate(),
          secondaryActions: <Widget>[
            Card(
              margin: EdgeInsets.symmetric(vertical: 10.0, horizontal: 5),
              child: new IconSlideAction(
                color: Colors.red,
                caption: "Delete",
                icon: Icons.delete,
                onTap: () {
                  this.dismissSingle(index);
                },
              ),
            ),
          ],
        ),
      ));
    }
    return widgets;
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      children: this.generate(context)
        ..add(Padding(
          padding: const EdgeInsets.all(20.0),
          child: Row(
            children: <Widget>[
              Expanded(
                child: IgnorePointer(
                  ignoring:
                      this.scanning == ScanningStatus.RUNNING ? true : false,
                  child: RaisedButton(
                    padding: EdgeInsets.all(20.0),
                    color: Colors.lightGreen,
                    textColor: Colors.white,
                    onPressed: () {
                      if (this.saveAll != null) this.saveAll();
                    },
                    child: Text(
                      "Save All",
                      style: TextStyle(fontSize: 20.0),
                    ),
                  ),
                ),
              ),
              Container(
                child: IgnorePointer(
                  ignoring:
                      this.scanning == ScanningStatus.RUNNING ? true : false,
                  child: RaisedButton(
                    padding: EdgeInsets.all(20.0),
                    color: Colors.red,
                    onPressed: () {
                      dismissAll();
                    },
                    child: Icon(
                      Icons.delete,
                      color: Colors.white,
                    ),
                  ),
                ),
              )
            ],
          ),
        )),
    );
  }
}
