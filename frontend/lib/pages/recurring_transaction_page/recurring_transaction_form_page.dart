import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:flutter_masked_text2/flutter_masked_text2.dart';
import 'package:datetime_picker_formfield/datetime_picker_formfield.dart';
import 'package:tracko/Utils/enums.dart';
import 'package:tracko/models/account.dart';
import 'package:tracko/models/category.dart';
import 'package:tracko/models/recurring_transaction.dart';
import 'package:tracko/repositories/account_repository.dart';
import 'package:tracko/repositories/category_repository.dart';
import 'package:tracko/repositories/recurring_transaction_repository.dart';
import 'package:tracko/pages/smart_add_item/components/transaction_type_selector.dart';
import 'package:tracko/pages/smart_add_item/components/transaction_details_form.dart';
import 'package:tracko/pages/smart_add_item/components/amount_input_section.dart';
import 'package:tracko/services/SessionService.dart';
import 'package:tracko/component/app_dropdown.dart';
import 'package:tracko/di/di.dart';

class RecurringTransactionFormPage extends StatefulWidget {
  final RecurringTransaction? transaction;

  const RecurringTransactionFormPage({Key? key, this.transaction})
      : super(key: key);

  @override
  _RecurringTransactionFormPageState createState() =>
      _RecurringTransactionFormPageState();
}

class _RecurringTransactionFormPageState
    extends State<RecurringTransactionFormPage> {
  final _formKey = GlobalKey<FormState>();
  late final RecurringTransactionRepository _repository;
  late final AccountRepository _accountRepository;
  late final CategoryRepository _categoryRepository;

  late MoneyMaskedTextController _amountController;
  late TextEditingController _nameController;
  final TextEditingController _exchangeRateController =
      TextEditingController(text: '1.0');

  late int _transactionType;
  late Frequency _frequency;
  late DateTime _startDate;
  DateTime? _endDate;
  bool _isActive = true;

  int? _accountId;
  int? _toAccountId;
  int? _categoryId;

  // Currency State
  String _baseCurrency = 'INR';
  String _selectedCurrency = 'INR';
  double _convertedAmount = 0.0;
  List<String> _availableCurrencies = ['INR'];
  Map<String, double> _currencyRates = {};

  List<Account> _accounts = [];
  List<Category> _categories = [];
  bool _isLoading = true;
  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    _repository = sl<RecurringTransactionRepository>();
    _accountRepository = sl<AccountRepository>();
    _categoryRepository = sl<CategoryRepository>();
    final rt = widget.transaction;
    _transactionType = rt?.transactionType ?? TransactionType.DEBIT;
    _frequency = rt?.frequency ?? Frequency.MONTHLY;
    _startDate = rt?.startDate ?? DateTime.now();
    _endDate = rt?.endDate;
    _isActive = rt?.isActive ?? true;
    _accountId = rt?.accountId == 0 ? null : rt?.accountId;
    _toAccountId = rt?.toAccountId;
    _categoryId = rt?.categoryId == 0 ? null : rt?.categoryId;

    _amountController = MoneyMaskedTextController(
        decimalSeparator: '.', thousandSeparator: ',', initialValue: 0.0);
    _nameController = TextEditingController(text: rt?.name ?? '');

    if (rt != null &&
        rt.originalCurrency != null &&
        rt.originalCurrency!.isNotEmpty) {
      _selectedCurrency = rt.originalCurrency!;
      _amountController.text = (rt.originalAmount ?? 0.0).toStringAsFixed(2);
      _exchangeRateController.text = (rt.exchangeRate ?? 1.0).toString();
    } else {
      _amountController.text = (rt?.amount ?? 0.0) == 0.0
          ? "0.00"
          : (rt?.amount ?? 0.0).toStringAsFixed(2);
    }

    _amountController.addListener(_updateCalculatedAmount);
    _exchangeRateController.addListener(_updateCalculatedAmount);

    _loadDependencies();
  }

  @override
  void dispose() {
    _amountController.removeListener(_updateCalculatedAmount);
    _exchangeRateController.removeListener(_updateCalculatedAmount);
    _amountController.dispose();
    _exchangeRateController.dispose();
    _nameController.dispose();
    super.dispose();
  }

  Future<void> _loadDependencies() async {
    try {
      final accounts = await _accountRepository.getAllAccounts();
      final categories = await _categoryRepository.getAll();
      final user = await sl<SessionService>().fetchMe();

      setState(() {
        _accounts = accounts;
        _categories = categories;

        // Setup Currency
        _baseCurrency =
            user.baseCurrency.isNotEmpty ? user.baseCurrency : 'INR';
        _availableCurrencies = [_baseCurrency];
        _currencyRates = {};

        if (user.secondaryCurrencies.isNotEmpty) {
          for (var uc in user.secondaryCurrencies) {
            if (!_availableCurrencies.contains(uc.currencyCode)) {
              _availableCurrencies.add(uc.currencyCode);
            }
            _currencyRates[uc.currencyCode] = uc.exchangeRate;
          }
        }

        final rt = widget.transaction;
        if (rt != null &&
            rt.originalCurrency != null &&
            rt.originalCurrency!.isNotEmpty) {
          if (!_availableCurrencies.contains(rt.originalCurrency!)) {
            _availableCurrencies.add(rt.originalCurrency!);
          }
        }

        if (rt == null) {
          _selectedCurrency = _baseCurrency;
          _exchangeRateController.text = '1.0';
        } else if (rt.originalCurrency != null &&
            _availableCurrencies.contains(rt.originalCurrency)) {
          _selectedCurrency = rt.originalCurrency!;
        } else {
          // Fallback or legacy case where we might want to default to base or keep existing if it matches base
          if (_selectedCurrency.isEmpty) _selectedCurrency = _baseCurrency;
        }

        _isLoading = false;

        // Auto-select first account if not set
        if (_accountId == null && _accounts.isNotEmpty) {
          _accountId = _accounts.first.id;
        }

        _updateCalculatedAmount();
      });
    } catch (e) {
      setState(() => _isLoading = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Failed to load data: $e')),
      );
    }
  }

  double _castAmountText2Double(String text) {
    text = text.replaceAll(",", "");
    return double.tryParse(text) ?? 0.0;
  }

  void _updateCalculatedAmount() {
    if (_selectedCurrency != _baseCurrency) {
      double amt = _castAmountText2Double(_amountController.text);
      double rate = double.tryParse(_exchangeRateController.text) ?? 1.0;
      setState(() {
        _convertedAmount = amt * rate;
      });
    } else {
      setState(() {
        _convertedAmount = _castAmountText2Double(_amountController.text);
      });
    }
  }

  void _setCurrency(String currency) {
    setState(() {
      _selectedCurrency = currency;
      if (_selectedCurrency == _baseCurrency) {
        _exchangeRateController.text = '1.0';
      } else if (_currencyRates.containsKey(_selectedCurrency)) {
        _exchangeRateController.text =
            _currencyRates[_selectedCurrency].toString();
      }
      _updateCalculatedAmount();
    });
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    _formKey.currentState!.save();

    if (_accountId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Please select an account')),
      );
      return;
    }
    if (_categoryId == null && _transactionType != TransactionType.TRANSFER) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Please select a category')),
      );
      return;
    }
    if (_transactionType == TransactionType.TRANSFER && _toAccountId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Please select a destination account')),
      );
      return;
    }

    if (_nameController.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Please enter a description')),
      );
      return;
    }

    setState(() => _isSaving = true);

    try {
      final rt = widget.transaction ?? RecurringTransaction();
      rt.name = _nameController.text;

      double inputAmount = _castAmountText2Double(_amountController.text);
      // Always set original fields, even for base currency
      rt.originalCurrency = _selectedCurrency;
      rt.originalAmount = inputAmount;

      if (_selectedCurrency != _baseCurrency) {
        rt.exchangeRate = double.tryParse(_exchangeRateController.text) ?? 1.0;
      } else {
        rt.exchangeRate = 1.0;
      }

      // Legacy amount field (still used for display in UI until refreshed)
      rt.amount = inputAmount * (rt.exchangeRate ?? 1.0);

      rt.transactionType = _transactionType;
      rt.frequency = _frequency;
      rt.startDate = _startDate;
      if (widget.transaction == null) {
        rt.nextRunDate = _startDate;
      }
      rt.endDate = _endDate;
      rt.isActive = _isActive;
      rt.accountId = _accountId!;
      rt.categoryId = _categoryId ?? 0;
      rt.toAccountId =
          _transactionType == TransactionType.TRANSFER ? _toAccountId : null;

      if (rt.id != null) {
        await _repository.update(rt.id!, rt);
      } else {
        await _repository.create(rt);
      }

      Navigator.pop(context, true);
    } catch (e) {
      setState(() => _isSaving = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Failed to save: $e')),
      );
    }
  }

  List<Category> get _filteredCategories {
    return _categories.where((c) {
      if (_transactionType == TransactionType.CREDIT) {
        return c.categoryType == 'INCOME';
      }
      if (_transactionType == TransactionType.DEBIT) {
        return c.categoryType == 'EXPENSE';
      }
      return true;
    }).toList();
  }

  Color get _themeColor {
    if (_transactionType == TransactionType.DEBIT) return Colors.redAccent;
    if (_transactionType == TransactionType.TRANSFER) return Colors.blueGrey;
    return Colors.teal;
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return Scaffold(
        appBar: AppBar(title: Text('Recurring Transaction')),
        body: Center(child: CircularProgressIndicator()),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(widget.transaction == null
            ? 'New Recurring Transaction'
            : 'Edit Recurring Transaction'),
        backgroundColor: _themeColor,
        elevation: 0,
      ),
      body: Form(
        key: _formKey,
        child: SingleChildScrollView(
          child: Column(
            children: [
              TransactionTypeSelector(
                transactionType: _transactionType,
                onTypeChanged: (val) {
                  setState(() {
                    _transactionType = val;
                    // Reset category if type changes and current category invalid
                    if (_transactionType != TransactionType.TRANSFER) {
                      if (_categoryId != null) {
                        // Check if current category is valid for new type
                        final valid =
                            _filteredCategories.any((c) => c.id == _categoryId);
                        if (!valid) _categoryId = null;
                      }
                    } else {
                      _categoryId = null;
                    }
                  });
                },
              ),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // Amount Input Section
                    AmountInputSection(
                      transactionType: _transactionType,
                      baseCurrency: _baseCurrency,
                      selectedCurrency: _selectedCurrency,
                      availableCurrencies: _availableCurrencies,
                      amountController: _amountController,
                      exchangeRateController: _exchangeRateController,
                      convertedAmount: _convertedAmount,
                      currencyRates: _currencyRates,
                      onCurrencyChanged: _setCurrency,
                    ),
                    SizedBox(height: 16),

                    // Frequency Dropdown
                    AppBottomSheetPicker<Frequency>(
                      value: _frequency,
                      items: Frequency.values,
                      title: 'Frequency',
                      labelBuilder: (f) => f.name[0] + f.name.substring(1).toLowerCase(),
                      iconBuilder: (_) => Icons.repeat,
                      onSelected: (v) => setState(() {
                        if (v != null) _frequency = v;
                      }),
                      allItemsLabel: 'Select Frequency',
                      isExpanded: true,
                    ),
                    SizedBox(height: 16),

                    // Transaction Details (Date, Category, Accounts, Name)
                    TransactionDetailsForm(
                      transactionType: _transactionType,
                      date: _startDate,
                      onDateChanged: (val) => setState(() => _startDate = val),
                      dateLabel: 'Start Date',
                      categoryId: _categoryId ?? 0,
                      filteredCategories: _filteredCategories,
                      onCategoryChanged: (val) =>
                          setState(() => _categoryId = val),
                      onAddCategory: () => _loadDependencies(),
                      accountId: _accountId ?? 0,
                      transferFromAccountId: _accountId ?? 0,
                      transferToAccountId: _toAccountId ?? 0,
                      accounts: _accounts,
                      onAccountChanged: (val) =>
                          setState(() => _accountId = val),
                      onTransferFromAccountChanged: (val) =>
                          setState(() => _accountId = val),
                      onTransferToAccountChanged: (val) =>
                          setState(() => _toAccountId = val),
                      onAddAccount: () => _loadDependencies(),
                      nameController: _nameController,
                    ),
                    SizedBox(height: 16),

                    // End Date Picker
                    _buildEndDateField(),
                    SizedBox(height: 16),

                    // Active Switch
                    _buildActiveSwitch(),
                    SizedBox(height: 24),

                    // Save Button
                    ElevatedButton(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: _themeColor,
                        padding: EdgeInsets.symmetric(vertical: 16),
                        foregroundColor: Colors.white,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                        elevation: 4,
                      ),
                      onPressed: _isSaving ? null : _save,
                      child: _isSaving
                          ? SizedBox(
                              width: 24,
                              height: 24,
                              child: CircularProgressIndicator(
                                  color: Colors.white, strokeWidth: 2))
                          : Text(
                              'Save',
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
      ),
    );
  }

  Widget _buildEndDateField() {
    return DateTimeField(
      initialValue: _endDate,
      format: DateFormat('dd-MMM-yyyy'),
      readOnly: true,
      resetIcon: null,
      decoration: InputDecoration(
        labelText: 'End Date (Optional)',
        prefixIcon: Icon(Icons.event_busy_outlined),
        suffixIcon: _endDate != null
            ? IconButton(
                icon: Icon(Icons.clear, size: 20),
                onPressed: () => setState(() => _endDate = null),
              )
            : null,
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
          borderSide: BorderSide(color: _themeColor, width: 2),
        ),
        filled: true,
        fillColor: Theme.of(context).cardColor,
        contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 16),
      ),
      onShowPicker: (context, currentValue) {
        return showDatePicker(
          context: context,
          initialDate: currentValue ?? _startDate.add(Duration(days: 30)),
          firstDate: _startDate,
          lastDate: DateTime(2100),
        );
      },
      onChanged: (date) => setState(() => _endDate = date),
    );
  }

  Widget _buildActiveSwitch() {
    return Container(
      decoration: BoxDecoration(
        color: Theme.of(context).cardColor,
        borderRadius: BorderRadius.circular(16),
        border:
            Border.all(color: Theme.of(context).dividerColor.withOpacity(0.1)),
      ),
      padding: EdgeInsets.symmetric(vertical: 4),
      child: SwitchListTile(
        title: Text('Active', style: TextStyle(fontWeight: FontWeight.w600)),
        secondary: Icon(Icons.power_settings_new,
            color: _isActive ? Colors.green : Colors.grey),
        value: _isActive,
        activeColor: _themeColor,
        onChanged: (v) => setState(() => _isActive = v),
      ),
    );
  }
}
