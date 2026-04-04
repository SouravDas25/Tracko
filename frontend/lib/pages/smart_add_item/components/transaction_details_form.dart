import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:datetime_picker_formfield/datetime_picker_formfield.dart';
import 'package:tracko/Utils/enums.dart';
import 'package:tracko/component/AccountDialog.dart';
import 'package:tracko/component/CategoryDialog.dart';
import 'package:tracko/models/account.dart';
import 'package:tracko/component/app_dropdown.dart';
import 'package:tracko/models/category.dart';

class TransactionDetailsForm extends StatelessWidget {
  final int transactionType;
  final DateTime date;
  final Function(DateTime) onDateChanged;
  final int categoryId;
  final List<Category> filteredCategories;
  final Function(int) onCategoryChanged;
  final Function onAddCategory;
  final int accountId;
  final int transferFromAccountId;
  final int transferToAccountId;
  final List<Account> accounts;
  final Function(int) onAccountChanged;
  final Function(int) onTransferFromAccountChanged;
  final Function(int) onTransferToAccountChanged;
  final Function onAddAccount;
  final TextEditingController nameController;
  final String dateLabel;
  final VoidCallback? onSwapTransferAccounts;

  const TransactionDetailsForm({
    Key? key,
    required this.transactionType,
    required this.date,
    required this.onDateChanged,
    required this.categoryId,
    required this.filteredCategories,
    required this.onCategoryChanged,
    required this.onAddCategory,
    required this.accountId,
    required this.transferFromAccountId,
    required this.transferToAccountId,
    required this.accounts,
    required this.onAccountChanged,
    required this.onTransferFromAccountChanged,
    required this.onTransferToAccountChanged,
    required this.onAddAccount,
    required this.nameController,
    this.dateLabel = 'Date',
    this.onSwapTransferAccounts,
  }) : super(key: key);

  InputDecoration _fieldDecoration(BuildContext context,
      {required String label, required IconData icon}) {
    return InputDecoration(
      labelText: label,
      prefixIcon: Icon(icon, size: 20),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide.none,
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide:
            BorderSide(color: Theme.of(context).dividerColor.withOpacity(0.1)),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide(color: Theme.of(context).primaryColor, width: 2),
      ),
      filled: true,
      fillColor: Theme.of(context).cardColor,
      contentPadding: EdgeInsets.symmetric(horizontal: 14, vertical: 12),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // Date Picker
        DateTimeField(
          initialValue: date,
          format: DateFormat('dd-MMM-yyyy'),
          readOnly: true,
          resetIcon: null,
          decoration: _fieldDecoration(context,
              label: dateLabel, icon: Icons.calendar_today_outlined),
          onShowPicker: (context, currentValue) {
            return showDatePicker(
                context: context,
                firstDate: DateTime(1900),
                initialDate: currentValue ?? DateTime.now(),
                lastDate: DateTime(2100));
          },
          onChanged: (date) {
            onDateChanged(date ?? DateTime.now());
          },
        ),
        SizedBox(height: 10),

        // Category (if not transfer)
        if (transactionType != TransactionType.TRANSFER) ...[
          Row(
            children: [
              Expanded(
                child: AppBottomSheetPicker<Category>(
                  value: _findCategory(categoryId),
                  items: filteredCategories,
                  title: 'Select Category',
                  labelBuilder: (c) => c.name,
                  iconBuilder: (c) => Icons.category_outlined,
                  onSelected: (c) => onCategoryChanged(c?.id ?? 0),
                  allItemsLabel: 'Category',
                  isExpanded: true,
                ),
              ),
              SizedBox(width: 10),
              _addButton(context, onTap: () {
                showDialog(
                  context: context,
                  builder: (_) => CategoryDialog(
                    callback: onAddCategory,
                    categoryType: transactionType == TransactionType.CREDIT
                        ? 'INCOME'
                        : 'EXPENSE',
                  ),
                );
              }),
            ],
          ),
          SizedBox(height: 10),
        ],

        // Accounts
        if (transactionType == TransactionType.TRANSFER) ...[
          Row(
            children: [
              Expanded(
                child: AppBottomSheetPicker<Account>(
                  value: _findAccount(transferFromAccountId),
                  items: accounts,
                  title: 'Select From Account',
                  labelBuilder: (a) => a.name,
                  iconBuilder: (a) => Icons.account_balance_wallet_outlined,
                  onSelected: (a) => onTransferFromAccountChanged(a?.id ?? 0),
                  allItemsLabel: 'From Account',
                  isExpanded: true,
                ),
              ),
              if (onSwapTransferAccounts != null) ...[
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
                    onTap: onSwapTransferAccounts,
                    child: Container(
                      height: 48,
                      width: 48,
                      alignment: Alignment.center,
                      child: Icon(Icons.swap_vert_rounded,
                          size: 20,
                          color: Theme.of(context).colorScheme.onSurface),
                    ),
                  ),
                ),
              ],
            ],
          ),
          SizedBox(height: 10),
          AppBottomSheetPicker<Account>(
            value: _findAccount(transferToAccountId),
            items: accounts,
            title: 'Select To Account',
            labelBuilder: (a) => a.name,
            iconBuilder: (a) => Icons.account_balance_wallet_outlined,
            onSelected: (a) => onTransferToAccountChanged(a?.id ?? 0),
            allItemsLabel: 'To Account',
            isExpanded: true,
          ),
        ] else ...[
          Row(
            children: [
              Expanded(
                child: AppBottomSheetPicker<Account>(
                  value: _findAccount(accountId),
                  items: accounts,
                  title: 'Select Account',
                  labelBuilder: (a) => a.name,
                  iconBuilder: (a) => Icons.account_balance_wallet_outlined,
                  onSelected: (a) => onAccountChanged(a?.id ?? 0),
                  allItemsLabel: 'Account',
                  isExpanded: true,
                ),
              ),
              SizedBox(width: 10),
              _addButton(context, onTap: () {
                showDialog(
                  context: context,
                  builder: (_) => AccountDialog(
                    callback: onAddAccount,
                  ),
                );
              }),
            ],
          ),
        ],
        SizedBox(height: 10),

        // Name Input
        TextField(
          controller: nameController,
          decoration: _fieldDecoration(context,
              label: 'Description', icon: Icons.description_outlined),
        ),
      ],
    );
  }

  Widget _addButton(BuildContext context, {required VoidCallback onTap}) {
    return Ink(
      decoration: BoxDecoration(
        color: Theme.of(context).cardColor,
        shape: BoxShape.circle,
        border:
            Border.all(color: Theme.of(context).dividerColor.withOpacity(0.1)),
      ),
      child: InkWell(
        customBorder: CircleBorder(),
        onTap: onTap,
        child: Container(
          height: 48,
          width: 48,
          alignment: Alignment.center,
          child:
              Icon(Icons.add, size: 20, color: Theme.of(context).colorScheme.onSurface),
        ),
      ),
    );
  }

  Account? _findAccount(int? id) {
    if (id == null || id == 0) return null;
    try {
      return accounts.firstWhere((a) => a.id == id);
    } catch (_) {
      return null;
    }
  }

  Category? _findCategory(int? id) {
    if (id == null || id == 0) return null;
    try {
      return filteredCategories.firstWhere((c) => c.id == id);
    } catch (_) {
      return null;
    }
  }
}
