



import 'package:expense_manager/Utils/CommonUtil.dart';
import 'package:expense_manager/Utils/enums.dart';
import 'package:expense_manager/models/PossibleTransaction.dart';
import 'package:expense_manager/pages/smart_add_item/smart_add_item.dart';
import 'package:flutter/material.dart';
import 'package:flutter_slidable/flutter_slidable.dart';

class PossibleTransactionList {

  List<PossibleTransaction> possibleTransactions;
  ScanningStatus scaning;

  PossibleTransactionList(this.possibleTransactions,this.scaning);

  List<Widget> generate(BuildContext context) {
    List<Widget> widgets = List<Widget>();
    int i = 0;
    for(i=0;i<possibleTransactions.length;i++){
      int index = i;
      PossibleTransaction transaction = possibleTransactions[i];
      widgets.add(
          IgnorePointer(
            ignoring: this.scaning == ScanningStatus.RUNNING ? true : false,
            child: Slidable(
              child: Card(
                child: ListTile(
                  onTap: () {
                    Navigator.of(context).push(MaterialPageRoute(
                        builder: (context) => SmartAddItemPage(transaction,index)));
                  },
                  contentPadding: EdgeInsets.all(10.0),
                  leading: Image.network(
                    transaction.logo(),
                    scale: transaction.entities.length <= 0 ? 1.0 : 2.0,
                  ),
                  title: Text(
                    transaction.name,
                    style: TextStyle(fontSize: 20.0),
                  ),
                  subtitle: Text(
                    transaction.entities.length <= 0
                        ? CommonUtil.humanDate(transaction.dates[0]) : transaction.entities[0].category,
                  ),
                  trailing: Text(
                    "₹ " + transaction.amounts[0].toString(),
                    style: TextStyle(fontWeight: FontWeight.w600, fontSize: 20),
                  ),
                ),
              ), delegate: new SlidableScrollDelegate(),
              secondaryActions: <Widget>[
                Card(
                  margin: EdgeInsets.symmetric(vertical:10.0,horizontal: 5),
                  child: new IconSlideAction(
                    color: Colors.red,
                    icon: Icons.delete,
                    onTap: () {},
                  ),
                ),
              ],
            ),
          )
      );
    }
    return widgets;
  }

}

class IndexHolder extends ListTile {
  @override
  Widget build(BuildContext context) {
    return null;
  }

}