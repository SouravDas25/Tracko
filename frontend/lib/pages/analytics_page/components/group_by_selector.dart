import 'package:flutter/material.dart';
import 'package:tracko/pages/analytics_page/models/analytics_models.dart';

class GroupBySelector extends StatelessWidget {
  final GroupByMode groupBy;
  final ValueChanged<GroupByMode> onChanged;

  const GroupBySelector({
    Key? key,
    required this.groupBy,
    required this.onChanged,
  }) : super(key: key);

  String _label(GroupByMode mode) {
    switch (mode) {
      case GroupByMode.none:
        return 'None';
      case GroupByMode.category:
        return 'By Category';
      case GroupByMode.account:
        return 'By Account';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Container(
        height: 40,
        decoration: BoxDecoration(
          color: Theme.of(context).cardColor,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
              color: Theme.of(context).dividerColor.withOpacity(0.1)),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.02),
              blurRadius: 8,
              offset: const Offset(0, 2),
            )
          ],
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: GroupByMode.values.map((mode) {
            final isSelected = groupBy == mode;
            return GestureDetector(
              onTap: () => onChanged(mode),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 200),
                padding: const EdgeInsets.symmetric(horizontal: 16),
                height: 40,
                decoration: BoxDecoration(
                  color: isSelected
                      ? Theme.of(context).primaryColor
                      : Colors.transparent,
                  borderRadius: BorderRadius.circular(20),
                ),
                alignment: Alignment.center,
                child: Text(
                  _label(mode),
                  style: TextStyle(
                    color: isSelected
                        ? Colors.white
                        : Theme.of(context).hintColor,
                    fontWeight: FontWeight.w600,
                    fontSize: 13,
                  ),
                ),
              ),
            );
          }).toList(),
        ),
      ),
    );
  }
}
