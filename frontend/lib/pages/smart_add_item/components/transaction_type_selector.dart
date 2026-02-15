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
                  context, TransactionType.CREDIT, "Income", Colors.teal),
            ),
            Expanded(
              child: _buildTypeButton(
                  context, TransactionType.DEBIT, "Expense", Colors.redAccent),
            ),
            Expanded(
              child: _buildTypeButton(
                  context, TransactionType.TRANSFER, "Transfer", Colors.blueGrey),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTypeButton(
      BuildContext context, int type, String label, Color color) {
    bool isSelected = transactionType == type;
    return GestureDetector(
      onTap: () => onTypeChanged(type),
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
}
