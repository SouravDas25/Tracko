


import 'dart:async';

import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/models/user.dart';
import 'package:flutter/material.dart';
import 'package:pull_to_refresh/pull_to_refresh.dart';
import 'package:sms/sms.dart';

/*
* Text Razor APi KEY - 6a72f94bb1182f454256c0591e99e75dcc630bdb8054f2fa05edd15b
* */

class SmartPage extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return _SmartPage();
  }
}

class _SmartPage extends State<SmartPage> {

  bool animatePic = false;
  SmsQuery query = new SmsQuery();

  _SmartPage() {
//    initData();
  }

  void initData() async {
    animatePic = true;
    List<SmsMessage> messages = await query.getAllSms;
    print(messages[0].body);
    Future<void>.delayed(Duration(seconds: 1));
    setState(() {

    });
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      children: <Widget>[
        Container(
          child: Image.asset("assets/images/scaning.${animatePic?"gif":"png"}"),
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
            child: Text("Scan" ,style: TextStyle(fontSize: 25.0),),
          ),
        )
      ],
    );
  }
}
