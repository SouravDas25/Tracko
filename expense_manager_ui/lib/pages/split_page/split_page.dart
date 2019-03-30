import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/models/user.dart';
import 'package:pull_to_refresh/pull_to_refresh.dart';
import 'package:flutter/material.dart';
import 'package:sms/contact.dart';


class SplitPage extends StatefulWidget {
  splitAmount(double amount){

  }
  @override
  State<StatefulWidget> createState() {
    return _SplitPage();
  }
}

class _SplitPage extends State<SplitPage> {
  _SplitPage() {
    initData();
  }

  void initData() async {

  }

  @override
  Widget build(BuildContext context) {
    return new Scaffold(
      floatingActionButton: RaisedButton(
        padding: EdgeInsets.all(10.0),
        onPressed: () {},
        child: Icon(Icons.add,size: 45.0,),
        textColor: Colors.white,
        color: Colors.blue,
        shape: CircleBorder(),
      ),
    );
  }
}

