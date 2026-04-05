import 'package:flutter/material.dart';
import 'package:tracko/Utils/enums.dart';
import 'package:tracko/component/FLushDialog.dart';
import 'package:tracko/component/LoadingDialog.dart';
import 'package:tracko/models/transaction.dart';
import 'package:tracko/pages/smart_add_item/controllers/smart_add_item_controller.dart';
import 'package:tracko/pages/smart_add_item/components/transaction_type_selector.dart';
import 'package:tracko/pages/smart_add_item/components/amount_input_section.dart';
import 'package:tracko/pages/smart_add_item/components/transaction_details_form.dart';
import 'package:tracko/pages/smart_add_item/components/split_manager_section.dart';
import 'package:tracko/pages/smart_add_item/components/comments_section.dart';

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
    return _SmartAddItemPage();
  }
}

class _SmartAddItemPage extends State<SmartAddItemPage> {
  late SmartAddItemController _controller;

  @override
  void initState() {
    super.initState();
    _controller = SmartAddItemController(widget.transaction);
    _controller.addListener(_onControllerUpdate);
  }

  void _onControllerUpdate() {
    if (mounted) setState(() {});
  }

  @override
  void dispose() {
    _controller.removeListener(_onControllerUpdate);
    _controller.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    bool isSuccessfulSave = false;
    LoadingDialog.show(context);
    try {
      Transaction transaction = _controller.prepareTransactionForSave();
      await widget.saveCallback(transaction);
      isSuccessfulSave = true;
    } catch (exception) {
      await FlushDialog.flash(context, "Error", exception.toString());
    } finally {
      await LoadingDialog.hide(context);
    }

    if (isSuccessfulSave && mounted) {
      Navigator.of(context).pop(isSuccessfulSave);
      await widget.complete(widget.transaction);
    }
  }

  @override
  Widget build(BuildContext context) {
    final typeColor = TransactionType.color(_controller.transactionType);

    return Scaffold(
      appBar: AppBar(
        backgroundColor: typeColor,
        actions: _controller.transactionType == TransactionType.DEBIT
            ? <Widget>[
                IconButton(
                  icon: Icon(Icons.call_split, size: 28.0),
                  onPressed: () => _controller.callSplitPage(context),
                  tooltip: "Split Transaction",
                )
              ]
            : [],
        title:
            Text(_controller.isEdit ? "Edit Transaction" : "New Transaction"),
        centerTitle: true,
        elevation: 0,
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 6, 16, 10),
          child: ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: typeColor,
              padding: EdgeInsets.symmetric(vertical: 14),
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(10),
              ),
              elevation: 2,
            ),
            onPressed: _save,
            child: Text(
              widget.mainButtonText,
              style: TextStyle(fontSize: 16.0, fontWeight: FontWeight.w600),
            ),
          ),
        ),
      ),
      body: SingleChildScrollView(
        child: Column(
          children: <Widget>[
            TransactionTypeSelector(
              transactionType: _controller.transactionType,
              onTypeChanged: _controller.setTransactionType,
            ),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  AmountInputSection(
                    transactionType: _controller.transactionType,
                    baseCurrency: _controller.baseCurrency,
                    selectedCurrency: _controller.selectedCurrency,
                    availableCurrencies: _controller.availableCurrencies,
                    amountController: _controller.amountController,
                    exchangeRateController: _controller.exchangeRateController,
                    convertedAmount: _controller.convertedAmount,
                    currencyRates: _controller.currencyRates,
                    onCurrencyChanged: _controller.setCurrency,
                  ),
                  SizedBox(height: 10),
                  _sectionHeader(
                      context, "Details", Icons.receipt_long_outlined),
                  SizedBox(height: 4),
                  TransactionDetailsForm(
                    transactionType: _controller.transactionType,
                    date: _controller.date,
                    onDateChanged: _controller.setDate,
                    categoryId: _controller.categoryId,
                    filteredCategories: _controller.filteredCategories,
                    onCategoryChanged: _controller.setCategory,
                    onAddCategory: () {
                      _controller.initData();
                    },
                    accountId: _controller.accountId,
                    transferFromAccountId: _controller.transferFromAccountId,
                    transferToAccountId: _controller.transferToAccountId,
                    accounts: _controller.accounts,
                    onAccountChanged: _controller.setAccount,
                    onTransferFromAccountChanged:
                        _controller.setTransferFromAccount,
                    onTransferToAccountChanged:
                        _controller.setTransferToAccount,
                    onAddAccount: () {
                      _controller.initData();
                    },
                    nameController: _controller.nameController,
                    onSwapTransferAccounts: _controller.swapTransferAccounts,
                  ),
                  if (_controller.transactionType == TransactionType.DEBIT) ...[
                    SizedBox(height: 10),
                    SplitManagerSection(
                      frequentSplitters: _controller.frequentSplitters,
                      splitList: _controller.splitList,
                      splitAmountControllers:
                          _controller.splitAmountControllers,
                      amount: _controller.castAmountText2Double(
                          _controller.amountController.text),
                      currencySymbol: _controller.selectedCurrency,
                      onCallSplitPage: () => _controller.callSplitPage(context),
                      onAddSplit: _controller.addSplit,
                      onDeleteSplit: _controller.removeSplit,
                    ),
                  ],
                  SizedBox(height: 10),
                  _sectionHeader(
                      context, "Notes", Icons.sticky_note_2_outlined),
                  SizedBox(height: 4),
                  CommentsSection(controller: _controller.commentsController),
                  SizedBox(height: 12),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _sectionHeader(BuildContext context, String title, IconData icon) {
    return Row(
      children: [
        Icon(icon, size: 16, color: Theme.of(context).hintColor),
        SizedBox(width: 6),
        Text(
          title,
          style: TextStyle(
            fontSize: 13,
            fontWeight: FontWeight.w600,
            color: Theme.of(context).hintColor,
            letterSpacing: 0.5,
          ),
        ),
      ],
    );
  }
}
