


import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/models/user.dart';
import 'package:flutter/material.dart';
import 'package:pull_to_refresh/pull_to_refresh.dart';

class SplitPage extends StatefulWidget {
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
    return ListView(
      children: <Widget>[

      ],
    );
  }
}
