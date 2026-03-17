import 'package:flutter/material.dart';

class AnalyticsEmptyState extends StatelessWidget {
  final String message;

  const AnalyticsEmptyState({
    Key? key,
    this.message = 'No transactions found for this period',
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 220,
      child: Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.insert_chart_outlined,
              size: 56,
              color: Theme.of(context).hintColor.withOpacity(0.4),
            ),
            const SizedBox(height: 12),
            Text(
              message,
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 15,
                color: Theme.of(context).hintColor,
                fontWeight: FontWeight.w500,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              'Try adjusting your filters or date range',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 13,
                color: Theme.of(context).hintColor.withOpacity(0.6),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
