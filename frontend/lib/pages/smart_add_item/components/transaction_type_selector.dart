import 'package:flutter/material.dart';
import 'package:tracko/Utils/enums.dart';

class TransactionTypeSelector extends StatelessWidget {
  final int transactionType;
  final Function(int) onTypeChanged;

  const TransactionTypeSelector({
    Key? key,
    required this.transactionType,
    required this.onTypeChanged,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Theme.of(context).cardColor,
      padding: EdgeInsets.symmetric(vertical: 10, horizontal: 12),
      margin: EdgeInsets.only(bottom: 12),
      child: Container(
        height: 40,
        decoration: BoxDecoration(
          color: Theme.of(context).brightness == Brightness.dark
              ? Colors.black26
              : Colors.grey.shade200,
          borderRadius: BorderRadius.circular(20),
        ),
        padding: EdgeInsets.all(3),
        child: Row(
          children: [
            Expanded(
              child: _buildTypeButton(context, TransactionType.CREDIT, "Income",
                  TransactionType.color(TransactionType.CREDIT)),
            ),
            Expanded(
              child: _buildTypeButton(context, TransactionType.DEBIT, "Expense",
                  TransactionType.color(TransactionType.DEBIT)),
            ),
            Expanded(
              child: _buildTypeButton(context, TransactionType.TRANSFER,
                  "Transfer", TransactionType.color(TransactionType.TRANSFER)),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTypeButton(
      BuildContext context, int type, String label, Color color) {
    bool isSelected = transactionType == type;
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: () => onTypeChanged(type),
        borderRadius: BorderRadius.circular(17),
        child: AnimatedContainer(
          duration: Duration(milliseconds: 200),
          decoration: BoxDecoration(
            color: isSelected ? color : Colors.transparent,
            borderRadius: BorderRadius.circular(17),
          ),
          alignment: Alignment.center,
          child: Text(
            label,
            style: TextStyle(
              color: isSelected
                  ? Colors.white
                  : Theme.of(context).hintColor.withOpacity(0.7),
              fontWeight: FontWeight.w600,
              fontSize: 13,
            ),
          ),
        ),
      ),
    );
  }
}
