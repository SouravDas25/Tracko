import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/models/budget_allocation_request.dart';
import 'package:tracko/services/budget_service.dart';

class AllocationDialog extends StatefulWidget {
  final int categoryId;
  final String categoryName;
  final int month;
  final int year;
  final double currentAllocation;
  final double availableToAssign;
  final VoidCallback onSuccess;

  const AllocationDialog({
    Key? key,
    required this.categoryId,
    required this.categoryName,
    required this.month,
    required this.year,
    required this.currentAllocation,
    required this.availableToAssign,
    required this.onSuccess,
  }) : super(key: key);

  @override
  _AllocationDialogState createState() => _AllocationDialogState();
}

class _AllocationDialogState extends State<AllocationDialog> {
  final _amountController = TextEditingController();
  final _budgetService = BudgetService();
  final _focusNode = FocusNode();
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _amountController.text = widget.currentAllocation == 0
        ? ''
        : widget.currentAllocation.toStringAsFixed(2);

    // Request focus after frame build to avoid assertion error
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _focusNode.requestFocus();
    });
  }

  @override
  void dispose() {
    _amountController.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  Future<void> _saveAllocation() async {
    final amountText = _amountController.text.trim();
    if (amountText.isEmpty) return;

    final amount = double.tryParse(amountText);
    if (amount == null) return;

    // Validate that input is not more than unallocated funds
    // Max allocatable for this category = What's already allocated to it + What's available in the pool
    double maxAllocatable = widget.currentAllocation + widget.availableToAssign;

    // Use a small epsilon for float comparison logic
    if (amount > maxAllocatable + 0.01) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Insufficient funds. Max allocatable: ${CommonUtil.toCurrency(maxAllocatable)}',
          ),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    setState(() {
      _isLoading = true;
    });

    try {
      final request = BudgetAllocationRequest(
        month: widget.month,
        year: widget.year,
        categoryId: widget.categoryId,
        amount: amount,
      );

      await _budgetService.allocateFunds(request);
      widget.onSuccess();
      Navigator.of(context).pop();
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Failed to update allocation: $e')),
      );
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text('Budget for ${widget.categoryName}'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Available to Assign: ${CommonUtil.toCurrency(widget.availableToAssign)}',
            style: TextStyle(
              color: widget.availableToAssign >= 0 ? Colors.green : Colors.red,
              fontWeight: FontWeight.bold,
            ),
          ),
          SizedBox(height: 16),
          TextField(
            controller: _amountController,
            focusNode: _focusNode,
            keyboardType: TextInputType.numberWithOptions(decimal: true),
            inputFormatters: [
              FilteringTextInputFormatter.allow(RegExp(r'^\d+\.?\d{0,2}')),
            ],
            decoration: InputDecoration(
              labelText: 'Allocated Amount',
              prefixText: CommonUtil.rupeeSign,
              border: OutlineInputBorder(),
            ),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: _isLoading ? null : () => Navigator.of(context).pop(),
          child: Text('Cancel'),
        ),
        ElevatedButton(
          onPressed: _isLoading ? null : _saveAllocation,
          child: _isLoading
              ? SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : Text('Save'),
        ),
      ],
    );
  }
}
