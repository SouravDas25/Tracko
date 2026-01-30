import 'package:Tracko/component/FLushDialog.dart';
import 'package:Tracko/controllers/SmsController.dart';
import 'package:Tracko/controllers/TransactionController.dart';
import 'package:Tracko/models/transaction.dart';
import 'package:Tracko/pages/smartify_page/smartify_list_view.dart';
import 'package:Tracko/services/SmsScanningService.dart';
import 'package:flutter/material.dart';

/*
* Text Razor APi KEY - 6a72f94bb1182f454256c0591e99e75dcc630bdb8054f2fa05edd15b
* */

class SmartPage extends StatefulWidget {
  List<Transaction> get foundTransactions =>
      SmsScanningService.possibleTransactions;

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
    SmsScanningService.stopScan();
    setState(() {});
  }

  void initData() async {
    try {
      bool isNewMsg = await SmsController.isNewSmsPresent();
      if (!isNewMsg) {
        FlushDialog.flash(
            context, "Process Completed", "No new sms found in the device.");
      } else {
        SmsScanningService.scan(callback: onUpdate).then((value) => onUpdate(value));
      }
    } catch (e) {
      print(e);
      if (this.mounted) {
        FlushDialog.flash(context, "Permission Denied",
            "You have to grant sms permission to use this feature.");
      }
    }
    setState(() {});
  }

  void deleteSingle(int index) {
    Transaction transaction = widget.foundTransactions[index];
    TransactionController.deleteById(transaction.id ?? 0);
    widget.foundTransactions.removeAt(index);
    if (widget.foundTransactions.length <= 0) {
      SmsScanningService.reset();
    }
    setState(() {});
  }

  void complete() async {
    SmsScanningService.reset();
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    if (SmsScanningService.status == ScanningStatus.RUNNING ||
        SmsScanningService.status == ScanningStatus.COMPLETED) {
      return SmartListView(
        widget.foundTransactions,
        this,
        deleteSingle: this.deleteSingle,
        complete: this.complete,
        cancel: this.cancel,
      );
    }
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
              "SCAN",
              style: TextStyle(fontSize: 22.0),
            ),
          ),
        )
      ],
    );
  }
}
