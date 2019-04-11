import 'dart:async';

import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/Utils/DestinationUtil.dart';
import 'package:expense_manager/Utils/SmartUtil.dart';
import 'package:expense_manager/Utils/enums.dart';
import 'package:expense_manager/component/FLushDialog.dart';
import 'package:expense_manager/models/transaction.dart';
import 'package:expense_manager/pages/smartify_page/list_view.dart';
import 'package:flutter/material.dart';
import 'package:sms/sms.dart';
import 'package:http/http.dart' as http;
import 'dart:convert' as convert;
import 'package:flushbar/flushbar.dart';
/*
* Text Razor APi KEY - 6a72f94bb1182f454256c0591e99e75dcc630bdb8054f2fa05edd15b
* */



class SmartPage extends StatefulWidget {
  List<Transaction> possibleTransactions = SmartUtil.getPT();
  ScanningStatus scanning = ScanningStatus.NOT_RUNNING;

  SmartPage() {
    if (possibleTransactions.length > 0) {
      this.scanning = ScanningStatus.COMPLETED;
    }
  }

  @override
  State<StatefulWidget> createState() {
    return _SmartPage();
  }
}

class _SmartPage extends State<SmartPage> {
  SmsQuery query = new SmsQuery();

  Flushbar flushbar;

  _SmartPage() {
    this.flushbar = Flushbar(
      icon: Icon(
        Icons.search,
        color: Colors.white,
      ),
      flushbarPosition: FlushbarPosition.BOTTOM,
      title: "Scanning...",
      isDismissible: false,
      showProgressIndicator: true,
      message: "Please be patient, It may take a while.",
      duration: Duration(seconds: 200),
      mainButton: FlatButton(
        onPressed: () {
          this.cancel();
        },
        child: Text(
          "Cancel",
          style: TextStyle(color: Colors.red, fontWeight: FontWeight.bold),
        ),
      ),
    );
  }

  void cancel() {
    flushbar.dismiss();
    widget.possibleTransactions.clear();
    widget.scanning = ScanningStatus.NOT_RUNNING;
    setState(() {});
  }

  void processMessages(List<SmsMessage> messages) async {
    setState(() {});
    print(messages.length);
    for (SmsMessage message in messages) {
//      print(message.toMap);
//      print(message.sender);
      Transaction transaction = await getApiData(message);
      if (transaction != null) {
//        transaction.dates.add(message.date);
        widget.possibleTransactions.add(transaction);
        print(transaction);
        setState(() {});
      }
    }
    setState(() {
      flushbar.dismiss();
      widget.scanning = ScanningStatus.COMPLETED;
    });
  }

  void initData() async {
    widget.scanning = ScanningStatus.RUNNING;
    List<SmsMessage> messages = await query.querySms(count: 20);
    processMessages(messages);
    flushbar.show(context);
  }

  void dismissSingle(int index){
    widget.possibleTransactions.removeAt(index);
    if(widget.possibleTransactions.length <= 0){
      widget.scanning = ScanningStatus.NOT_RUNNING;
    }
    setState(() {});
  }

  void dismissAll() {
    widget.possibleTransactions.clear();
    widget.scanning = ScanningStatus.NOT_RUNNING;
    setState(() {});
  }

  void saveAll() async {
    var adapter = await DatabaseUtil.getAdapter();
    TransactionBean transactionBean = TransactionBean(adapter);
    for(Transaction transaction in widget.possibleTransactions){
      await transactionBean.insert(transaction);
    }
    FlushDialog.flash(context, "Saved", "All Transaction Persisted!");
    widget.possibleTransactions.clear();
    widget.scanning = ScanningStatus.NOT_RUNNING;
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    if (widget.scanning == ScanningStatus.RUNNING ||
        widget.scanning == ScanningStatus.COMPLETED) {
      return SmartListView(
        widget.possibleTransactions,
        widget.scanning,
        dismissAll: this.dismissAll,
        dismissSingle: this.dismissSingle,
        saveAll: this.saveAll,
      );
    }
    return ListView(
      children: <Widget>[
        Image.asset(
          "assets/images/scaning.png",
          scale: 2.0,
        ),
        Container(
          padding: EdgeInsets.all(40.0),
          child: RaisedButton(
            padding: EdgeInsets.all(20.0),
            color: Theme.of(context).primaryColor,
            textColor: Colors.white,
            onPressed: () {
              initData();
            },
            child: Text(
              "Scan",
              style: TextStyle(fontSize: 25.0),
            ),
          ),
        )
      ],
    );
  }
}

Future<Transaction> getApiData(SmsMessage message) async {
  var url = DestinationUtil.pythonBackend() + "apis/";
  Map<String,String> body = new Map();
  body['text'] = message.body;
  body['address'] = message.address;
  body['date'] = message.dateSent.toString();
  String data = convert.jsonEncode(body);
  var response = await http.post(url,body: data, headers: {"Content-Type": "application/json"},);
//  print(response.statusCode);
  if (response.statusCode == 200) {
//    print(response.body);
    var jsonResponse = convert.jsonDecode(response.body);
    print(jsonResponse);
    Transaction transaction = await Transaction.fromJson(jsonResponse);
    if (transaction != null) {
      return transaction;
    }
    return null;
  } else {
    return null;
  }
}
