import 'package:flutter/material.dart';
import 'package:flutter_slidable/flutter_slidable.dart';
import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/models/contact.dart';

class SplitManagerSection extends StatelessWidget {
  final List<Contact> frequentSplitters;
  final Set<Contact> splitList;
  final List<TextEditingController> splitAmountControllers;
  final double amount;
  final String currencySymbol;
  final Function onCallSplitPage;
  final Function(Contact) onAddSplit;
  final Function(Contact, int) onDeleteSplit;

  const SplitManagerSection({
    Key? key,
    required this.frequentSplitters,
    required this.splitList,
    required this.splitAmountControllers,
    required this.amount,
    required this.currencySymbol,
    required this.onCallSplitPage,
    required this.onAddSplit,
    required this.onDeleteSplit,
  }) : super(key: key);

  String _calcAmount() {
    final totalPeople = splitList.length + 1; // + you
    final a = (amount > 0 && totalPeople > 0) ? (amount / totalPeople) : 0.0;
    return "$currencySymbol ${a.toStringAsFixed(2)}";
  }

  @override
  Widget build(BuildContext context) {
    final contactsList = splitList.toList(growable: false);
    final hasSplits = contactsList.isNotEmpty;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Row(
              children: [
                Icon(Icons.call_split, size: 16,
                    color: Theme.of(context).hintColor),
                SizedBox(width: 6),
                Text(
                  "Splits",
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: Theme.of(context).hintColor,
                    letterSpacing: 0.5,
                  ),
                ),
              ],
            ),
            TextButton.icon(
              icon: Icon(Icons.person_add_outlined, size: 18),
              label: Text("Add"),
              onPressed: () => onCallSplitPage(),
              style: TextButton.styleFrom(
                foregroundColor: Theme.of(context).primaryColor,
              ),
            ),
          ],
        ),
        if (frequentSplitters.isNotEmpty) ...[
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: frequentSplitters
                  .map((Contact contact) => Padding(
                        padding: const EdgeInsets.only(right: 8.0),
                        child: ActionChip(
                          avatar: WidgetUtil.textAvatar(contact.name),
                          label: Text(contact.name),
                          backgroundColor: Theme.of(context).cardColor,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(16),
                            side: BorderSide(
                                color: Theme.of(context)
                                    .dividerColor
                                    .withOpacity(0.1)),
                          ),
                          onPressed: () => onAddSplit(contact),
                        ),
                      ))
                  .toList(),
            ),
          ),
          SizedBox(height: 12),
        ],
        if (!hasSplits && frequentSplitters.isEmpty)
          Container(
            width: double.infinity,
            padding: EdgeInsets.symmetric(vertical: 24),
            decoration: BoxDecoration(
              color: Theme.of(context).cardColor,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(
                  color: Theme.of(context).dividerColor.withOpacity(0.1)),
            ),
            child: Column(
              children: [
                Icon(Icons.group_outlined, size: 32,
                    color: Theme.of(context).hintColor.withOpacity(0.4)),
                SizedBox(height: 8),
                Text(
                  "No splits yet",
                  style: TextStyle(
                    color: Theme.of(context).hintColor.withOpacity(0.6),
                    fontSize: 13,
                  ),
                ),
              ],
            ),
          ),
        if (hasSplits)
          ListView.builder(
            physics: NeverScrollableScrollPhysics(),
            shrinkWrap: true,
            itemCount: contactsList.length + 1, // +1 for "You" row
            itemBuilder: (context, int index) {
              final displayAmount = _calcAmount();
              final isYouRow = index == 0;
              final Contact? element =
                  isYouRow ? null : contactsList[index - 1];

              return Padding(
                padding: const EdgeInsets.only(bottom: 8.0),
                child: Slidable(
                  enabled: !isYouRow,
                  endActionPane: ActionPane(
                    motion: ScrollMotion(),
                    children: [
                      SlidableAction(
                        onPressed: (context) {
                          if (!isYouRow && element != null) {
                            onDeleteSplit(element, index);
                          }
                        },
                        backgroundColor: Theme.of(context).colorScheme.error,
                        foregroundColor: Theme.of(context).colorScheme.onError,
                        icon: Icons.delete,
                        borderRadius: BorderRadius.circular(16),
                      ),
                    ],
                  ),
                  child: Container(
                    decoration: BoxDecoration(
                      color: Theme.of(context).cardColor,
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(
                          color: Theme.of(context)
                              .dividerColor
                              .withOpacity(0.1)),
                    ),
                    child: ListTile(
                      contentPadding:
                          EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                      dense: true,
                      trailing: Text(
                        displayAmount,
                        style: TextStyle(
                            fontSize: 18.0, fontWeight: FontWeight.bold),
                      ),
                      title: Text(
                        isYouRow ? "You" : (element?.name ?? ''),
                        style: TextStyle(
                            fontSize: 16.0, fontWeight: FontWeight.w600),
                      ),
                      subtitle: isYouRow
                          ? null
                          : Text(
                              element?.phoneNo ?? '',
                              style: TextStyle(fontSize: 14.0),
                            ),
                      leading: WidgetUtil.textAvatar(
                          isYouRow ? "You" : (element?.name ?? '')),
                    ),
                  ),
                ),
              );
            },
          ),
      ],
    );
  }
}
