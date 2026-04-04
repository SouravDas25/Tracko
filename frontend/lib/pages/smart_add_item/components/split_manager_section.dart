import 'package:flutter/material.dart';
import 'package:flutter_slidable/flutter_slidable.dart';
import 'package:tracko/Utils/CommonUtil.dart';
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
                Icon(Icons.call_split,
                    size: 16, color: Theme.of(context).hintColor),
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
                          avatar: CircleAvatar(
                            radius: 14,
                            backgroundColor: Theme.of(context).primaryColor,
                            child: Text(
                              CommonUtil.getInitials(contact.name),
                              style: TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.bold,
                                fontSize: 10,
                              ),
                            ),
                          ),
                          label: Text(contact.name, style: TextStyle(fontSize: 12)),
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
          SizedBox(height: 8),
        ],
        if (!hasSplits && frequentSplitters.isEmpty)
          Padding(
            padding: EdgeInsets.symmetric(vertical: 16),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.group_outlined,
                    size: 16,
                    color: Theme.of(context).hintColor.withOpacity(0.4)),
                SizedBox(width: 6),
                Text(
                  "No splits yet",
                  style: TextStyle(
                    color: Theme.of(context).hintColor.withOpacity(0.6),
                    fontSize: 12,
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
              final name = isYouRow ? "You" : (element?.name ?? '');

              return Slidable(
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
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 12, vertical: 8),
                    child: Row(
                      children: [
                        Container(
                          width: 32,
                          height: 32,
                          decoration: BoxDecoration(
                            color: Theme.of(context).primaryColor,
                            shape: BoxShape.circle,
                          ),
                          child: Center(
                            child: Text(
                              CommonUtil.getInitials(name),
                              style: TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.bold,
                                fontSize: 13,
                              ),
                            ),
                          ),
                        ),
                        SizedBox(width: 10),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                name,
                                style: TextStyle(
                                    fontSize: 14,
                                    fontWeight: FontWeight.w600),
                              ),
                              if (!isYouRow &&
                                  element?.phoneNo != null &&
                                  element!.phoneNo!.isNotEmpty)
                                Text(
                                  element.phoneNo!,
                                  style: TextStyle(
                                    fontSize: 11,
                                    color: Theme.of(context).hintColor,
                                  ),
                                ),
                            ],
                          ),
                        ),
                        Text(
                          displayAmount,
                          style: TextStyle(
                              fontSize: 14, fontWeight: FontWeight.w600),
                        ),
                      ],
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
