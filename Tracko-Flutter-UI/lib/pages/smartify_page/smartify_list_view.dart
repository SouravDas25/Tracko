import 'package:Tracko/component/TransactionTile.dart';
import 'package:Tracko/models/transaction.dart';
import 'package:Tracko/services/SmsScanningService.dart';
import 'package:flutter/material.dart';
import 'package:percent_indicator/percent_indicator.dart';

class SmartListView extends StatelessWidget {
  final List<Transaction> transactions;
  final Function deleteSingle;
  final Function complete;
  final Function cancel;

  State<StatefulWidget> parent;

  SmartListView(this.transactions, this.parent,
      {required this.deleteSingle, required this.complete, required this.cancel});

  Color get progressBarColor {
    if (SmsScanningService.progress() < 0.25) {
      return Colors.redAccent;
    }
    if (SmsScanningService.progress() < 0.5) {
      return Color(0xffffbb33);
    }
    return Color(0xff00C851);
  }

  List<Widget> generate(BuildContext context) {
    List<Widget> widgets = <Widget>[];
    if (SmsScanningService.isRunning()) {
      widgets.add(Padding(
        padding: const EdgeInsets.only(top: 15, bottom: 8, left: 20, right: 20),
        child: LinearPercentIndicator(
          lineHeight: 20,
          backgroundColor: Color(0xffd0d6e2),
          percent: SmsScanningService.progress(),
          progressColor: progressBarColor,
          animateFromLastPercent: true,
          clipLinearGradient: true,
          animation: true,
          center: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8.0),
            child: Text(
              "${(SmsScanningService.progress() * 100).round()}%",
              style: TextStyle(color: Colors.black),
            ),
          ),
        ),
      ));
    }
    int i = 0;
    for (i = 0; i < transactions.length; i++) {
      Transaction transaction = transactions[i];
      widgets.add(IgnorePointer(
        ignoring:
        SmsScanningService.status == ScanningStatus.RUNNING ? true : false,
        child: TransactionTile(this.parent, transaction,
                (parent, Transaction transaction) {
              SmsScanningService.possibleTransactions.removeWhere(
                      (Transaction element) => element.id == transaction.id);
              if (SmsScanningService.possibleTransactions.isEmpty) {
                SmsScanningService.reset();
              }
              parent.setState(() {});
        }),
      ));
    }
    return widgets;
  }

  ElevatedButton get actionButton {
    if (SmsScanningService.status == ScanningStatus.RUNNING) {
      return ElevatedButton(
        style: ElevatedButton.styleFrom(
          padding: EdgeInsets.all(20.0),
          backgroundColor: !SmsScanningService.isCancelRequested
              ? Colors.redAccent
              : Colors.grey,
          foregroundColor: Colors.white,
        ),
        onPressed: () {
          if (!SmsScanningService.isCancelRequested) {
            this.cancel();
          }
        },
        child: Text(
          "CANCEL",
          style: TextStyle(fontSize: 20.0),
        ),
      );
    }
    return ElevatedButton(
      style: ElevatedButton.styleFrom(
        padding: EdgeInsets.all(20.0),
        backgroundColor: SmsScanningService.status == ScanningStatus.RUNNING
            ? Colors.grey
            : Colors.blueAccent,
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
