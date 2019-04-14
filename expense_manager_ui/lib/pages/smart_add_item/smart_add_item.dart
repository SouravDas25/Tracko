import 'package:expense_manager/Utils/CommonUtil.dart';
import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/Utils/enums.dart';
import 'package:expense_manager/component/AccountDialog.dart';
import 'package:expense_manager/component/CategoryDialog.dart';
import 'package:expense_manager/component/screen.dart';
import 'package:expense_manager/component/select_contact.dart';
import 'package:expense_manager/models/account.dart';
import 'package:expense_manager/models/category.dart';
import 'package:expense_manager/models/transaction.dart';
import 'package:flushbar/flushbar.dart';
import 'package:flutter/material.dart';
import 'package:datetime_picker_formfield/datetime_picker_formfield.dart';
import 'package:intl/intl.dart';

class SmartAddItemPage extends StatefulWidget {
  Transaction transaction;
  Function saveCallback;
  String mainButtonText;

  SmartAddItemPage(this.transaction, {this.saveCallback, this.mainButtonText});

  @override
  State<StatefulWidget> createState() {
    return _SmartAddItemPage(this.transaction);
  }
}

class _SmartAddItemPage extends State<SmartAddItemPage> {
  TextEditingController comments = TextEditingController();
  TextEditingController amount = TextEditingController();
  TextEditingController name = TextEditingController();

  bool isEdit = false;

  String logo;

  DateTime date = DateTime.now();
  int categoryId;
  int accountId;
  int transactionType;

  List<Category> categories = new List<Category>(0);

  List<Account> accounts = new List<Account>(0);

  _SmartAddItemPage(Transaction transaction) {
    this.date = transaction.date;
    this.amount.text = transaction.amount.toString();
    this.comments.text = transaction.comments;
    this.categoryId = transaction.categoryId;
    this.accountId = transaction.accountId;
    this.logo = transaction.logo;
    this.name.text = transaction.name;
    this.transactionType = transaction.transactionType;
//    print(possibleTransaction.category);
    name.addListener(onNameChange);
    initData();
  }

  initData() async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    CategoryBean categoryBean = CategoryBean(adapter);
    categories = await categoryBean.getAll();
    AccountBean accountBean = AccountBean(adapter);
    accounts = await accountBean.getAll();
    if (accountId == null) accountId = accounts[0].id;

//    await adapter.close();
    Future<void>.delayed(new Duration(seconds: 1));
    setState(() {});
  }

  save() async {
    if (categoryId == null || accountId == null) {
      Flushbar(
        titleText: Text(
          "Error",
          style: TextStyle(color: Colors.deepOrange),
        ),
        message: "Category and Account has to be specified",
        duration: Duration(seconds: 3),
      )..show(context);
      return;
    }
    Transaction transaction = widget.transaction;
    transaction.amount = double.parse(amount.text);
    transaction.date = date;
    transaction.name = name.text;
    transaction.categoryId = categoryId;
    transaction.accountId = this.accountId;
    transaction.comments = comments.text;
    transaction.logo = logo;
    transaction.transactionType = transactionType;
    if (widget.saveCallback != null) {
      widget.saveCallback(transaction);
    }
    Navigator.of(context).pop();
  }

  void onRadioChange(int val) {
    setState(() {
      transactionType = val;
    });
  }

  void onNameChange() {
    this.logo = CommonUtil.toImageUrl(name.text);
  }

  @override
  Widget build(BuildContext context) {
    return Screen(
      body: ListView(
        children: <Widget>[
          Row(
            children: <Widget>[
              CircleAvatar(
                minRadius: 30,
                backgroundColor: Colors.transparent,
                backgroundImage: NetworkImage(this.logo),
              ),
              Flexible(
                child: Padding(
                  padding: const EdgeInsets.all(18.0),
                  child: TextField(
                    style: TextStyle(fontSize: 25, fontWeight: FontWeight.bold),
                    controller: name,
                  ),
                ),
              ),
            ],
          ),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              new Radio<int>(
                value: TransactionType.CREDIT,
                groupValue: transactionType,
                onChanged: onRadioChange,
              ),
              new Text(
                'Income',
                style: new TextStyle(fontSize: 16.0),
              ),
              new Radio<int>(
                value: TransactionType.DEBIT,
                groupValue: transactionType,
                onChanged: onRadioChange,
              ),
              new Text(
                'Expense',
                style: new TextStyle(fontSize: 16.0),
              ),
            ],
          ),
          new DropdownButton<int>(
            style: TextStyle(
                fontSize: 18, fontWeight: FontWeight.bold, color: Colors.black),
            isExpanded: true,
            value: categoryId,
            hint: Text("Please choose a Category"),
            items: categories.map((Category value) {
              return new DropdownMenuItem<int>(
                value: value.id,
                child: new Text(value.name),
              );
            }).toList(),
            onChanged: (int id) {
              setState(() {
                this.categoryId = id;
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
            style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            decoration: new InputDecoration(
                labelText: 'Amount', hasFloatingPlaceholder: false),
            controller: amount,
          ),
          DateTimePickerFormField(
            initialValue: date,
            inputType: InputType.date,
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            format: DateFormat('dd-MMM-yyyy'),
            editable: false,
            decoration: InputDecoration(
                labelText: 'Date', hasFloatingPlaceholder: false),
            onChanged: (date) {
              this.date = date;
            },
          ),
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 8.0),
            child: new DropdownButton<int>(
              isExpanded: true,
              value: accountId,
              style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: Colors.black),
              hint: Text("Please choose a Account"),
              items: accounts.map((Account value) {
                return new DropdownMenuItem<int>(
                  value: value.id,
                  child: new Text(value.name),
                );
              }).toList(),
              onChanged: (int value) {
                setState(() {
                  this.accountId = value;
                });
              },
            ),
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
            maxLines: 5,
            decoration: new InputDecoration(
              labelStyle: TextStyle(fontSize: 19),
              labelText: 'comments',
            ),
          ),
          RaisedButton(
            color: Theme.of(context).primaryColor,
            padding: EdgeInsets.all(20.0),
            onPressed: () {
              this.save();
            },
            textColor: Colors.white,
            child: Text(
              widget.mainButtonText == null ? "Update" : widget.mainButtonText,
              style: TextStyle(fontSize: 18.0),
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(8.0),
            child: RaisedButton(
              color: Theme.of(context).primaryColor,
              padding: EdgeInsets.all(20.0),
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(builder: (context) => select_contact()),
                );
              },
              textColor: Colors.white,
              child: Text("Split"),
            ),
          ),
        ],
      ),
    );
  }
}
