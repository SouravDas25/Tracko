import 'package:flutter/material.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/models/recurring_transaction.dart';
import 'recurring_transaction_form_page.dart';
import 'package:tracko/repositories/recurring_transaction_repository.dart';
import 'package:intl/intl.dart';
import 'package:tracko/di/di.dart';

class RecurringTransactionListPage extends StatefulWidget {
  @override
  _RecurringTransactionListPageState createState() =>
      _RecurringTransactionListPageState();
}

class _RecurringTransactionListPageState
    extends State<RecurringTransactionListPage> {
  late final RecurringTransactionRepository _repository;
  List<RecurringTransaction> _transactions = [];
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _repository = sl<RecurringTransactionRepository>();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    try {
      final list = await _repository.getAll();
      setState(() {
        _transactions = list;
        _isLoading = false;
      });
    } catch (e) {
      setState(() => _isLoading = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Failed to load recurring transactions: $e')),
      );
    }
  }

  Future<void> _deleteTransaction(RecurringTransaction rt) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Delete Recurring Transaction'),
        content: Text('Are you sure you want to delete "${rt.name}"?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(foregroundColor: Colors.red),
            child: Text('Delete'),
          ),
        ],
      ),
    );

    if (confirm == true) {
      try {
        await _repository.delete(rt.id!);
        _loadData();
      } catch (e) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to delete: $e')),
        );
      }
    }
  }

  void _openForm([RecurringTransaction? rt]) async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => RecurringTransactionFormPage(transaction: rt),
      ),
    );

    if (result == true) {
      _loadData();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Recurring Transactions'),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _openForm(),
        child: Icon(Icons.add),
      ),
      body: _isLoading
          ? Center(child: CircularProgressIndicator())
          : _transactions.isEmpty
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.repeat, size: 64, color: Colors.grey),
                      SizedBox(height: 16),
                      Text('No recurring transactions found',
                          style: TextStyle(color: Colors.grey)),
                    ],
                  ),
                )
              : ListView.builder(
                  padding: EdgeInsets.all(16),
                  itemCount: _transactions.length,
                  itemBuilder: (context, index) {
                    final rt = _transactions[index];
                    return Card(
                      elevation: 2,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                      margin: EdgeInsets.only(bottom: 12),
                      child: ListTile(
                        onTap: () => _openForm(rt),
                        leading: CircleAvatar(
                          backgroundColor:
                              Theme.of(context).primaryColor.withOpacity(0.1),
                          child: Icon(
                            Icons.repeat,
                            color: Theme.of(context).primaryColor,
                          ),
                        ),
                        title: Text(
                          rt.name,
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                        subtitle: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              '${CommonUtil.toCurrency(rt.amount ?? 0)} • ${rt.frequency.name}',
                              style: TextStyle(fontWeight: FontWeight.w500),
                            ),
                            Text(
                              'Next: ${DateFormat('MMM dd, yyyy').format(rt.nextRunDate)}',
                              style:
                                  TextStyle(fontSize: 12, color: Colors.grey),
                            ),
                          ],
                        ),
                        trailing: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Switch(
                              value: rt.isActive,
                              onChanged: (val) async {
                                rt.isActive = val;
                                try {
                                  await _repository.update(rt.id!, rt);
                                  setState(() {});
                                } catch (e) {
                                  _loadData(); // Revert on error
                                }
                              },
                            ),
                            IconButton(
                              icon: Icon(Icons.delete, color: Colors.red),
                              onPressed: () => _deleteTransaction(rt),
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                ),
    );
  }
}
