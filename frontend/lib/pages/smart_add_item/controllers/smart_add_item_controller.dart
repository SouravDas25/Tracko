import 'package:flutter/material.dart';
import 'package:flutter_masked_text2/flutter_masked_text2.dart';
import 'package:tracko/component/select_backend_contact.dart';
import 'package:tracko/controllers/AccountController.dart';
import 'package:tracko/controllers/CategoryController.dart';
import 'package:tracko/controllers/TransactionController.dart';
import 'package:tracko/models/account.dart';
import 'package:tracko/models/category.dart';
import 'package:tracko/models/contact.dart';
import 'package:tracko/models/transaction.dart';
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

  List<Contact> frequentSplitters = [];
  Set<Contact> splitList = {};
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
    transferFromAccountId = transaction.fromAccountId ?? transaction.accountId;
    transferToAccountId = transaction.toAccountId ?? transaction.accountId;
    nameController.text = transaction.name;
    transactionType = transaction.transactionType;

    splitList = Set<Contact>.from(transaction.contacts);

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
      final user = await SessionService.fetchMe();
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
      if (isEdit && transaction.id != null) {
        // Reload transaction to get full details (like linkedTransactionId) which might be missing from list DTO
        final fullTx = await TransactionController.findById(transaction.id!);
        if (fullTx != null) {
          // Update local state from full transaction
          transaction.linkedTransactionId = fullTx.linkedTransactionId;
          transaction.transactionType = fullTx.transactionType;
          transaction.accountId = fullTx.accountId;

          if (transaction.isTransfer &&
              transaction.linkedTransactionId != null) {
            try {
              final linkedTx = await TransactionController.findById(
                  transaction.linkedTransactionId!);
              if (linkedTx != null) {
                if (transaction.transactionType == TransactionType.DEBIT) {
                  transferFromAccountId = transaction.accountId;
                  transferToAccountId = linkedTx.accountId;
                } else {
                  transferFromAccountId = linkedTx.accountId;
                  transferToAccountId = transaction.accountId;
                }
              }
            } catch (e) {
              print("Error loading linked transaction: $e");
            }
          }
        }
      }

      categories = await CategoryController.getAllCategories();
      accounts = await AccountController.getAllAccounts();
      myContacts = await _contactRepo.listMine();

      _ensureValidCategorySelection();

      final loadedContacts =
          await TransactionController.loadSplits(transaction);
      splitList = Set<Contact>.from(loadedContacts);

      if (splitList.isEmpty && transactionType == TransactionType.DEBIT) {
        frequentSplitters = await _contactRepo.listMine();
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
        List.generate(splitList.length + 1, (_) => TextEditingController());
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

  Future<void> addSplit(Contact backendContact) async {
    frequentSplitters.remove(backendContact);
    if (!splitList.contains(backendContact)) {
      splitList.add(backendContact);
      splitAmountControllers.add(TextEditingController());
      notifyListeners();
    }
  }

  void syncSplits(List<Contact> contacts) {
    frequentSplitters.clear();
    splitList = Set<Contact>.from(contacts);
    _rebuildSplitControllers();
    notifyListeners();
  }

  void removeSplit(Contact contact, int index) {
    splitList.remove(contact);
    // index=0 is reserved for implicit "You" row in UI
    final controllerIndex = index;
    if (controllerIndex > 0 &&
        controllerIndex < splitAmountControllers.length) {
      splitAmountControllers[controllerIndex].dispose();
      splitAmountControllers.removeAt(controllerIndex);
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
      transaction.fromAccountId = transferFromAccountId;
      transaction.toAccountId = transferToAccountId;
      transaction.accountId =
          transferFromAccountId; // Source is the main account
      // Ensure category is TRANSFER
      // We might need to fetch it or let backend handle it, but for UI state:
      // transaction.categoryId = transferCategory (if available)
    } else {
      transaction.categoryId = categoryId;
      transaction.accountId = accountId;
      // Clear transfer fields if switching back
      transaction.fromAccountId = null;
      transaction.toAccountId = null;
    }

    transaction.comments = commentsController.text;
    transaction.transactionType = transactionType;

    transaction.contacts.clear();
    transaction.contacts.addAll(splitList);

    return transaction;
  }

  Future<void> callSplitPage(BuildContext context) async {
    List<Contact>? result = await Navigator.push(
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
