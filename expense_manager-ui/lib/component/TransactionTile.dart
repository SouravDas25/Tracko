import 'package:expense_manager/Utils/CommonUtil.dart';
import 'package:expense_manager/models/transaction.dart';
import 'package:expense_manager/pages/add_item_page/add_item.dart';
import 'package:expense_manager/pages/smart_add_item/smart_add_item.dart';
import 'package:flutter/material.dart';
import 'package:flutter_slidable/flutter_slidable.dart';

class TransactionTile extends StatelessWidget {
  Transaction transaction;

  TransactionTile(this.transaction);

  @override
  Widget build(BuildContext context) {
    return Slidable(
      delegate: new SlidableScrollDelegate(),
      child: Card(
          child: ListTile(
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
                  CommonUtil.toSign(transaction.transactionType) + CommonUtil.toCurrency(transaction.amount),
                style: TextStyle(fontWeight: FontWeight.w600, fontSize: 20,color: CommonUtil.toTypeColor(transaction.transactionType)),
              ))),
      secondaryActions: <Widget>[
        Card(
          margin: EdgeInsets.symmetric(vertical: 10.0, horizontal: 5),
          child: new IconSlideAction(
            caption: 'Edit',
            color: Colors.blue,
            icon: Icons.edit,
            onTap: () {
              print(transaction);
              Navigator.of(context).push(MaterialPageRoute(
                  builder: (context) => AddItemPage(transaction: transaction)));
            },
          ),
        ),
        Card(
          margin: EdgeInsets.symmetric(vertical: 10.0, horizontal: 5),
          child: new IconSlideAction(
            caption: 'Delete',
            color: Colors.red,
            icon: Icons.delete,
          ),
        ),
      ],
    );
  }
}
