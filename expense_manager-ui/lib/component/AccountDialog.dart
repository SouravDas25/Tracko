

import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/models/account.dart';
import 'package:expense_manager/models/user.dart';
import 'package:flutter/material.dart';

class AccountDialog extends StatelessWidget {

  String name;
  Function callback;

  AccountDialog({this.callback});

  addAccount() async {
    if(name.trim().length <= 0){
      return;
    }
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    AccountBean accountBean = AccountBean(adapter);
    Account account = new Account();
    account.name = name;
    User user = await UserBean.getCurrentUser(adapter: adapter);
    accountBean.associateUser(account, user);
    await accountBean.insert(account);
    print(account);
//    await adapter.close();
    if(callback != null){
      callback();
    }
  }

  @override
  Widget build(BuildContext context) {
    return new AlertDialog(
      title: new Text("Add Account"),
      content: TextField(
        decoration: new InputDecoration(
          hintText: 'Name',
        ),
        onChanged: (text) {
          name = text;
        },
      ),
      actions: <Widget>[
        RaisedButton(
          onPressed: () {
            Navigator.pop(context);
          },
          textColor: Colors.white,
          child: Text("Cancel"),
        ),
        RaisedButton(
          onPressed: () {
            this.addAccount();
            Navigator.pop(context);
          },
          textColor: Colors.white,
          child: Text("Add"),
        )
      ],
    );
  }

}