import 'package:flutter/material.dart';
import 'package:flutter_masked_text2/flutter_masked_text2.dart';
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
import 'package:tracko/repositories/contact_repository.dart';
import 'package:tracko/services/SessionService.dart';
import 'package:tracko/Utils/enums.dart';

class SmartAddItemController extends ChangeNotifier {
  final Transaction transaction;
  final bool isEdit;

  // Controllers
  final TextEditingController commentsController = TextEditingController();
  final MoneyMaskedTextController amountController = MoneyMaskedTextController(
      decimalSeparator: '.', thousandSeparator: ',', initialValue: 0.00);
  final TextEditingController exchangeRateController =
      TextEditingController(text: '1.0');
  final TextEditingController nameController = TextEditingController();
  List<TextEditingController> splitAmountControllers = [];

  // State
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
  Set<TrakoContact> splitList = {};
  List<Category> categories = [];
  List<Account> accounts = [];
  List<Contact> myContacts = [];
  final _contactRepo = ContactRepository();

  bool isLoading = true;

  SmartAddItemController(this.transaction) : isEdit = transaction.id != null {
    _initFromTransaction();
    _setupListeners();
    initData();
  }

  void _initFromTransaction() {
    date = transaction.date;

    if (transaction.originalCurrency != null &&
        transaction.originalCurrency!.isNotEmpty) {
      selectedCurrency = transaction.originalCurrency!;
      amountController.text =
          (transaction.originalAmount ?? 0.0).toStringAsFixed(2);
      exchangeRateController.text =
          (transaction.exchangeRate ?? 1.0).toString();
    } else {
      amountController.text = (transaction.amount == 0.0)
          ? "0.00"
          : transaction.amount.toStringAsFixed(2);
    }

    commentsController.text = transaction.comments;
    categoryId = transaction.categoryId;
    accountId = transaction.accountId;
    transferFromAccountId =
        transaction.transferFromAccountId ?? transaction.accountId;
    transferToAccountId =
        transaction.transferToAccountId ?? transaction.accountId;
    nameController.text = transaction.name;
    transactionType = transaction.transactionType;

    if (transaction.contacts != null) {
      splitList.addAll(transaction.contacts);
    }

    _rebuildSplitControllers();
  }

  void _setupListeners() {
    amountController.addListener(updateCalculatedAmount);
    exchangeRateController.addListener(updateCalculatedAmount);
  }

  Future<void> initData() async {
    isLoading = true;
    notifyListeners();

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

      if (transaction.originalCurrency != null &&
          transaction.originalCurrency!.isNotEmpty) {
        if (!availableCurrencies.contains(transaction.originalCurrency!)) {
          availableCurrencies.add(transaction.originalCurrency!);
        }
      }

      if (transaction.id == null && transaction.originalCurrency == null) {
        selectedCurrency = baseCurrency;
        exchangeRateController.text = '1.0';
      } else if (transaction.originalCurrency != null &&
          availableCurrencies.contains(transaction.originalCurrency)) {
        selectedCurrency = transaction.originalCurrency!;
      }
    } catch (e) {
      print("Error initializing user data: $e");
    }

    try {
      categories = await CategoryController.getAllCategories();
      accounts = await AccountController.getAllAccounts();
      myContacts = await _contactRepo.listMine();

      _ensureValidCategorySelection();

      final loadedContacts =
          await TransactionController.loadSplits(transaction);
      splitList = Set<TrakoContact>.from(loadedContacts);

      if (splitList.isEmpty && transactionType == TransactionType.DEBIT) {
        frequentSplitters = await UserController.getFrequentSplitters();
        TrakoContact userContact = SessionService.currentUserContact();
        splitList.add(userContact);
      }
      _rebuildSplitControllers();
    } catch (e) {
      print("Error loading external data: $e");
    }

    updateCalculatedAmount();
    isLoading = false;
    notifyListeners();
  }

  void _rebuildSplitControllers() {
    for (final controller in splitAmountControllers) {
      controller.dispose();
    }
    splitAmountControllers =
        List.generate(splitList.length, (_) => TextEditingController());
  }

  void updateCalculatedAmount() {
    if (selectedCurrency != baseCurrency) {
      double amt = castAmountText2Double(amountController.text);
      double rate = double.tryParse(exchangeRateController.text) ?? 1.0;
      convertedAmount = amt * rate;
    } else {
      convertedAmount = castAmountText2Double(amountController.text);
    }
    notifyListeners();
  }

  List<Category> get filteredCategories {
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
    final filtered = filteredCategories;
    final ids = filtered.map((c) => c.id).whereType<int>().toSet();
    if (categoryId != 0 && ids.contains(categoryId)) return;
    categoryId = 0;
  }

  void setTransactionType(int type) {
    if (type == TransactionType.CREDIT || type == TransactionType.TRANSFER) {
      splitList.clear();
      _rebuildSplitControllers();
    }
    transactionType = type;
    _ensureValidCategorySelection();
    notifyListeners();
  }

  void setCurrency(String currency) {
    selectedCurrency = currency;
    if (selectedCurrency == baseCurrency) {
      exchangeRateController.text = '1.0';
    } else if (currencyRates.containsKey(selectedCurrency)) {
      exchangeRateController.text = currencyRates[selectedCurrency].toString();
    }
    updateCalculatedAmount();
    notifyListeners();
  }

  void setDate(DateTime newDate) {
    date = newDate;
    notifyListeners();
  }

  void setCategory(int id) {
    categoryId = id;
    notifyListeners();
  }

  void setAccount(int id) {
    accountId = id;
    notifyListeners();
  }

  void setTransferFromAccount(int id) {
    transferFromAccountId = id;
    if (transferToAccountId == 0) transferToAccountId = id;
    notifyListeners();
  }

  void setTransferToAccount(int id) {
    transferToAccountId = id;
    notifyListeners();
  }

  String _normalizePhone(String? phone) {
    if (phone == null) return '';
    final digitsOnly = phone.replaceAll(RegExp(r'[^0-9]'), '');
    if (digitsOnly.length <= 10) return digitsOnly;
    return digitsOnly.substring(digitsOnly.length - 10);
  }

  Future<void> addSplit(User user) async {
    frequentSplitters.remove(user);
    TrakoContact contact = UserController.user2Contact(user);

    if (myContacts.isEmpty) {
      try {
        myContacts = await _contactRepo.listMine();
      } catch (_) {}
    }

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
    } catch (e) {}

    if (!splitList.contains(contact)) {
      splitList.add(contact);
      splitAmountControllers.add(TextEditingController());
      notifyListeners();
    }
  }

  void syncSplits(List<TrakoContact> contacts) {
    frequentSplitters.clear();
    splitList.clear();
    for (var contact in contacts) {
      if (!splitList.contains(contact)) {
        splitList.add(contact);
      }
    }
    _rebuildSplitControllers();
    notifyListeners();
  }

  void removeSplit(TrakoContact contact, int index) {
    splitList.remove(contact);
    if (index < splitAmountControllers.length) {
      splitAmountControllers[index].dispose();
      splitAmountControllers.removeAt(index);
    }
    notifyListeners();
  }

  double castAmountText2Double(String text) {
    text = text.replaceAll(",", "");
    return double.tryParse(text) ?? 0.0;
  }

  Transaction prepareTransactionForSave() {
    double inputAmount = castAmountText2Double(amountController.text);
    if (inputAmount <= 0) {
      throw Exception("Amount should be non-zero and non-negative");
    }

    if (nameController.text.trim().isEmpty) {
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

    // Clone or use existing (be careful with mutation if save fails)
    // Here we mutate as per original logic but controller isolates it a bit

    if (selectedCurrency != baseCurrency) {
      transaction.originalCurrency = selectedCurrency;
      transaction.originalAmount = inputAmount;
      transaction.exchangeRate = null;
    } else {
      transaction.amount = inputAmount;
      transaction.originalCurrency = null;
      transaction.originalAmount = null;
      transaction.exchangeRate = null;
    }

    transaction.date = date;
    transaction.name = nameController.text.trim();

    if (transactionType == TransactionType.TRANSFER) {
      transaction.transferFromAccountId = transferFromAccountId;
      transaction.transferToAccountId = transferToAccountId;
      transaction.accountId = transferFromAccountId;
    } else {
      transaction.categoryId = categoryId;
      transaction.accountId = accountId;
    }

    transaction.comments = commentsController.text;
    transaction.transactionType = transactionType;

    transaction.contacts.clear();
    if (splitList.isNotEmpty) {
      transaction.contacts.addAll(splitList);
    }

    return transaction;
  }

  Future<void> callSplitPage(BuildContext context) async {
    List<TrakoContact>? result = await Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => SelectBackendContactPage()),
    );
    if (result != null) {
      syncSplits(result);
    }
  }

  bool _isDisposed = false;

  @override
  void dispose() {
    _isDisposed = true;
    amountController.removeListener(updateCalculatedAmount);
    exchangeRateController.removeListener(updateCalculatedAmount);

    commentsController.dispose();
    amountController.dispose();
    exchangeRateController.dispose();
    nameController.dispose();
    for (var controller in splitAmountControllers) {
      controller.dispose();
    }
    super.dispose();
  }

  @override
  void notifyListeners() {
    if (!_isDisposed) {
      super.notifyListeners();
    }
  }
}
