import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/Utils/enums.dart';
import 'package:tracko/component/amount_text.dart';
import 'package:tracko/controllers/TransactionController.dart';
import 'package:tracko/models/transaction.dart';
import 'package:tracko/pages/add_item_page/add_item.dart';
import 'package:flutter/material.dart';
import 'package:flutter_slidable/flutter_slidable.dart';

typedef DeleteCallback = void Function(dynamic, Transaction);

class TransactionTile extends StatelessWidget {
  final Transaction transaction;
  final State parent;
  final DeleteCallback deleteCallback;

  TransactionTile(this.parent, this.transaction, this.deleteCallback);

  void deleteTransactionAndCallback() async {
    await TransactionController.deleteById(transaction.id ?? 0);
    Navigator.pop(parent.context);
    WidgetUtil.toast("Transaction removed");
    if (this.deleteCallback != null) {
      try {
        this.deleteCallback(parent, transaction);
      } catch (e) {
        print(e.toString());
      }
    }
  }

  void _showDialog() {
    // flutter defined function
    showDialog(
      context: parent.context,
      builder: (BuildContext context) {
        // return object of type Dialog
        return AlertDialog(
          title: new Text("Delete Transaction"),
          content: new Text("Are sure you want to delete this transaction ?"),
          actions: <Widget>[
            new TextButton(
              child: new Text("No"),
              onPressed: () {
                Navigator.pop(context);
              },
            ),
            new TextButton(
              child: new Text("Yes"),
              onPressed: deleteTransactionAndCallback,
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Slidable(
      endActionPane: ActionPane(
        motion: ScrollMotion(),
        children: [
          SlidableAction(
            onPressed: (context) {
              _showDialog();
            },
            backgroundColor: Colors.red.shade400,
            foregroundColor: Colors.white,
            icon: Icons.delete_outline,
            label: 'Delete',
          ),
        ],
      ),
      child: Container(
        decoration: BoxDecoration(
          border: Border(
            bottom: BorderSide(
              color: Theme.of(context).dividerColor.withOpacity(0.08),
              width: 0.5,
            ),
          ),
        ),
        child: Material(
          color: Colors.transparent,
          child: InkWell(
            onTap: () async {
              final saved = await Navigator.of(context).push(MaterialPageRoute(
                  builder: (context) => AddItemPage(transaction: transaction)));
              if (saved == true) {
                try {
                  (parent as dynamic).refresh();
                } catch (e) {
                  print("Parent refresh failed: $e");
                }
              }
            },
            child: Padding(
              padding:
                  const EdgeInsets.symmetric(horizontal: 12.0, vertical: 8.0),
              child: Row(
                children: [
                  _buildAvatar(context),
                  SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          transaction.name,
                          style: TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                            color: Theme.of(context).textTheme.bodyLarge?.color,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        SizedBox(height: 2),
                        Text(
                          _getSubtitle(transaction),
                          style: TextStyle(
                            fontSize: 11.5,
                            color: Theme.of(context).hintColor,
                            fontWeight: FontWeight.w500,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                  SizedBox(width: 8),
                  _buildAmount(context),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  String _getSubtitle(Transaction t) {
    if (t.isTransfer) {
      // For now we might not have the linked account name loaded in the transaction list
      // unless we expand or fetch it.
      // But we can show "Transfer" at least.
      if (t.transactionType == TransactionType.DEBIT) {
        return "Transfer Out";
      } else if (t.transactionType == TransactionType.CREDIT) {
        return "Transfer In";
      } else {
        return "Transfer";
      }
    }

    return t.contacts.length > 1
        ? "${t.category?.name ?? ''} • ${t.contacts.length} contacts"
        : "${t.category?.name ?? 'Uncategorized'}";
  }

  Widget _buildAvatar(BuildContext context) {
    return Container(
      width: 32,
      height: 32,
      decoration: BoxDecoration(
        color: Theme.of(context).primaryColor,
        shape: BoxShape.circle,
      ),
      child: Center(
        child: Text(
          CommonUtil.getInitials(transaction.name),
          style: TextStyle(
            color: Colors.white,
            fontWeight: FontWeight.bold,
            fontSize: 13,
          ),
        ),
      ),
    );
  }

  Widget _buildAmount(BuildContext context) {
    final bool isDebit = transaction.transactionType == TransactionType.DEBIT;
    final bool isTransfer = transaction.isTransfer;

    Color color = Colors.blue;
    if (!isTransfer) {
      color = isDebit ? Colors.red : Colors.green;
    }

    return AmountText(
      amount: transaction.originalAmount ?? 0.0,
      color: color,
      currencyCode: transaction.originalCurrency,
    );
  }
}
