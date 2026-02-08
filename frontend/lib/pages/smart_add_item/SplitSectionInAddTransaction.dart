import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/dtos/TrackoContact.dart';
import 'package:tracko/services/SessionService.dart';
import 'package:flutter/material.dart';
import 'package:flutter_slidable/flutter_slidable.dart';

// ignore: must_be_immutable
class SplitSectionInAddTransaction extends StatelessWidget {
  State parentState;
  final double amount;
  final String currencySymbol;
  Set<TrakoContact> splitList;
  List<TextEditingController> textEditingControllers;
  bool disableSlide = false;

  SplitSectionInAddTransaction(
      {required this.parentState,
      required this.amount,
      this.currencySymbol = '₹',
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
    // Using simple format with symbol
    return "$currencySymbol ${a.toStringAsFixed(2)}";
  }

  deleteSplit(TrakoContact element, int index) {
    splitList.remove(element);
    textEditingControllers.removeAt(index);
    this.parentState.setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    List<TrakoContact> contactsList = splitList.toList();
    return ListView.builder(
      physics: NeverScrollableScrollPhysics(),
      shrinkWrap: true,
      itemCount: contactsList.length,
      itemBuilder: (context, int index) {
        TrakoContact element = contactsList[index];

        String displayAmount = calcAmount();

        return Padding(
          padding: const EdgeInsets.only(bottom: 8.0),
          child: Slidable(
            enabled: !disableSlide,
            endActionPane: ActionPane(
              motion: ScrollMotion(),
              children: [
                SlidableAction(
                  onPressed: (context) {
                    deleteSplit(element, index);
                  },
                  backgroundColor: Theme.of(context).colorScheme.error,
                  foregroundColor: Theme.of(context).colorScheme.onError,
                  icon: Icons.delete,
                  borderRadius: BorderRadius.circular(16),
                ),
              ],
            ),
            child: Container(
              decoration: BoxDecoration(
                color: Theme.of(context).cardColor,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(
                    color: Theme.of(context).dividerColor.withOpacity(0.1)),
              ),
              child: ListTile(
                contentPadding:
                    EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                dense: true,
                trailing: Text(
                  displayAmount,
                  style: TextStyle(fontSize: 18.0, fontWeight: FontWeight.bold),
                ),
                title: Text(
                  element.phoneNo != SessionService.currentUser().phoneNo
                      ? element.name
                      : "You",
                  style: TextStyle(fontSize: 16.0, fontWeight: FontWeight.w600),
                ),
                subtitle: Text(
                  element.phoneNo != null ? element.phoneNo : "",
                  style: TextStyle(fontSize: 14.0),
                ),
                leading: WidgetUtil.textAvatar(element.name),
              ),
            ),
          ),
        );
      },
    );
  }
}
