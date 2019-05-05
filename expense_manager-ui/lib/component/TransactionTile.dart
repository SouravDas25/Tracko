import 'package:expense_manager/Utils/CommonUtil.dart';
import 'package:expense_manager/Utils/WidgetUtil.dart';
import 'package:expense_manager/component/interfaces.dart';
import 'package:expense_manager/models/transaction.dart';
import 'package:expense_manager/pages/add_item_page/add_item.dart';
import 'package:expense_manager/pages/smart_add_item/smart_add_item.dart';
import 'package:flutter/material.dart';
import 'package:flutter_slidable/flutter_slidable.dart';

class TransactionTile extends StatelessWidget {
  Transaction transaction;
  RefreshableState parent;

  TransactionTile(this.parent, this.transaction);

  void _showDialog() {
    // flutter defined function
    showDialog(
      context: parent.context,
      builder: (BuildContext context) {
        // return object of type Dialog
        return AlertDialog(
          title: new Text("Delete Transaction"),
          content: new Text("Are sure you want to delete this transaction ?"),
          actions: <Widget>[
            new FlatButton(
              child: new Text("No"),
              onPressed: () {
                Navigator.pop(context);
              },
            ),
            new FlatButton(
              child: new Text("Yes"),
              onPressed: () {
                TransactionBean.delete(transaction);
                Navigator.pop(context);
                parent.refresh();
              },
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Slidable(
      delegate: new SlidableScrollDelegate(),
      child: Card(
          shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(4.0),
              side: new BorderSide(color: Colors.grey, width: 0.25)),
          child: ListTile(
            onTap: () {
              Navigator.of(context).push(MaterialPageRoute(
                  builder: (context) => AddItemPage(transaction: transaction)));
            },
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
            trailing: WidgetUtil.transformAmount2TextWidget(transaction),
          )),
      secondaryActions: <Widget>[
//        Card(
//          margin: EdgeInsets.symmetric(vertical: 10.0, horizontal: 5),
//          child: new IconSlideAction(
//            caption: 'Edit',
//            color: Colors.blue,
//            icon: Icons.edit,
//            onTap: () {
////              print(transaction);
//              Navigator.of(context).push(MaterialPageRoute(
//                  builder: (context) => AddItemPage(transaction: transaction)));
//            },
//          ),
//        ),
        Card(
          margin: EdgeInsets.symmetric(vertical: 10.0, horizontal: 5),
          child: new IconSlideAction(
            onTap: () {
              _showDialog();
            },
            caption: 'Delete',
            color: Colors.red,
            icon: Icons.delete,
          ),
        ),
      ],
    );
  }
}
