import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:datetime_picker_formfield/datetime_picker_formfield.dart';
import 'package:tracko/Utils/enums.dart';
import 'package:tracko/component/AccountDialog.dart';
import 'package:tracko/component/CategoryDialog.dart';
import 'package:tracko/models/account.dart';
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
  }) : super(key: key);

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
          decoration: InputDecoration(
            labelText: dateLabel,
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
            onDateChanged(date ?? DateTime.now());
          },
        ),
        SizedBox(height: 16),

        // Category (if not transfer)
        if (transactionType != TransactionType.TRANSFER) ...[
          Row(
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
                          color:
                              Theme.of(context).dividerColor.withOpacity(0.1)),
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
                    onCategoryChanged(id ?? 0);
                  },
                ),
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
                      builder: (_) => CategoryDialog(
                        callback: onAddCategory,
                        categoryType: transactionType == TransactionType.CREDIT
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
          ),
          SizedBox(height: 16),
        ],

        // Accounts
        if (transactionType == TransactionType.TRANSFER) ...[
          _buildAccountDropdown(context, "From Account", transferFromAccountId,
              (val) {
            onTransferFromAccountChanged(val ?? 0);
          }),
          SizedBox(height: 16),
          _buildAccountDropdown(context, "To Account", transferToAccountId,
              (val) {
            onTransferToAccountChanged(val ?? 0);
          }),
        ] else ...[
          Row(
            children: [
              Expanded(
                child:
                    _buildAccountDropdown(context, "Account", accountId, (val) {
                  onAccountChanged(val ?? 0);
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
                        callback: onAddAccount,
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
          controller: nameController,
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

  Widget _buildAccountDropdown(BuildContext context, String label, int? value,
      Function(int?) onChanged) {
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
}
