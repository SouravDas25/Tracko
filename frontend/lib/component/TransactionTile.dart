import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/Utils/enums.dart';
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
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 6.0),
      child: Slidable(
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
              borderRadius: BorderRadius.circular(16),
            ),
          ],
        ),
        child: Container(
          decoration: BoxDecoration(
            color: Theme.of(context).cardColor,
            borderRadius: BorderRadius.circular(16),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.04),
                blurRadius: 10,
                offset: Offset(0, 4),
              ),
            ],
            border: Border.all(
              color: Theme.of(context).dividerColor.withOpacity(0.05),
            ),
          ),
          child: Material(
            color: Colors.transparent,
            child: InkWell(
              borderRadius: BorderRadius.circular(16),
              onTap: () async {
                final saved = await Navigator.of(context).push(MaterialPageRoute(
                    builder: (context) =>
                        AddItemPage(transaction: transaction)));
                if (saved == true) {
                  try {
                    (parent as dynamic).refresh();
                  } catch (e) {
                    print("Parent refresh failed: $e");
                  }
                }
              },
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Row(
                  children: [
                    _buildAvatar(context),
                    SizedBox(width: 16),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            transaction.name,
                            style: TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.w600,
                              color:
                                  Theme.of(context).textTheme.bodyLarge?.color,
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                          SizedBox(height: 4),
                          Text(
                            _getSubtitle(transaction),
                            style: TextStyle(
                              fontSize: 13,
                              color: Theme.of(context).hintColor,
                              fontWeight: FontWeight.w500,
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ],
                      ),
                    ),
                    SizedBox(width: 12),
                    _buildAmount(context),
                  ],
                ),
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
      width: 48,
      height: 48,
      decoration: BoxDecoration(
        color: Theme.of(context).primaryColor,
        shape: BoxShape.circle,
        boxShadow: [
          BoxShadow(
            color: Theme.of(context).primaryColor.withOpacity(0.3),
            blurRadius: 8,
            offset: Offset(0, 2),
          ),
        ],
      ),
      child: Center(
        child: Text(
          CommonUtil.getInitials(transaction.name),
          style: TextStyle(
            color: Colors.white,
            fontWeight: FontWeight.bold,
            fontSize: 18,
          ),
        ),
      ),
    );
  }

  Widget _buildAmount(BuildContext context) {
    final bool isDebit = transaction.transactionType == TransactionType.DEBIT;
    final bool isTransfer = transaction.isTransfer;

    Color color;
    String sign;

    if (isTransfer) {
      // Transfers usually blue or distinctive
      color = Colors.blue.shade600;
      // If it's the debit side (out), maybe red or blue?
      // Typically transfers are neutral or blue in UI to distinguish from expense.
      // But let's stick to logic:
      // Transfer Out (Debit) -> -
      // Transfer In (Credit) -> +
      // But user might prefer neutral. Let's use blue for both but keep sign.
      if (transaction.transactionType == TransactionType.DEBIT) {
        sign = "- ";
      } else if (transaction.transactionType == TransactionType.CREDIT) {
        sign = "+ ";
      } else {
        sign = ""; // Type 3 (Transfer parent/wrapper?)
      }
    } else {
      color = isDebit ? Colors.red.shade400 : Colors.green.shade600;
      sign = isDebit ? "- " : "+ ";
    }

    String amountText = CommonUtil.toCurrency(transaction.amount);

    return Text(
      "$sign$amountText",
      style: TextStyle(
        color: color,
        fontWeight: FontWeight.bold,
        fontSize: 15,
      ),
    );
  }
}
