import 'package:expense_manager/Utils/CommonUtil.dart';
import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/Utils/enums.dart';
import 'package:expense_manager/component/AccountDialog.dart';
import 'package:expense_manager/component/CategoryDialog.dart';
import 'package:expense_manager/component/FLushDialog.dart';
import 'package:expense_manager/component/PaddedText.dart';
import 'package:expense_manager/component/screen.dart';
import 'package:expense_manager/models/account.dart';
import 'package:expense_manager/models/category.dart';
import 'package:expense_manager/models/transaction.dart';
import 'package:expense_manager/models/user.dart';
import 'package:expense_manager/pages/smart_add_item/smart_add_item.dart';
import 'package:flushbar/flushbar.dart';
import 'package:flutter/material.dart';
import 'package:datetime_picker_formfield/datetime_picker_formfield.dart';
import 'package:intl/intl.dart';

class AddItemPage extends StatefulWidget {
  Transaction transaction;

  AddItemPage({this.transaction});

  @override
  State<StatefulWidget> createState() {
    return _AddItemPage(this.transaction);
  }
}

class _AddItemPage extends State<AddItemPage> {
  Transaction newTransaction;

  _AddItemPage(this.newTransaction) {
    if (newTransaction == null) newTransaction = Transaction.defaultObject();
  }

  save(Transaction current) async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();

    TransactionBean transactionBean = TransactionBean(adapter);
    await transactionBean.upsert(current);

    FlushDialog.flash(context, "Saved", "Transaction Persisted");
  }

  @override
  Widget build(BuildContext context) {
    return SmartAddItemPage(
      newTransaction,
      mainButtonText: "Save",
      saveCallback: this.save,
    );
  }
}
