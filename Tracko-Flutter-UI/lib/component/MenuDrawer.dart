import 'package:tracko/models/user.dart';
import 'package:tracko/services/SessionService.dart';
import 'package:flutter/material.dart';

class MenuDrawer extends StatelessWidget {
  User user;

  MenuDrawer({required Key key}) : super(key: key) {
    this.loadData();
  }

  loadData() async {
    this.user = SessionService.currentUser();
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
