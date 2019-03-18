import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/component/AccountDialog.dart';
import 'package:expense_manager/component/CategoryDialog.dart';
import 'package:expense_manager/component/PaddedText.dart';
import 'package:expense_manager/component/screen.dart';
import 'package:expense_manager/models/PossibleTransaction.dart';
import 'package:expense_manager/models/account.dart';
import 'package:expense_manager/models/category.dart';
import 'package:expense_manager/models/transaction.dart';
import 'package:expense_manager/models/user.dart';
import 'package:flushbar/flushbar.dart';
import 'package:flutter/material.dart';
import 'package:datetime_picker_formfield/datetime_picker_formfield.dart';
import 'package:intl/intl.dart';

class SmartAddItemPage extends StatefulWidget {

  PossibleTransaction possibleTransaction;

  SmartAddItemPage(this.possibleTransaction);

  @override
  State<StatefulWidget> createState() {
    // TODO: implement createState
    return _SmartAddItemPage(this.possibleTransaction);
  }
}

class _SmartAddItemPage extends State<SmartAddItemPage> {
  TextEditingController comments = TextEditingController();
  TextEditingController amount = TextEditingController();
  bool isEdit = false;
  Transaction current;

  DateTime date = DateTime.now();
  Category category;
  Account account;

  List<Category> categories = new List<Category>(0);

  List<Account> accounts = new List<Account>(0);

  _SmartAddItemPage(PossibleTransaction possibleTransaction){
    this.date = possibleTransaction.dates[0];
    this.amount.text = possibleTransaction.amounts[0].toString();
    this.comments.text = possibleTransaction.comments.join(" ");
    initData();
  }

  initData() async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    CategoryBean categoryBean = CategoryBean(adapter);
    categories = await categoryBean.getAll();
    AccountBean accountBean = AccountBean(adapter);
    accounts = await accountBean.getAll();
    account = accounts[0];

    await adapter.close();
    Future<void>.delayed(new Duration(seconds: 1));
    setState(() {});
  }

  save() async {
    if (category == null || account == null) {
      Flushbar(
        titleText : Text("Error",style: TextStyle(color: Colors.deepOrange),),
        message: "Category and Account has to be specified",
        duration: Duration(seconds: 3),
      )..show(context);
      return;
    }
    widget.possibleTransaction.comments.clear();
    widget.possibleTransaction.comments.add(comments.text);

    widget.possibleTransaction.amounts[0] = double.parse(amount.text);
    widget.possibleTransaction.dates[0] = date;
    widget.possibleTransaction.category = category;
    widget.possibleTransaction.account = account;
    Navigator.of(context).pop();
  }

  @override
  Widget build(BuildContext context) {
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
                            initData();
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
                              initData();
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
          RaisedButton(
            color: Theme.of(context).primaryColor,
            padding: EdgeInsets.all(20.0),
            onPressed: () {
              this.save();
            },
            textColor: Colors.white,
            child: Text("Save"),
          )
        ],
      ),
    );
  }
}
