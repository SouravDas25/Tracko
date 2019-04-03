import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/models/user.dart';
import 'package:flutter/material.dart';

class MenuDrawer extends StatelessWidget {

  User user;

  MenuDrawer({Key key}) : super(key: key) {
    this.loadData();
  }

  loadData() async {
    this.user = await UserBean.getCurrentUser();
  }

  @override
  Widget build(BuildContext context) {
    return new Drawer(
        child: new ListView(
      children: <Widget>[
        new DrawerHeader(
          child: new Text('${this.user.name}'),
        ),
        new ListTile(
          title: new Text('First Menu Item'),
          onTap: () {},
        ),
        new ListTile(
          title: new Text('Second Menu Item'),
          onTap: () {},
        ),
        new Divider(),
        new ListTile(
          title: new Text('About'),
          onTap: () {},
        ),
      ],
    ));
  }
}
