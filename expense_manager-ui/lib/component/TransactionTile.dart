


import 'package:expense_manager/Utils/CommonUtil.dart';
import 'package:expense_manager/pages/add_item_page/add_item.dart';
import 'package:flutter/material.dart';
import 'package:flutter_slidable/flutter_slidable.dart';

class TransactionTile extends StatelessWidget {

  Map<String,dynamic> transaction;

  TransactionTile(this.transaction);

  @override
  Widget build(BuildContext context) {
    return Slidable(
      delegate: new SlidableScrollDelegate(),
      child: Card(
          child: ListTile(
            trailing: Text(
              CommonUtil.toCurrency(transaction['amount']),
              style: TextStyle(
                  fontWeight: FontWeight.w600, fontSize: 20),
            ),
            title: Text(
              transaction['category_name'].toString(),
              style: TextStyle(
                  fontWeight: FontWeight.w500, fontSize: 20),
            ),
            subtitle: Text(
                transaction['comments'].toString().isEmpty
                    ? CommonUtil.humanDate(transaction['date'])
                    : transaction['comments']),
          )),
      secondaryActions: <Widget>[
        Card(
          margin: EdgeInsets.symmetric(vertical:10.0,horizontal: 5),
          child: new IconSlideAction(
            caption: 'Edit',
            color: Colors.blue,
            icon: Icons.edit,
            onTap: () {
              print(transaction);
              Navigator.of(context).push(MaterialPageRoute(
                  builder: (context) =>
                      AddItemPage(id: transaction['id'])));
            },
          ),
        ),
        Card(
          margin: EdgeInsets.symmetric(vertical:10.0,horizontal: 5),
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