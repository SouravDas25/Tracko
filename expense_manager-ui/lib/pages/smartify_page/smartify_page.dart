import 'dart:async';

import 'package:expense_manager/Utils/CommonUtil.dart';
import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/models/PossibleTransaction.dart';
import 'package:expense_manager/models/transaction.dart';
import 'package:expense_manager/models/user.dart';
import 'package:flutter/material.dart';
import 'package:pull_to_refresh/pull_to_refresh.dart';
import 'package:sms/sms.dart';
import 'package:http/http.dart' as http;
import 'dart:convert' as convert;
import 'package:flushbar/flushbar.dart';
/*
* Text Razor APi KEY - 6a72f94bb1182f454256c0591e99e75dcc630bdb8054f2fa05edd15b
* */

class SmartPage extends StatefulWidget {

  List<PossibleTransaction> possibleTransactions = List<PossibleTransaction>();
  bool scanning = false;

  @override
  State<StatefulWidget> createState() {
    return _SmartPage();
  }
}

class _SmartPage extends State<SmartPage> {


  SmsQuery query = new SmsQuery();

  Flushbar flushbar = Flushbar(
    icon: Icon(Icons.search,color: Colors.white,),
    flushbarPosition: FlushbarPosition.BOTTOM,
    title: "Scanning...",
    isDismissible: false,
    showProgressIndicator: true,
    message: "Please be patient, It may take a while.",
    duration: Duration(seconds: 200),
  );


  _SmartPage() {
//    initData();
  }

  Future<PossibleTransaction> getApiData(String message) async {
    var url = "http://souravdas25.pythonanywhere.com/apis/?text=$message";

    // Await the http get response, then decode the json-formatted responce.
    var response = await http.get(url);
    if (response.statusCode == 200) {
      var jsonResponse = convert.jsonDecode(response.body);
      print(jsonResponse);
      PossibleTransaction transaction = PossibleTransaction.fromJson(
          jsonResponse);
      if (transaction.valid) {
        return transaction;
      }
      return null;
    } else {
      return null;
    }
  }

  void processMessages(List<SmsMessage> messages) async {
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
    if(widget.scanning){
      return ListView(
        children: widget.possibleTransactions.map((
            PossibleTransaction transaction) {
          return Card(
            child: ListTile(
              onTap: () {

              },
              contentPadding: EdgeInsets.all(10.0),
              title: Text("Item "),
              subtitle: Text(CommonUtil.humanDate(transaction.dates[0])),
              trailing: Text(
                "₹ " + transaction.amounts[0].toString(),
                style: TextStyle(
                    fontWeight: FontWeight.w600, fontSize: 20),
              ),
            ),
          );
        }).toList(),
      );
    }
    return ListView(
      children: <Widget>[
        Image.asset("assets/images/scaning.png",scale: 2.0,) ,
        Container(
          padding: EdgeInsets.all(40.0),
          child: RaisedButton(
            padding: EdgeInsets.all(20.0),
            color: Theme
                .of(context)
                .primaryColor,
            textColor: Colors.white,
            onPressed: () {
              initData();
            },
            child: Text("Scan", style: TextStyle(fontSize: 25.0),),
          ),
        )
      ],
    );
  }
}
