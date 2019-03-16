import 'dart:async';

import 'package:expense_manager/Utils/CommonUtil.dart';
import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/Utils/SmartUtil.dart';
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
  bool scanning = false;

  SmartPage() {
    if (possibleTransactions.length > 0) this.scanning = true;
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
          this.cancle();
        },
        child: Text(
          "Cancle",
          style: TextStyle(color: Colors.red, fontWeight: FontWeight.bold),
        ),
      ),
    );
  }

  void cancle() {
    flushbar.dismiss();
    widget.possibleTransactions.clear();
    widget.scanning = false;
    setState(() {});
  }

  Future<PossibleTransaction> getApiData(String message) async {
    var url = "http://souravdas25.pythonanywhere.com/apis/?text=$message";

    // Await the http get response, then decode the json-formatted responce.
    var response = await http.get(url);
    if (response.statusCode == 200) {
      var jsonResponse = convert.jsonDecode(response.body);
      print(jsonResponse);
      PossibleTransaction transaction =
          PossibleTransaction.fromJson(jsonResponse);
      if (transaction.valid) {
        return transaction;
      }
      return null;
    } else {
      return null;
    }
  }

  void processMessages(List<SmsMessage> messages) async {
    setState(() {});
    print(messages.length);
    for (SmsMessage message in messages) {
//      print(message.body);
      PossibleTransaction transaction = await getApiData(message.body);
      if (transaction != null) {
        transaction.dates.add(message.date);
        widget.possibleTransactions.add(transaction);
        print(transaction);
        setState(() {});
      }
    }
    setState(() {
      flushbar.dismiss();
//      scanning = false;
    });
  }

  void initData() async {
    widget.scanning = true;
    List<SmsMessage> messages = await query.querySms(count: 20);
    processMessages(messages);
    flushbar.show(context);
  }

  @override
  Widget build(BuildContext context) {
    if (widget.scanning) {
      return ListView(
        children: widget.possibleTransactions
            .map<Widget>((PossibleTransaction transaction) {
          return Card(
            child: ListTile(
              onTap: () {
                Navigator.of(context).push(MaterialPageRoute(
                    builder: (context) => SmartAddItemPage(transaction)));
              },
              contentPadding: EdgeInsets.all(10.0),
              title: Text(
                transaction.category == null
                    ? "Item "
                    : transaction.category.name,
                style: TextStyle(fontSize: 20.0),
              ),
              subtitle: Text(CommonUtil.humanDate(transaction.dates[0])),
              trailing: Text(
                "₹ " + transaction.amounts[0].toString(),
                style: TextStyle(fontWeight: FontWeight.w600, fontSize: 20),
              ),
            ),
          );
        }).toList()
              ..add(Row(
                children: <Widget>[
                  Expanded(
                    child: SizedBox(
                      width: double.infinity,
                      child: RaisedButton(
                        color: Colors.green,
                        textColor: Colors.white,
                        onPressed: () {},
                        child: Text(
                          "Save All",
                          style: TextStyle(fontSize: 20.0),
                        ),
                      ),
                    ),
                  ),
                  Column(
                    children: <Widget>[
                      RaisedButton(
                        color: Colors.red,
                        onPressed: () {},
                        child: Icon(
                          Icons.delete,
                          color: Colors.white,
                        ),
                      ),
                    ],
                  )
                ],
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
