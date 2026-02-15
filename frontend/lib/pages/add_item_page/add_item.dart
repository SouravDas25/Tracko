import 'package:tracko/component/FLushDialog.dart';
import 'package:tracko/controllers/TransactionController.dart';
import 'package:tracko/models/transaction.dart';
import 'package:tracko/pages/smart_add_item/smart_add_item.dart';
import 'package:flutter/material.dart';

class AddItemPage extends StatefulWidget {
  final Transaction? transaction;

  AddItemPage({this.transaction});

  @override
  State<StatefulWidget> createState() {
    return _AddItemPage(this.transaction);
  }
}

class _AddItemPage extends State<AddItemPage> {
  late Transaction newTransaction;

  _AddItemPage(Transaction? transaction) {
    newTransaction = transaction ?? Transaction.defaultObject();
  }

  complete(Transaction current) async {
    await FlushDialog.flash(context, "Saved", "Transaction Saved");
  }

  save(Transaction current) async {
    await TransactionController.saveTransaction(current);
  }

  @override
  Widget build(BuildContext context) {
    return SmartAddItemPage(
      newTransaction,
      mainButtonText: "Save",
      saveCallback: this.save,
      complete: this.complete,
    );
  }
}
