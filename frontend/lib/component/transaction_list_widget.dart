import 'package:flutter/material.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/component/TransactionTile.dart';
import 'package:tracko/models/transaction.dart';

class TransactionListWidget extends StatelessWidget {
  final List<Transaction> transactions;
  final bool hasMore;
  final bool isSliver;
  final State parent;
  final Future<void> Function(Transaction t)? onDelete;
  final ScrollController? scrollController;

  const TransactionListWidget({
    Key? key,
    required this.transactions,
    required this.parent,
    this.hasMore = false,
    this.isSliver = false,
    this.onDelete,
    this.scrollController,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    if (transactions.isEmpty && !hasMore) {
      final emptyState = Padding(
        padding: const EdgeInsets.only(top: 48.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: Theme.of(context).disabledColor.withOpacity(0.05),
                shape: BoxShape.circle,
              ),
              child: Icon(
                Icons.receipt_long_rounded,
                size: 48,
                color: Theme.of(context).disabledColor.withOpacity(0.5),
              ),
            ),
            const SizedBox(height: 16),
            Text(
              "No transactions yet",
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w600,
                color: Theme.of(context).hintColor,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              "Tap + to add a new one",
              style: TextStyle(
                fontSize: 14,
                color: Theme.of(context).disabledColor,
              ),
            ),
          ],
        ),
      );

      if (isSliver) {
        return SliverToBoxAdapter(child: emptyState);
      } else {
        return Center(child: emptyState);
      }
    }

    final int itemCount =
        transactions.length + ((!isSliver && hasMore) ? 1 : 0);

    Widget buildItem(BuildContext context, int index) {
      if (!isSliver && hasMore && index == transactions.length) {
        return const Padding(
          padding: EdgeInsets.all(16.0),
          child: Center(
            child: SizedBox(
              width: 24,
              height: 24,
              child: CircularProgressIndicator(strokeWidth: 2),
            ),
          ),
        );
      }

      final tx = transactions[index];
      final currentHuman = CommonUtil.humanDate(tx.date).toUpperCase();
      String? prevHuman;
      if (index > 0) {
        prevHuman =
            CommonUtil.humanDate(transactions[index - 1].date).toUpperCase();
      }

      final List<Widget> children = [];
      if (index == 0 || prevHuman != currentHuman) {
        children.add(
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 24, 20, 8),
            child: Text(
              currentHuman,
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.bold,
                color: Theme.of(context).hintColor,
                letterSpacing: 1.0,
              ),
            ),
          ),
        );
      }

      children.add(
        TransactionTile(parent, tx, (p, Transaction t) async {
          if (onDelete != null) {
            await onDelete!(t);
          }
        }),
      );

      if (children.length == 1) {
        return children.first;
      }
      return Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        mainAxisSize: MainAxisSize.min,
        children: children,
      );
    }

    if (isSliver) {
      return SliverList(
        delegate: SliverChildBuilderDelegate(
          buildItem,
          childCount: transactions.length,
        ),
      );
    } else {
      return ListView.builder(
        controller: scrollController,
        itemCount: itemCount,
        itemBuilder: buildItem,
      );
    }
  }
}
