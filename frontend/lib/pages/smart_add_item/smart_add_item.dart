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
    return Scaffold(
      appBar: AppBar(
        backgroundColor: _controller.transactionType == TransactionType.DEBIT
            ? Colors.redAccent
            : (_controller.transactionType == TransactionType.TRANSFER
                ? Colors.blueGrey
                : Colors.teal),
        actions: _controller.transactionType == TransactionType.DEBIT
            ? <Widget>[
                IconButton(
                  icon: Icon(
                    Icons.call_split,
                    size: 28.0,
                  ),
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
                  SizedBox(height: 16),
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
                  ),
                  if (_controller.transactionType == TransactionType.DEBIT) ...[
                    SizedBox(height: 16),
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
                  SizedBox(height: 16),
                  CommentsSection(controller: _controller.commentsController),
                  SizedBox(height: 24),
                  ElevatedButton(
                    style: ElevatedButton.styleFrom(
                      backgroundColor:
                          _controller.transactionType == TransactionType.DEBIT
                              ? Colors.redAccent
                              : (_controller.transactionType ==
                                      TransactionType.TRANSFER
                                  ? Colors.blueGrey
                                  : Colors.teal),
                      padding: EdgeInsets.symmetric(vertical: 16),
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                      elevation: 4,
                    ),
                    onPressed: _save,
                    child: Text(
                      widget.mainButtonText,
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
}
