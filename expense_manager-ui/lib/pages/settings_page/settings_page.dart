



import 'package:flutter/material.dart';
import 'package:pull_to_refresh/pull_to_refresh.dart';

class SettingsPage extends StatefulWidget {

  @override
  State<StatefulWidget> createState() {
    return _SettingsPage();
  }

}

class _SettingsPage extends State<SettingsPage> {

  List<Map> accounts;

  _SettingsPage() {
    initData();
  }

  void initData() {

  }

  @override
  Widget build(BuildContext context) {
    return SmartRefresher(
      child: ListView(
        children: <Widget>[

        ],
      ),
    );
  }



}