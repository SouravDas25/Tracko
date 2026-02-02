import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/controllers/TransactionController.dart';
import 'package:tracko/models/transaction.dart';
import 'package:tracko/pages/add_item_page/add_item.dart';
import 'package:flutter/material.dart';
import 'package:flutter_slidable/flutter_slidable.dart';

typedef DeleteCallback = void Function(dynamic, Transaction);

class TransactionTile extends StatelessWidget {
  final Transaction transaction;
  final State parent;
  final DeleteCallback deleteCallback;

  TransactionTile(this.parent, this.transaction, this.deleteCallback);

  void deleteTransactionAndCallback() async {
    await TransactionController.deleteById(transaction.id ?? 0);
    Navigator.pop(parent.context);
    WidgetUtil.toast("Transaction removed");
    if (this.deleteCallback != null) {
      try {
        this.deleteCallback(parent, transaction);
      } catch (e) {
        print(e.toString());
      }
    }
  }

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
            new TextButton(
              child: new Text("No"),
              onPressed: () {
                Navigator.pop(context);
              },
            ),
            new TextButton(
              child: new Text("Yes"),
              onPressed: deleteTransactionAndCallback,
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Slidable(
      endActionPane: ActionPane(
        motion: ScrollMotion(),
        children: [
          SlidableAction(
            onPressed: (context) {
              _showDialog();
            },
            backgroundColor: Colors.red,
            foregroundColor: Colors.white,
            icon: Icons.delete,
            label: 'Delete',
          ),
        ],
      ),
      child: Card(
          shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(4.0),
              side: new BorderSide(color: Colors.grey, width: 0.25)),
          child: ListTile(
            onTap: () {
              Navigator.of(context).push(MaterialPageRoute(
                  builder: (context) => AddItemPage(transaction: transaction)));
            },
            leading: WidgetUtil.textAvatar(transaction.name),
            title: Text(
              transaction.name,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(fontSize: 20.0),
            ),
            subtitle: Text(transaction.contacts.length > 1
                ? "${transaction.category?.name ?? ''} / ${transaction.contacts?.length}"
                : "${transaction.category?.name ?? ''}"),
            trailing: WidgetUtil.transformTransaction2TextWidget(transaction),
          )),
    );
  }
}
