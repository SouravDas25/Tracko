import 'package:tracko/models/account.dart';
import 'package:tracko/models/user.dart';
import 'package:tracko/repositories/account_repository.dart';
import 'package:tracko/services/SessionService.dart';
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
    if (name.trim().length <= 0) {
      return;
    }
    Account account = this.account ?? Account();
    account.name = name;
    User user = SessionService.currentUser();
    account.userId = user.id;
    final repo = AccountRepository();
    if (account.id == null) {
      final created = await repo.createAccount(account.name, user.globalId);
      account.id = created.id;
    } else {
      await repo.updateAccount(account.id!, account.name, user.globalId);
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
