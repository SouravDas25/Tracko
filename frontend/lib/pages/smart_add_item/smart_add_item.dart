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
import 'package:tracko/models/contact.dart';
import 'package:tracko/models/transaction.dart';
import 'package:tracko/models/user.dart';
import 'package:tracko/models/user_currency.dart';
import 'package:tracko/pages/smart_add_item/SplitSectionInAddTransaction.dart';
import 'package:tracko/repositories/contact_repository.dart';
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
  List<Contact> myContacts = [];
  final _contactRepo = ContactRepository();

  List<Category> _filteredCategoriesForCurrentType() {
    if (transactionType == TransactionType.CREDIT) {
      return categories
          .where((c) => (c.categoryType).toUpperCase() == 'INCOME')
          .toList();
    }
    if (transactionType == TransactionType.DEBIT) {
      return categories
          .where((c) => (c.categoryType).toUpperCase() == 'EXPENSE')
          .toList();
    }
    return categories;
  }

  void _ensureValidCategorySelection() {
    final filtered = _filteredCategoriesForCurrentType();
    final ids = filtered.map((c) => c.id).whereType<int>().toSet();
    if (categoryId != 0 && ids.contains(categoryId)) return;
    categoryId = 0;
  }

  String _normalizePhone(String? phone) {
    if (phone == null) return '';
    final digitsOnly = phone.replaceAll(RegExp(r'[^0-9]'), '');
    if (digitsOnly.length <= 10) return digitsOnly;
    // Compare by last 10 digits to handle country codes
    return digitsOnly.substring(digitsOnly.length - 10);
  }

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

    try {
      categories = await CategoryController.getAllCategories();
      accounts = await AccountController.getAllAccounts();
      myContacts = await _contactRepo.listMine();

      _ensureValidCategorySelection();

      // Do not auto-select account/category; require explicit user selection.
      // For transfers, the user must explicitly pick both accounts.

      final loadedContacts =
          await TransactionController.loadSplits(widget.transaction);
      splitList = Set<TrakoContact>.from(loadedContacts);
      if (splitList.length <= 0 && transactionType == TransactionType.DEBIT) {
        frequentSplitters = await UserController.getFrequentSplitters();
        TrakoContact userContact = SessionService.currentUserContact();
        splitList.add(userContact);
        splitAmountTextEditionControllers.add(TextEditingController());
      }
    } catch (e) {
      print("Error loading external data: $e");
    }

    updateCalculatedAmount();
    setState(() {});
  }

  void updateCalculatedAmount() {
    if (selectedCurrency != baseCurrency) {
      double amt = castAmountText2Double(amount.text);
      // Since backend will fetch the rate, we cannot preview exact conversion.
      // We'll show a placeholder or fetch user's configured rate for preview.
      // For now, keep existing behavior using the field value as a preview.
      double rate = double.tryParse(exchangeRateController.text) ?? 1.0;
      convertedAmount = amt * rate;
    } else {
      convertedAmount = castAmountText2Double(amount.text);
    }
    setState(() {});
  }

  save() async {
    bool isSuccessfulSave = false;
    LoadingDialog.show(context);
    try {
      double inputAmount = castAmountText2Double(amount.text);
      if (inputAmount <= 0) {
        throw Exception("Amount should be non-zero and non-negative");
      }

      if (name.text.trim().isEmpty) {
        throw Exception("Description has to be specified");
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
        // Let backend fetch exchangeRate; omit exchangeRate and amount
        // amount is non-nullable, so we don't set it; repository will omit if null
        transaction.exchangeRate = null as double?;
      } else {
        transaction.amount = inputAmount;
        transaction.originalCurrency = null;
        transaction.originalAmount = null as double?;
        transaction.exchangeRate = null as double?;
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

      // Sync contacts from UI selection
      transaction.contacts.clear();
      if (splitList.isNotEmpty) {
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

  Future<void> addSplit(User user) async {
    frequentSplitters.remove(user);
    TrakoContact contact = UserController.user2Contact(user);

    if (myContacts.isEmpty) {
      try {
        myContacts = await _contactRepo.listMine();
      } catch (_) {
        // ignore
      }
    }

    // Attempt to match with existing contact to get ID
    try {
      final userPhone = _normalizePhone(user.phoneNo);
      if (userPhone.isNotEmpty) {
        final match = myContacts.firstWhere(
            (c) => _normalizePhone(c.phoneNo) == userPhone,
            orElse: () => Contact());
        if (match.id != null) {
          contact.contactId = match.id;
        }
      }
    } catch (e) {
      // ignore
    }

    syncSplit(contact);
    setState(() {});
  }

  void onRadioChange(int? val) {
    if (val == TransactionType.CREDIT || val == TransactionType.TRANSFER) {
      splitList.clear();
    }
    setState(() {
      transactionType = val ?? 0;

      _ensureValidCategorySelection();
    });
  }

  void callSplitPage() async {
    List<TrakoContact>? result = await Navigator.push(
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
            ? Colors.redAccent
            : (transactionType == TransactionType.TRANSFER
                ? Colors.blueGrey
                : Colors.teal),
        actions: transactionType == TransactionType.DEBIT
            ? <Widget>[
                IconButton(
                  icon: Icon(
                    Icons.call_split,
                    size: 28.0,
                  ),
                  onPressed: callSplitPage,
                  tooltip: "Split Transaction",
                )
              ]
            : [],
        title: Text(isEdit ? "Edit Transaction" : "New Transaction"),
        centerTitle: true,
        elevation: 0,
      ),
      body: SingleChildScrollView(
        child: Column(
          children: <Widget>[
            _buildTypeSelector(),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  _buildAmountSection(),
                  SizedBox(height: 16),
                  _buildDetailsSection(),
                  if (transactionType == TransactionType.DEBIT) ...[
                    SizedBox(height: 16),
                    _buildSplitsSection(),
                  ],
                  SizedBox(height: 16),
                  _buildCommentsSection(),
                  SizedBox(height: 24),
                  ElevatedButton(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: transactionType == TransactionType.DEBIT
                          ? Colors.redAccent
                          : (transactionType == TransactionType.TRANSFER
                              ? Colors.blueGrey
                              : Colors.teal),
                      padding: EdgeInsets.symmetric(vertical: 16),
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                      elevation: 4,
                    ),
                    onPressed: () {
                      this.save();
                    },
                    child: Text(
                      widget.mainButtonText != null
                          ? widget.mainButtonText
                          : (isEdit ? "Update" : "Save"),
                      style: TextStyle(
                          fontSize: 18.0, fontWeight: FontWeight.bold),
                    ),
                  ),
                  SizedBox(height: 40),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTypeSelector() {
    return Container(
      color: Theme.of(context).cardColor,
      padding: EdgeInsets.symmetric(vertical: 16, horizontal: 16),
      margin: EdgeInsets.only(bottom: 16),
      child: Container(
        height: 48,
        decoration: BoxDecoration(
          color: Theme.of(context).brightness == Brightness.dark
              ? Colors.black26
              : Colors.grey.shade200,
          borderRadius: BorderRadius.circular(24),
        ),
        padding: EdgeInsets.all(4),
        child: Row(
          children: [
            Expanded(
              child: _buildTypeButton(
                  TransactionType.CREDIT, "Income", Colors.teal),
            ),
            Expanded(
              child: _buildTypeButton(
                  TransactionType.DEBIT, "Expense", Colors.redAccent),
            ),
            Expanded(
              child: _buildTypeButton(
                  TransactionType.TRANSFER, "Transfer", Colors.blueGrey),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTypeButton(int type, String label, Color color) {
    bool isSelected = transactionType == type;
    return GestureDetector(
      onTap: () => onRadioChange(type),
      child: AnimatedContainer(
        duration: Duration(milliseconds: 200),
        decoration: BoxDecoration(
          color: isSelected ? color : Colors.transparent,
          borderRadius: BorderRadius.circular(20),
          boxShadow: isSelected
              ? [
                  BoxShadow(
                    color: color.withOpacity(0.3),
                    blurRadius: 8,
                    offset: Offset(0, 2),
                  )
                ]
              : [],
        ),
        alignment: Alignment.center,
        child: Text(
          label,
          style: TextStyle(
            color: isSelected
                ? Colors.white
                : Theme.of(context).hintColor.withOpacity(0.7),
            fontWeight: FontWeight.bold,
            fontSize: 14,
          ),
        ),
      ),
    );
  }

  Widget _buildAmountSection() {
    bool isDarkMode = Theme.of(context).brightness == Brightness.dark;
    Color typeColor;
    if (transactionType == TransactionType.DEBIT) {
      typeColor = isDarkMode ? Colors.redAccent : Colors.red;
    } else if (transactionType == TransactionType.TRANSFER) {
      typeColor = isDarkMode ? Colors.lightBlueAccent : Colors.blueGrey;
    } else {
      typeColor = isDarkMode ? Colors.tealAccent : Colors.teal;
    }

    return Card(
      elevation: 0,
      color: Theme.of(context).cardColor,
      shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
          side: BorderSide(
              color: Theme.of(context).dividerColor.withOpacity(0.1))),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 24.0, horizontal: 20.0),
        child: Column(
          children: [
            Text(
              "Amount",
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
                color: Theme.of(context).hintColor,
                letterSpacing: 0.5,
              ),
            ),
            SizedBox(height: 12),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.baseline,
              textBaseline: TextBaseline.alphabetic,
              children: [
                DropdownButton<String>(
                  value: availableCurrencies.contains(selectedCurrency)
                      ? selectedCurrency
                      : (availableCurrencies.isNotEmpty
                          ? availableCurrencies.first
                          : null),
                  underline: SizedBox(),
                  icon: Icon(Icons.arrow_drop_down, size: 20, color: typeColor),
                  style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                      color: typeColor),
                  items: availableCurrencies.map((String value) {
                    return DropdownMenuItem<String>(
                      value: value,
                      child: Text(value),
                    );
                  }).toList(),
                  onChanged: (newValue) {
                    if (newValue == null) return;
                    setState(() {
                      selectedCurrency = newValue;
                      if (selectedCurrency == baseCurrency) {
                        exchangeRateController.text = '1.0';
                      } else if (currencyRates.containsKey(selectedCurrency)) {
                        exchangeRateController.text =
                            currencyRates[selectedCurrency].toString();
                      }
                      updateCalculatedAmount();
                    });
                  },
                ),
                SizedBox(width: 8),
                IntrinsicWidth(
                  child: TextField(
                    controller: amount,
                    keyboardType:
                        TextInputType.numberWithOptions(decimal: true),
                    style: TextStyle(
                      fontSize: 40,
                      fontWeight: FontWeight.w700,
                      color: typeColor,
                      height: 1.0,
                    ),
                    decoration: InputDecoration(
                      hintText: "0.00",
                      border: InputBorder.none,
                      contentPadding: EdgeInsets.zero,
                      isDense: true,
                    ),
                    textAlign: TextAlign.center,
                  ),
                ),
              ],
            ),
            if (selectedCurrency != baseCurrency) ...[
              SizedBox(height: 20),
              Container(
                padding: EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Theme.of(context).scaffoldBackgroundColor,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Column(
                  children: [
                    Row(
                      children: [
                        Text("Exchange Rate:", style: TextStyle(fontSize: 12)),
                        SizedBox(width: 8),
                        Expanded(
                          child: TextField(
                            controller: exchangeRateController,
                            keyboardType:
                                TextInputType.numberWithOptions(decimal: true),
                            decoration: InputDecoration(
                              isDense: true,
                              border: InputBorder.none,
                              contentPadding: EdgeInsets.zero,
                            ),
                            style: TextStyle(
                                fontSize: 14, fontWeight: FontWeight.bold),
                            textAlign: TextAlign.end,
                          ),
                        ),
                      ],
                    ),
                    Divider(height: 16),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text("Converted:", style: TextStyle(fontSize: 12)),
                        Text(
                          "$baseCurrency ${convertedAmount.toStringAsFixed(2)}",
                          style: TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.bold,
                              color:
                                  Theme.of(context).textTheme.bodyLarge?.color),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildAccountDropdown(
      String label, int? value, Function(int?) onChanged) {
    final ids = accounts.map((a) => a.id).whereType<int>().toSet();
    final int? safeValue =
        (value != null && value != 0 && ids.contains(value)) ? value : null;
    return DropdownButtonFormField<int>(
      value: safeValue,
      decoration: InputDecoration(
        labelText: label,
        prefixIcon: Icon(Icons.account_balance_wallet_outlined),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide.none,
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(
              color: Theme.of(context).dividerColor.withOpacity(0.1)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide:
              BorderSide(color: Theme.of(context).primaryColor, width: 2),
        ),
        filled: true,
        fillColor: Theme.of(context).cardColor,
        contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 16),
      ),
      items: accounts.map((Account value) {
        return DropdownMenuItem<int>(
          value: value.id,
          child: Text(value.name),
        );
      }).toList(),
      onChanged: onChanged,
    );
  }

  Widget _buildDetailsSection() {
    return Column(
      children: [
        // Date Picker
        DateTimeField(
          initialValue: date,
          format: DateFormat('dd-MMM-yyyy'),
          readOnly: true,
          resetIcon: null,
          decoration: InputDecoration(
            labelText: 'Date',
            prefixIcon: Icon(Icons.calendar_today_outlined),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide: BorderSide.none,
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide: BorderSide(
                  color: Theme.of(context).dividerColor.withOpacity(0.1)),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide:
                  BorderSide(color: Theme.of(context).primaryColor, width: 2),
            ),
            filled: true,
            fillColor: Theme.of(context).cardColor,
            contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 16),
          ),
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
        SizedBox(height: 16),

        // Category (if not transfer)
        if (transactionType != TransactionType.TRANSFER) ...[
          Builder(
            builder: (context) {
              final filteredCategories = _filteredCategoriesForCurrentType();
              return Row(
                children: [
                  Expanded(
                    child: DropdownButtonFormField<int>(
                      value: filteredCategories
                              .map((c) => c.id)
                              .whereType<int>()
                              .contains(categoryId)
                          ? categoryId
                          : null,
                      decoration: InputDecoration(
                        labelText: 'Category',
                        prefixIcon: Icon(Icons.category_outlined),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(16),
                          borderSide: BorderSide.none,
                        ),
                        enabledBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(16),
                          borderSide: BorderSide(
                              color: Theme.of(context)
                                  .dividerColor
                                  .withOpacity(0.1)),
                        ),
                        focusedBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(16),
                          borderSide: BorderSide(
                              color: Theme.of(context).primaryColor, width: 2),
                        ),
                        filled: true,
                        fillColor: Theme.of(context).cardColor,
                        contentPadding:
                            EdgeInsets.symmetric(horizontal: 16, vertical: 16),
                      ),
                      items: filteredCategories.map((Category value) {
                        return DropdownMenuItem<int>(
                          value: value.id,
                          child: Text(
                            value.name,
                            overflow: TextOverflow.ellipsis,
                          ),
                        );
                      }).toList(),
                      onChanged: (int? id) {
                        setState(() {
                          this.categoryId = id ?? 0;
                        });
                      },
                    ),
                  ),
                  SizedBox(width: 12),
                  Ink(
                    decoration: BoxDecoration(
                      color: Theme.of(context).cardColor,
                      shape: BoxShape.circle,
                      border: Border.all(
                          color:
                              Theme.of(context).dividerColor.withOpacity(0.1)),
                    ),
                    child: InkWell(
                      customBorder: CircleBorder(),
                      onTap: () {
                        showDialog(
                          context: context,
                          builder: (_) => CategoryDialog(
                            callback: () {
                              setState(() {
                                initData();
                              });
                            },
                            categoryType:
                                transactionType == TransactionType.CREDIT
                                    ? 'INCOME'
                                    : 'EXPENSE',
                          ),
                        );
                      },
                      child: Container(
                        height: 48,
                        width: 48,
                        alignment: Alignment.center,
                        child: Icon(Icons.add,
                            color: Theme.of(context).colorScheme.onSurface),
                      ),
                    ),
                  )
                ],
              );
            },
          ),
          SizedBox(height: 16),
        ],

        // Accounts
        if (transactionType == TransactionType.TRANSFER) ...[
          _buildAccountDropdown("From Account", transferFromAccountId, (val) {
            setState(() {
              transferFromAccountId = val ?? 0;
              if (transferToAccountId == 0) transferToAccountId = val ?? 0;
            });
          }),
          SizedBox(height: 16),
          _buildAccountDropdown("To Account", transferToAccountId, (val) {
            setState(() {
              transferToAccountId = val ?? 0;
            });
          }),
        ] else ...[
          Row(
            children: [
              Expanded(
                child: _buildAccountDropdown("Account", accountId, (val) {
                  setState(() {
                    this.accountId = val ?? 0;
                  });
                }),
              ),
              SizedBox(width: 12),
              Ink(
                decoration: BoxDecoration(
                  color: Theme.of(context).cardColor,
                  shape: BoxShape.circle,
                  border: Border.all(
                      color: Theme.of(context).dividerColor.withOpacity(0.1)),
                ),
                child: InkWell(
                  customBorder: CircleBorder(),
                  onTap: () {
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
                  child: Container(
                    height: 48,
                    width: 48,
                    alignment: Alignment.center,
                    child: Icon(Icons.add,
                        color: Theme.of(context).colorScheme.onSurface),
                  ),
                ),
              )
            ],
          ),
        ],
        SizedBox(height: 16),

        // Name Input
        TextField(
          controller: name,
          decoration: InputDecoration(
            labelText: 'Description',
            prefixIcon: Icon(Icons.description_outlined),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide: BorderSide.none,
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide: BorderSide(
                  color: Theme.of(context).dividerColor.withOpacity(0.1)),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(16),
              borderSide:
                  BorderSide(color: Theme.of(context).primaryColor, width: 2),
            ),
            filled: true,
            fillColor: Theme.of(context).cardColor,
            contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 16),
          ),
        ),
      ],
    );
  }

  Widget _buildSplitsSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              "Splits",
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: Theme.of(context).hintColor,
              ),
            ),
            IconButton(
              icon:
                  Icon(Icons.call_split, color: Theme.of(context).primaryColor),
              onPressed: callSplitPage,
              tooltip: "Split Transaction",
            ),
          ],
        ),
        if (frequentSplitters.isNotEmpty) ...[
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: frequentSplitters
                  .map((User user) => Padding(
                        padding: const EdgeInsets.only(right: 8.0),
                        child: ActionChip(
                          avatar: WidgetUtil.textAvatar(user.name),
                          label: Text(user.name),
                          backgroundColor: Theme.of(context).cardColor,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(16),
                            side: BorderSide(
                                color: Theme.of(context)
                                    .dividerColor
                                    .withOpacity(0.1)),
                          ),
                          onPressed: () {
                            addSplit(user);
                          },
                        ),
                      ))
                  .toList(),
            ),
          ),
          SizedBox(height: 12),
        ],
        SplitSectionInAddTransaction(
          parentState: this,
          amount: castAmountText2Double(amount.text),
          currencySymbol: ConstantUtil.getCurrencySymbol(selectedCurrency),
          splitList: splitList,
          textEditingControllers: splitAmountTextEditionControllers,
        ),
      ],
    );
  }

  Widget _buildCommentsSection() {
    return TextField(
      controller: comments,
      maxLines: 3,
      decoration: InputDecoration(
        labelText: 'Comments',
        prefixIcon: Icon(Icons.comment_outlined),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide.none,
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide: BorderSide(
              color: Theme.of(context).dividerColor.withOpacity(0.1)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(16),
          borderSide:
              BorderSide(color: Theme.of(context).primaryColor, width: 2),
        ),
        filled: true,
        fillColor: Theme.of(context).cardColor,
        alignLabelWithHint: true,
        contentPadding: EdgeInsets.all(16),
      ),
    );
  }
}
