import 'package:tracko/Utils/ConstantUtil.dart';
import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/Utils/enums.dart';
import 'package:tracko/component/AccountDialog.dart';
import 'package:tracko/component/CategoryDialog.dart';
import 'package:tracko/component/FLushDialog.dart';
import 'package:tracko/component/LoadingDialog.dart';
import 'package:tracko/component/select_backend_contact.dart';
import 'package:tracko/controllers/AccountController.dart';
import 'package:tracko/controllers/CategoryController.dart';
import 'package:tracko/controllers/TransactionController.dart';
import 'package:tracko/controllers/UserController.dart';
import 'package:tracko/dtos/TrackoContact.dart';
import 'package:tracko/models/account.dart';
import 'package:tracko/models/category.dart';
import 'package:tracko/models/transaction.dart';
import 'package:tracko/models/user.dart';
import 'package:tracko/models/user_currency.dart';
import 'package:tracko/pages/smart_add_item/SplitSectionInAddTransaction.dart';
import 'package:tracko/services/SessionService.dart';
import 'package:datetime_picker_formfield/datetime_picker_formfield.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_masked_text2/flutter_masked_text2.dart';
import 'package:intl/intl.dart';

class SmartAddItemPage extends StatefulWidget {
  final Transaction transaction;
  final Function saveCallback;
  final String mainButtonText;
  final Function complete;

  SmartAddItemPage(this.transaction,
      {required this.saveCallback,
      required this.mainButtonText,
      required this.complete});

  @override
  State<StatefulWidget> createState() {
    return _SmartAddItemPage(this.transaction);
  }
}

class _SmartAddItemPage extends State<SmartAddItemPage> {
  TextEditingController comments = TextEditingController();
  TextEditingController amount = MoneyMaskedTextController(
      decimalSeparator: '.', thousandSeparator: ',', initialValue: 0.00);
  TextEditingController exchangeRateController =
      TextEditingController(text: '1.0');
  List<TextEditingController> splitAmountTextEditionControllers = [];
  TextEditingController name = TextEditingController();
  bool isEdit = false;

  DateTime date = DateTime.now();
  int categoryId = 0;
  int accountId = 0;
  int transferFromAccountId = 0;
  int transferToAccountId = 0;
  int transactionType = 0;

  String baseCurrency = 'INR';
  String selectedCurrency = 'INR';
  double convertedAmount = 0.0;

  List<String> availableCurrencies = ['INR'];
  Map<String, double> currencyRates = {};

  List<User> frequentSplitters = [];
  Set<TrakoContact> splitList = Set();
  List<Category> categories = [];
  List<Account> accounts = [];

  _SmartAddItemPage(Transaction transaction) {
    this.date = transaction.date;

    // Check if it's a foreign currency transaction
    if (transaction.originalCurrency != null &&
        transaction.originalCurrency!.isNotEmpty) {
      this.selectedCurrency = transaction.originalCurrency!;
      this.amount.text = (transaction.originalAmount ?? 0.0).toStringAsFixed(2);
      this.exchangeRateController.text =
          (transaction.exchangeRate ?? 1.0).toString();
    } else {
      if (transaction.amount == 0.0) {
        this.amount.text = 0.toString();
      } else {
        this.amount.text = transaction.amount.toStringAsFixed(2);
      }
    }

    this.comments.text = transaction.comments;
    this.categoryId = transaction.categoryId;
    this.accountId = transaction.accountId;
    this.transferFromAccountId =
        transaction.transferFromAccountId ?? transaction.accountId;
    this.transferToAccountId =
        transaction.transferToAccountId ?? transaction.accountId;
    this.name.text = transaction.name;
    this.transactionType = transaction.transactionType;
    if (transaction.contacts != null) splitList.addAll(transaction.contacts);
//    print(transaction.contacts.length);
    splitList.forEach((contact) =>
        splitAmountTextEditionControllers.add(TextEditingController()));
//    print(splitAmountTextEditionControllers);
    name.addListener(onNameChange);
    amount.addListener(updateCalculatedAmount);
    exchangeRateController.addListener(updateCalculatedAmount);
  }

  @override
  void initState() {
    super.initState();
    initData();
  }

  initData() async {
    try {
      final user = await SessionService.getCurrentUser();
      baseCurrency = user.baseCurrency.isNotEmpty ? user.baseCurrency : 'INR';

      availableCurrencies = [baseCurrency];
      currencyRates = {};

      if (user.secondaryCurrencies.isNotEmpty) {
        for (var uc in user.secondaryCurrencies) {
          if (!availableCurrencies.contains(uc.currencyCode)) {
            availableCurrencies.add(uc.currencyCode);
          }
          currencyRates[uc.currencyCode] = uc.exchangeRate;
        }
      }

      // Ensure transaction's original currency is available
      if (widget.transaction.originalCurrency != null &&
          widget.transaction.originalCurrency!.isNotEmpty) {
        if (!availableCurrencies
            .contains(widget.transaction.originalCurrency!)) {
          availableCurrencies.add(widget.transaction.originalCurrency!);
        }
      }

      if (widget.transaction.id == null &&
          widget.transaction.originalCurrency == null) {
        selectedCurrency = baseCurrency;
        exchangeRateController.text = '1.0';
      } else if (widget.transaction.originalCurrency != null &&
          availableCurrencies.contains(widget.transaction.originalCurrency)) {
        selectedCurrency = widget.transaction.originalCurrency!;
      }
    } catch (e) {
      print("Error initializing data: $e");
    }

    categories = await CategoryController.getAllCategories();
    accounts = await AccountController.getAllAccounts();
    if (accountId == 0) {
      accountId = accounts.isNotEmpty ? (accounts[0].id ?? 0) : 0;
    }
    if (transferFromAccountId == 0) {
      transferFromAccountId = accounts.isNotEmpty ? (accounts[0].id ?? 0) : 0;
    }
    if (transferToAccountId == 0) {
      transferToAccountId = accounts.isNotEmpty ? (accounts[0].id ?? 0) : 0;
    }

    splitList = await TransactionController.loadSplits(widget.transaction);
    if (splitList.length <= 0 && transactionType == TransactionType.DEBIT) {
      frequentSplitters = await UserController.getFrequentSplitters();
      TrakoContact userContact = SessionService.currentUserContact();
      splitList.add(userContact);
      splitAmountTextEditionControllers.add(TextEditingController());
    }

    print(frequentSplitters);
    updateCalculatedAmount();
    setState(() {});
  }

  void updateCalculatedAmount() {
    double amt = castAmountText2Double(amount.text);
    double rate = double.tryParse(exchangeRateController.text) ?? 1.0;
    setState(() {
      convertedAmount = amt * rate;
    });
  }

  save() async {
    bool isSuccessfulSave = false;
    LoadingDialog.show(context);
    try {
      double inputAmount = castAmountText2Double(amount.text);
      if (inputAmount <= 0) {
        throw Exception("Amount should be non-zero and non-negative");
      }
      if (transactionType == TransactionType.TRANSFER) {
        if (transferFromAccountId == 0 || transferToAccountId == 0) {
          throw Exception("From Account and To Account has to be specified");
        }
        if (transferFromAccountId == transferToAccountId) {
          throw Exception("From Account and To Account cannot be same");
        }
      } else {
        if (categoryId == 0 || accountId == 0) {
          throw Exception("Category and Account has to be specified");
        }
      }
      Transaction transaction = widget.transaction;

      // Handle Currency Logic
      if (selectedCurrency != baseCurrency) {
        transaction.originalCurrency = selectedCurrency;
        transaction.originalAmount = inputAmount;
        transaction.exchangeRate =
            double.tryParse(exchangeRateController.text) ?? 1.0;
        transaction.amount =
            transaction.originalAmount! * transaction.exchangeRate!;
      } else {
        transaction.amount = inputAmount;
        transaction.originalCurrency = null;
        transaction.originalAmount = null;
        transaction.exchangeRate = null;
      }

      transaction.date = date;
      transaction.name = name.text.trim();
      if (transactionType == TransactionType.TRANSFER) {
        transaction.transferFromAccountId = transferFromAccountId;
        transaction.transferToAccountId = transferToAccountId;
        transaction.accountId = transferFromAccountId;
      } else {
        transaction.categoryId = categoryId;
        transaction.accountId = this.accountId;
      }
      transaction.comments = comments.text;
      transaction.transactionType = transactionType;
      if (splitList.length > 0) {
        transaction.contacts.clear();
        transaction.contacts.addAll(splitList);
      }
      await widget.saveCallback(transaction);
      isSuccessfulSave = true;
    } catch (exception) {
      await FlushDialog.flash(context, "Error", exception.toString());
      rethrow;
    } finally {
      await LoadingDialog.hide(context);
    }
    if (isSuccessfulSave) {
      Navigator.of(context).pop(isSuccessfulSave);
      await widget.complete(widget.transaction);
    }
  }

  double castAmountText2Double(String text) {
    text = text.replaceAll(",", "");
    return double.parse(text);
  }

  void syncSplit(TrakoContact contact) {
    if (!splitList.contains(contact)) {
      splitList.add(contact);
      splitAmountTextEditionControllers.add(TextEditingController());
    }
  }

  void addSplit(User user) {
    frequentSplitters.remove(user);
    TrakoContact contact = UserController.user2Contact(user);
    syncSplit(contact);
    setState(() {});
  }

  void onRadioChange(int? val) {
    if (val == TransactionType.CREDIT || val == TransactionType.TRANSFER) {
      splitList.clear();
    }
    setState(() {
      transactionType = val ?? 0;
    });
  }

  void onNameChange() async {
    await CategoryController.findById(categoryId);
  }

  void callSplitPage() async {
    List<TrakoContact> result = await Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => SelectBackendContactPage()),
    );
//    print(result);
    if (result != null) {
      frequentSplitters.clear();
      splitList.clear();
      for (TrakoContact contact in result) {
        syncSplit(contact);
      }
      setState(() {});
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: transactionType == TransactionType.DEBIT
            ? Colors.red
            : (transactionType == TransactionType.TRANSFER
                ? Colors.grey
                : Colors.lightGreen),
        actions: transactionType == TransactionType.DEBIT
            ? <Widget>[
                IconButton(
                  icon: Icon(
                    Icons.call_split,
                    size: 30.0,
                  ),
                  onPressed: callSplitPage,
                )
              ]
            : [],
        title: Text(isEdit ? "Update Transaction" : "New Transaction"),
        centerTitle: true,
      ),
      body: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16.0),
        child: ListView(
          children: <Widget>[
            Row(
              children: <Widget>[
                WidgetUtil.textAvatar(name.text),
                Flexible(
                  child: Padding(
                    padding: const EdgeInsets.all(18.0),
                    child: TextField(
                      style:
                          TextStyle(fontSize: 25, fontWeight: FontWeight.bold),
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
                new Radio<int>(
                  value: TransactionType.TRANSFER,
                  groupValue: transactionType,
                  onChanged: onRadioChange,
                ),
                new Text(
                  'Transfer',
                  style: new TextStyle(fontSize: 16.0),
                ),
              ],
            ),
            if (transactionType != TransactionType.TRANSFER)
              DropdownButton<int>(
                style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: Colors.black),
                isExpanded: true,
                value: categoryId,
                hint: Text("Please choose a Category"),
                items: categories.map((Category value) {
                  return new DropdownMenuItem<int>(
                    value: value.id,
                    child: Row(
                      children: <Widget>[
                        Padding(
                          padding: const EdgeInsets.all(10.0),
                          child: WidgetUtil.textAvatar(value.name,
                              backgroundColor: Colors.green),
                        ),
                        new Text(value.name),
                      ],
                    ),
                  );
                }).toList(),
                onChanged: (int? id) {
                  setState(() {
                    this.categoryId = id ?? 0;
                  });
                },
              ),
            SizedBox(
              width: double.infinity,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: <Widget>[
                  TextButton(
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
                      "+ Category",
                      textAlign: TextAlign.right,
                    ),
                  )
                ],
              ),
            ),
            Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Expanded(
                  flex: 2,
                  child: DropdownButtonFormField<String>(
                    value: selectedCurrency,
                    decoration: InputDecoration(
                      labelText: 'Currency',
                      contentPadding: EdgeInsets.only(bottom: 12, top: 12),
                    ),
                    items: availableCurrencies.map((String value) {
                      return DropdownMenuItem<String>(
                        value: value,
                        child: Text(value),
                      );
                    }).toList(),
                    onChanged: (newValue) {
                      setState(() {
                        selectedCurrency = newValue!;
                        if (selectedCurrency == baseCurrency) {
                          exchangeRateController.text = '1.0';
                        } else if (currencyRates
                            .containsKey(selectedCurrency)) {
                          exchangeRateController.text =
                              currencyRates[selectedCurrency].toString();
                        }
                        updateCalculatedAmount();
                      });
                    },
                  ),
                ),
                SizedBox(width: 10),
                Expanded(
                  flex: 4,
                  child: TextField(
                    keyboardType:
                        TextInputType.numberWithOptions(decimal: true),
                    style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                    decoration: new InputDecoration(
                        prefixText: '', // Removed fixed symbol
                        labelText: 'Amount',
                        floatingLabelBehavior: FloatingLabelBehavior.never),
                    controller: amount,
                  ),
                ),
              ],
            ),

            if (selectedCurrency != baseCurrency) ...[
              SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: exchangeRateController,
                      keyboardType:
                          TextInputType.numberWithOptions(decimal: true),
                      decoration: InputDecoration(
                        labelText: 'Exchange Rate',
                        helperText: '1 $selectedCurrency = ? $baseCurrency',
                      ),
                    ),
                  ),
                  SizedBox(width: 10),
                  Expanded(
                    child: InputDecorator(
                      decoration: InputDecoration(
                        labelText: 'Converted ($baseCurrency)',
                        border: InputBorder.none,
                      ),
                      child: Text(
                        convertedAmount.toStringAsFixed(2),
                        style: TextStyle(
                            fontSize: 18, fontWeight: FontWeight.bold),
                      ),
                    ),
                  ),
                ],
              ),
            ],

            DateTimeField(
              initialValue: date,
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              format: DateFormat('dd-MMM-yyyy'),
              readOnly: true,
              decoration: InputDecoration(
                  labelText: 'Date',
                  floatingLabelBehavior: FloatingLabelBehavior.never),
              onShowPicker: (context, currentValue) {
                return showDatePicker(
                    context: context,
                    firstDate: DateTime(1900),
                    initialDate: currentValue ?? DateTime.now(),
                    lastDate: DateTime(2100));
              },
              onChanged: (date) {
                this.date = date ?? DateTime.now();
              },
            ),
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8.0),
              child: transactionType == TransactionType.TRANSFER
                  ? Column(
                      children: [
                        Align(
                          alignment: Alignment.centerLeft,
                          child: Padding(
                            padding: const EdgeInsets.only(bottom: 6.0),
                            child: Text("From Account"),
                          ),
                        ),
                        new DropdownButton<int>(
                          isExpanded: true,
                          value: transferFromAccountId,
                          style: TextStyle(
                              fontSize: 18,
                              fontWeight: FontWeight.bold,
                              color: Colors.black),
                          hint: Text("Please choose From Account"),
                          items: accounts.map((Account value) {
                            return new DropdownMenuItem<int>(
                              value: value.id,
                              child: new Text(value.name),
                            );
                          }).toList(),
                          onChanged: (int? value) {
                            setState(() {
                              transferFromAccountId = value ?? 0;
                              if (transferToAccountId == 0) {
                                transferToAccountId = value ?? 0;
                              }
                            });
                          },
                        ),
                        SizedBox(height: 12),
                        Align(
                          alignment: Alignment.centerLeft,
                          child: Padding(
                            padding: const EdgeInsets.only(bottom: 6.0),
                            child: Text("To Account"),
                          ),
                        ),
                        new DropdownButton<int>(
                          isExpanded: true,
                          value: transferToAccountId,
                          style: TextStyle(
                              fontSize: 18,
                              fontWeight: FontWeight.bold,
                              color: Colors.black),
                          hint: Text("Please choose To Account"),
                          items: accounts.map((Account value) {
                            return new DropdownMenuItem<int>(
                              value: value.id,
                              child: new Text(value.name),
                            );
                          }).toList(),
                          onChanged: (int? value) {
                            setState(() {
                              transferToAccountId = value ?? 0;
                            });
                          },
                        ),
                      ],
                    )
                  : new DropdownButton<int>(
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
                      onChanged: (int? value) {
                        setState(() {
                          this.accountId = value ?? 0;
                        });
                      },
                    ),
            ),
            SizedBox(
                width: double.infinity,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: <Widget>[
                    if (transactionType != TransactionType.TRANSFER)
                      TextButton(
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
                          "+ Account",
                          textAlign: TextAlign.right,
                        ),
                      )
                  ],
                )),
            if (transactionType != TransactionType.TRANSFER) ...[
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 8.0),
                child: Text("Splits"),
              ),
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: Row(
                    children: frequentSplitters
                        .map((User user) => ActionChip(
                              avatar: WidgetUtil.textAvatar(user.name),
                              onPressed: () {
                                addSplit(user);
                              },
                              label: Text(user.name),
                            ))
                        .toList()),
              ),
              Container(
                child: SplitSectionInAddTransaction(
                  parentState: this,
                  amount: castAmountText2Double(amount.text),
                  currencySymbol:
                      ConstantUtil.getCurrencySymbol(selectedCurrency),
                  splitList: splitList,
                  textEditingControllers: splitAmountTextEditionControllers,
                ),
              ),
            ],
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8.0),
              child: TextField(
                controller: comments,
                maxLines: 5,
                decoration: new InputDecoration(
                    labelStyle: TextStyle(fontSize: 19),
                    labelText: 'comments',
                    border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(10.0))),
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8.0),
              child: ElevatedButton(
                style: ElevatedButton.styleFrom(
                  backgroundColor: Theme.of(context).primaryColor,
                  padding: EdgeInsets.all(20.0),
                  foregroundColor: Colors.white,
                ),
                onPressed: () {
                  this.save();
                },
                child: Text(
                  widget.mainButtonText == null
                      ? "Update"
                      : widget.mainButtonText,
                  style: TextStyle(fontSize: 18.0),
                ),
              ),
            ),
//          Padding(
//            padding: const EdgeInsets.only(top:8.0),
//            child: ElevatedButton(
//              color: Theme.of(context).primaryColor,
//              padding: EdgeInsets.all(20.0),
//              onPressed: () {
//
//              },
//              textColor: Colors.white,
//              child: Text("Split"),
//            ),
//          ),
          ],
        ),
      ),
    );
  }
}
