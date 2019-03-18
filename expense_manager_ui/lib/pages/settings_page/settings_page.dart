import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/models/user.dart';
import 'package:flutter/material.dart';
import 'package:pull_to_refresh/pull_to_refresh.dart';

class SettingsPage extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return _SettingsPage();
  }
}

class _SettingsPage extends State<SettingsPage> {
  User user;

  _SettingsPage() {
    initData();
  }

  void initData() async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    user = await UserBean.getCurrentUser(adapter: adapter);
    await adapter.close();
    Future<void>.delayed(Duration(seconds: 1));
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: ListView(children: <Widget>[
        Card(
          elevation: 0,
          child: ListTile(
            leading: Icon(
              Icons.account_circle,
              size: 40.0,
            ),
            title: Padding(
              padding: EdgeInsets.symmetric(vertical: 5.0),
              child: Text(user == null ? "" : user.name,
                  style: TextStyle(fontSize: 20.0)),
            ),
            subtitle: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              textDirection: TextDirection.ltr,
              children: <Widget>[
                Text(user == null ? "" : user.phoneNo),
                Text(user == null ? "" : user.email),
              ],
            ),
          ),
        ),
        Card(
          elevation: 0,
          child: ListTile(
            leading: Icon(Icons.backup, size: 30.0),
            title: Text("Auto Backup", style: TextStyle(fontSize: 20.0)),
            trailing: Switch(value: false, onChanged: (val) {}),
          ),
        ),
        Card(
          elevation: 0,
          child: ListTile(
            leading: Icon(Icons.dns, size: 30.0),
            title: Text("Reset Datebase", style: TextStyle(fontSize: 20.0)),
            trailing: RaisedButton(
              color: Theme.of(context).primaryColor,
              textColor: Colors.white,
              onPressed: () {},
              child: Text("Reset"),
            ),
          ),
        )
      ]),
    );
  }
}
