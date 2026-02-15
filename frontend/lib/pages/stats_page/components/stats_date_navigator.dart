import 'package:flutter/material.dart';

class StatsDateNavigator extends StatelessWidget {
  final String dateText;
  final bool isLoading;
  final Function() onPrevious;
  final Function() onNext;

  const StatsDateNavigator({
    Key? key,
    required this.dateText,
    required this.isLoading,
    required this.onPrevious,
    required this.onNext,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.fromLTRB(16, 0, 16, 16),
      padding: const EdgeInsets.symmetric(vertical: 8),
      decoration: BoxDecoration(
        color: Theme.of(context).cardColor,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: Theme.of(context).dividerColor.withOpacity(0.1),
        ),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          IconButton(
            icon: const Icon(Icons.chevron_left_rounded),
            color: Theme.of(context).primaryColor,
            onPressed: isLoading ? null : onPrevious,
          ),
          Text(
            dateText,
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
              color: Theme.of(context).textTheme.bodyLarge?.color,
            ),
          ),
          IconButton(
            icon: const Icon(Icons.chevron_right_rounded),
            color: Theme.of(context).primaryColor,
            onPressed: isLoading ? null : onNext,
          ),
        ],
      ),
    );
  }
}
