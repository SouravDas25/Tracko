import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/dtos/TrackoContact.dart';
import 'package:tracko/services/SessionService.dart';
import 'package:flutter/material.dart';
import 'package:flutter_slidable/flutter_slidable.dart';

// ignore: must_be_immutable
class SplitSectionInAddTransaction extends StatelessWidget {
  State parentState;
  final double amount;
  Set<TrakoContact> splitList;
  List<TextEditingController> textEditingControllers;
  bool disableSlide = false;

  SplitSectionInAddTransaction(
      {required this.parentState,
      required this.amount,
      required this.splitList,
      required this.textEditingControllers})
      : super() {
    if (splitList.length == 1) {
      disableSlide = true;
    }
  }

  String calcAmount() {
    double a;
    if (amount > 0 && splitList.length > 0) {
      a = (amount / splitList.length);
    } else {
      a = 0;
    }
    return a.toStringAsFixed(2);
  }

  deleteSplit(TrakoContact element, int index) {
    splitList.remove(element);
    textEditingControllers.removeAt(index);
    this.parentState.setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    Iterator<TextEditingController> iterator = textEditingControllers.iterator;
    Iterator<TrakoContact> splitListIterator = splitList.iterator;
    return ListView.builder(
      physics: NeverScrollableScrollPhysics(),
      shrinkWrap: true,
      itemCount: splitList.length,
      itemBuilder: (context, int index) {
        splitListIterator.moveNext();
        TrakoContact element = splitListIterator.current;
        iterator.moveNext();
        TextEditingController splitAmount = iterator.current;
        splitAmount.text = calcAmount();
        return Slidable(
          enabled: !disableSlide,
          endActionPane: ActionPane(
            motion: ScrollMotion(),
            children: [
              SlidableAction(
                onPressed: (context) {
                  deleteSplit(element, index);
                },
                backgroundColor: Colors.red,
                foregroundColor: Colors.white,
                icon: Icons.delete,
              ),
            ],
          ),
          child: ListTile(
            contentPadding: EdgeInsets.all(0.0),
            dense: true,
            trailing: Text(
              calcAmount(),
              style: TextStyle(fontSize: 20.0),
            ),
            title: Text(
              element.phoneNo != SessionService.currentUser().phoneNo
                  ? element.name
                  : "You",
              style: TextStyle(fontSize: 16.0),
            ),
            subtitle: Text(
              element.phoneNo != null ? element.phoneNo : "",
              style: TextStyle(fontSize: 15.0),
            ),
            leading: WidgetUtil.textAvatar(element.name),
          ),
        );
      },
    );
  }
}
