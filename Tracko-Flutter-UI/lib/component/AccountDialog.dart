import 'package:Tracko/Utils/DatabaseUtil.dart';
import 'package:Tracko/models/account.dart';
import 'package:Tracko/models/user.dart';
import 'package:Tracko/services/SessionService.dart';
import 'package:flutter/material.dart';

class AccountDialog extends StatelessWidget {
  TextEditingController _controller = TextEditingController();
  String name = '';
  Function callback;
  bool isEdit = false;
  Account? account;

  AccountDialog({required this.callback, this.account}) {
    if (this.account != null) {
      _controller.text = this.account?.name ?? '';
      isEdit = true;
    } else
      this.account = new Account();
  }

  addAccount() async {
    this.name = _controller.text;
    if (name
        .trim()
        .length <= 0) {
      return;
    }
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    // TODO: Reimplement with raw SQL after jaguar_orm removal
    // AccountBean accountBean = AccountBean(adapter);
    Account account = this.account ?? Account();
    account.name = name;
    User user = SessionService.currentUser();
    // accountBean.associateUser(account, user);
    account.userId = user.id;
    // await accountBean.upsert(account);
    // Stub: Insert or update account using raw SQL
    if (account.id == null) {
      await adapter.rawInsert('INSERT INTO accounts (name, userId) VALUES (?, ?)', [account.name, account.userId]);
    } else {
      await adapter.rawUpdate('UPDATE accounts SET name = ? WHERE id = ?', [account.name, account.id]);
    }
    print(account);
//    await adapter.close();
    callback();
  }

  @override
  Widget build(BuildContext context) {
    return new AlertDialog(
      title: new Text(isEdit ? "Update Account" : "Add Account"),
      content: TextField(
        controller: _controller,
        decoration: new InputDecoration(
          hintText: 'Name',
        ),
      ),
      actions: <Widget>[
        ElevatedButton(
          onPressed: () {
            Navigator.pop(context);
          },
          // textColor: Colors.white, // Use style instead
          child: Text("Cancel"),
        ),
        ElevatedButton(
          onPressed: () async {
            await this.addAccount();
            Navigator.pop(context);
          },
          // textColor: Colors.white, // Use style instead
          child: Text(isEdit ? "Update" : "Add"),
        )
      ],
    );
  }
}
