import 'package:tracko/Utils/ConstantUtil.dart';
import 'package:tracko/models/account.dart';
import 'package:tracko/models/user.dart';
import 'package:tracko/repositories/account_repository.dart';
import 'package:tracko/services/SessionService.dart';
import 'package:flutter/material.dart';
import 'package:tracko/di/di.dart';
import 'package:tracko/component/app_dropdown.dart';

class AccountDialog extends StatefulWidget {
  final Function callback;
  final Account? account;

  AccountDialog({required this.callback, this.account});

  @override
  _AccountDialogState createState() => _AccountDialogState();
}

class _AccountDialogState extends State<AccountDialog> {
  TextEditingController _controller = TextEditingController();
  String name = '';
  bool isEdit = false;
  late Account account;
  String selectedCurrency = 'INR';

  @override
  void initState() {
    super.initState();
    if (widget.account != null) {
      account = widget.account!;
      _controller.text = account.name;
      selectedCurrency = account.currency.isNotEmpty ? account.currency : 'INR';
      isEdit = true;
    } else {
      account = Account();
      selectedCurrency = sl<SessionService>().currentCurrencySymbol == '₹'
          ? 'INR'
          : 'INR'; // Just default to INR, backend handles defaults too
    }
  }

  addAccount() async {
    this.name = _controller.text;
    if (name.trim().length <= 0) {
      return;
    }
    account.name = name;
    account.currency = selectedCurrency;

    final repo = sl<AccountRepository>();

    if (account.id == null) {
      final created = await repo.createAccount(account.name, account.currency);
      account.id = created.id;
    } else {
      await repo.updateAccount(account.id!, account.name, account.currency);
    }
    print(account);
    widget.callback();
  }

  @override
  Widget build(BuildContext context) {
    return new AlertDialog(
      title: new Text(isEdit ? "Update Account" : "Add Account"),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          TextField(
            controller: _controller,
            decoration: new InputDecoration(
              hintText: 'Name',
            ),
          ),
          SizedBox(height: 16),
          AppFormDropdown<String>(
            value: selectedCurrency,
            items: ConstantUtil.CURRENCIES,
            labelBuilder: (c) => c,
            label: 'Currency',
            filled: false,
            onChanged: (newValue) {
              setState(() {
                selectedCurrency = newValue!;
              });
            },
          ),
        ],
      ),
      actions: <Widget>[
        ElevatedButton(
          onPressed: () {
            Navigator.pop(context);
          },
          child: Text("Cancel"),
        ),
        ElevatedButton(
          onPressed: () async {
            await this.addAccount();
            Navigator.pop(context);
          },
          child: Text(isEdit ? "Update" : "Add"),
        )
      ],
    );
  }
}
