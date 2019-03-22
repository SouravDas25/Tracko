import 'dart:async';

import 'package:expense_manager/Utils/CommonUtil.dart';
import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/Utils/SmartUtil.dart';
import 'package:expense_manager/Utils/enums.dart';
import 'package:expense_manager/component/PossibleTransactionList.dart';
import 'package:expense_manager/models/PossibleTransaction.dart';
import 'package:expense_manager/models/transaction.dart';
import 'package:expense_manager/models/user.dart';
import 'package:expense_manager/pages/smart_add_item/smart_add_item.dart';
import 'package:flutter/material.dart';
import 'package:flutter_slidable/flutter_slidable.dart';
import 'package:pull_to_refresh/pull_to_refresh.dart';
import 'package:sms/sms.dart';
import 'package:http/http.dart' as http;
import 'dart:convert' as convert;
import 'package:flushbar/flushbar.dart';
/*
* Text Razor APi KEY - 6a72f94bb1182f454256c0591e99e75dcc630bdb8054f2fa05edd15b
* */



class SmartPage extends StatefulWidget {
  List<PossibleTransaction> possibleTransactions = SmartUtil.getPT();
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
      PossibleTransaction transaction = await getApiData(message);
      if (transaction != null) {
        transaction.dates.add(message.date);
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

  void dismissAll() {
    widget.possibleTransactions.clear();
    widget.scanning = ScanningStatus.NOT_RUNNING;
    setState(() {});
  }

  void saveAll() {}

  @override
  Widget build(BuildContext context) {
    if (widget.scanning == ScanningStatus.RUNNING ||
        widget.scanning == ScanningStatus.COMPLETED) {
      return ListView(
        children: PossibleTransactionList(widget.possibleTransactions,widget.scanning).generate(context)
              ..add(Padding(
                padding: const EdgeInsets.all(20.0),
                child: Row(
                  children: <Widget>[
                    Expanded(
                      child: IgnorePointer(
                        ignoring: widget.scanning == ScanningStatus.RUNNING
                            ? true
                            : false,
                        child: RaisedButton(
                          padding: EdgeInsets.all(20.0),
                          color: Colors.lightGreen,
                          textColor: Colors.white,
                          onPressed: () {},
                          child: Text(
                            "Save All",
                            style: TextStyle(fontSize: 20.0),
                          ),
                        ),
                      ),
                    ),
                    Container(
                      child: IgnorePointer(
                        ignoring: widget.scanning == ScanningStatus.RUNNING
                            ? true
                            : false,
                        child: RaisedButton(
                          padding: EdgeInsets.all(20.0),
                          color: Colors.red,
                          onPressed: () {
                            dismissAll();
                          },
                          child: Icon(
                            Icons.delete,
                            color: Colors.white,
                          ),
                        ),
                      ),
                    )
                  ],
                ),
              )),
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

Future<PossibleTransaction> getApiData(SmsMessage message) async {
  var url = "http://souravdas25.pythonanywhere.com/apis/";
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
    PossibleTransaction transaction = await PossibleTransaction.fromJson(jsonResponse);
    if (transaction.valid) {
      return transaction;
    }
    return null;
  } else {
    return null;
  }
}
