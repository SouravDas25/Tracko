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
import 'package:contacts_service/contacts_service.dart';
import 'package:expense_manager/component/select_contact.dart';
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

<<<<<<< HEAD:expense_manager_ui/lib/pages/add_item_page/add_item.dart
  DateTime date = DateTime.now();
  Category category;
  Account account;

  List<Category> categories = new List<Category>(0);

  List<Account> accounts = new List<Account>(0);

  _AddItemPage(int id) {
    initData(id);


=======
  _AddItemPage(this.newTransaction) {
    if (newTransaction == null) newTransaction = Transaction.defaultObject();
>>>>>>> 71b09c5512961e663f826f6449eb5e9aa867b94f:expense_manager-ui/lib/pages/add_item_page/add_item.dart
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
<<<<<<< HEAD:expense_manager_ui/lib/pages/add_item_page/add_item.dart
    return Screen(
      body: ListView(
        children: <Widget>[
          PaddedText(
            "Add Expense/Income",
            style: TextStyle(fontSize: 23),
            vertical: 10.0,
          ),
          new DropdownButton<Category>(
            isExpanded: true,
            value: category,
            hint: Text("Please choose a Category"),
            items: categories.map((Category value) {
              return new DropdownMenuItem<Category>(
                value: value,
                child: new Text(value.name),
              );
            }).toList(),
            onChanged: (Category value) {
              setState(() {
                category = value;
              });
            },
          ),
          SizedBox(
            width: double.infinity,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: <Widget>[
                FlatButton(
                  textTheme: ButtonTextTheme.primary,
                  onPressed: () {
                    showDialog(
                      context: context,
                      builder: (_) => CategoryDialog(
                            callback: () {
                              setState(() {
                                initData(-1);
                              });
                            },
                          ),
                    );
                  },
                  child: Text(
                    "Add category",
                    textAlign: TextAlign.right,
                  ),
                )
              ],
            ),
          ),
          TextField(
            keyboardType: TextInputType.numberWithOptions(decimal: true),
            decoration: new InputDecoration(
              labelText: 'Amount',
            ),
            controller: amount,
          ),
          DateTimePickerFormField(
            initialValue: date,
            inputType: InputType.date,
            format: DateFormat('dd-MMM-yyyy'),
            editable: false,
            decoration: InputDecoration(
                labelText: 'Date', hasFloatingPlaceholder: false),
            onChanged: (date) {
              this.date = date;
            },
          ),
          new DropdownButton<Account>(
            isExpanded: true,
            value: account,
            hint: Text("Please choose a Account"),
            items: accounts.map((Account value) {
              return new DropdownMenuItem<Account>(
                value: value,
                child: new Text(value.name),
              );
            }).toList(),
            onChanged: (Account value) {
              setState(() {
                account = value;
              });
            },
          ),
          SizedBox(
              width: double.infinity,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: <Widget>[
                  FlatButton(
                    textTheme: ButtonTextTheme.primary,
                    onPressed: () {
                      showDialog(
                        context: context,
                        builder: (_) => AccountDialog(
                              callback: () {
                                setState(() {
                                  initData(-1);
                                });
                              },
                            ),
                      );
                    },
                    child: Text(
                      "Add account",
                      textAlign: TextAlign.right,
                    ),
                  )
                ],
              )),
          TextField(
            controller: comments,
            decoration: new InputDecoration(
              labelText: 'comments',
            ),
            onChanged: (text) {},
          ),
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 8.0),
            child: RaisedButton(
              color: Theme.of(context).primaryColor,
              padding: EdgeInsets.all(20.0),
              onPressed: () {
//                select_contact ob =new select_contact();
                Navigator.of(context).push(MaterialPageRoute(
                    builder: (context) =>
                        select_contact()));
              },
              textColor: Colors.white,
              child: Text("Split the Amount"),

            ),
          ),
          RaisedButton(
            color: Theme.of(context).primaryColor,
            padding: EdgeInsets.all(20.0),
            onPressed: () {},
            textColor: Colors.white,
            child: Text("Save"),
          )
        ],
      ),
=======
    return SmartAddItemPage(
      newTransaction,
      mainButtonText: "Save",
      saveCallback: this.save,
>>>>>>> 71b09c5512961e663f826f6449eb5e9aa867b94f:expense_manager-ui/lib/pages/add_item_page/add_item.dart
    );
  }
}

