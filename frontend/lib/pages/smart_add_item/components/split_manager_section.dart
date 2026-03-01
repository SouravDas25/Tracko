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
    double a;
    final totalPeople = splitList.length + 1; // + you
    if (amount > 0 && totalPeople > 0) {
      a = (amount / totalPeople);
    } else {
      a = 0;
    }
    return "$currencySymbol ${a.toStringAsFixed(2)}";
  }

  @override
  Widget build(BuildContext context) {
    final contactsList = splitList.toList(growable: false);
    final totalPeople = contactsList.length + 1;
    bool disableSlide = totalPeople <= 1;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              "Splits",
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: Theme.of(context).hintColor,
              ),
            ),
            IconButton(
              icon:
                  Icon(Icons.call_split, color: Theme.of(context).primaryColor),
              onPressed: () => onCallSplitPage(),
              tooltip: "Split Transaction",
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
                          onPressed: () {
                            onAddSplit(contact);
                          },
                        ),
                      ))
                  .toList(),
            ),
          ),
          SizedBox(height: 12),
        ],
        ListView.builder(
          physics: NeverScrollableScrollPhysics(),
          shrinkWrap: true,
          itemCount: totalPeople,
          itemBuilder: (context, int index) {
            String displayAmount = _calcAmount();

            final isYouRow = index == 0;
            final Contact? element = isYouRow ? null : contactsList[index - 1];

            return Padding(
              padding: const EdgeInsets.only(bottom: 8.0),
              child: Slidable(
                enabled: !disableSlide,
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
                        color: Theme.of(context).dividerColor.withOpacity(0.1)),
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
                    subtitle: Text(
                      isYouRow ? "" : ((element?.phoneNo ?? '')),
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
