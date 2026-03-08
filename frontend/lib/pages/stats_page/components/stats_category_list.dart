import 'package:flutter/material.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/pages/stats_page/controllers/stats_controller.dart';
import 'package:tracko/Utils/ChartUtil.dart';

/// A UI component that renders a list of categories along with their aggregate statistics.
///
/// For each category, it displays:
/// - A color-coded avatar based on the category name.
/// - The category name and total amount spent/earned.
/// - A progress bar representing the percentage of the total amount.
///
/// It handles its own loading and error states and delegates tap events back
/// to the parent widget via [onCategoryTap].
class StatsCategoryList extends StatelessWidget {
  final bool loading;
  final String? error;
  final List<CategoryStat> stats;
  final double total;
  final Color kindColor;
  final Function(CategoryStat) onCategoryTap;

  const StatsCategoryList({
    Key? key,
    required this.loading,
    required this.error,
    required this.stats,
    required this.total,
    required this.kindColor,
    required this.onCategoryTap,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    if (loading) {
      return const Padding(
        padding: EdgeInsets.only(top: 40),
        child: Center(child: CircularProgressIndicator()),
      );
    }

    if (error != null) {
      return Padding(
        padding: const EdgeInsets.only(top: 40),
        child: Center(
          child: Text(
            error!,
            style: const TextStyle(color: Colors.red),
          ),
        ),
      );
    }

    if (stats.isEmpty) {
      return const Padding(
        padding: EdgeInsets.only(top: 40),
        child: Center(child: Text('No data')),
      );
    }

    return Column(
      children: stats.asMap().entries.map((entry) {
        final index = entry.key;
        final s = entry.value;
        final percentage = total > 0 ? (s.amount / total) : 0.0;
        final categoryColor = ChartUtil.getColor(index);

        return Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
          child: Card(
            elevation: 0,
            color: Theme.of(context).cardColor,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
              side: BorderSide(
                color: Theme.of(context).dividerColor.withOpacity(0.1),
              ),
            ),
            child: InkWell(
              borderRadius: BorderRadius.circular(16),
              onTap: () => onCategoryTap(s),
              child: Padding(
                padding: const EdgeInsets.all(12.0),
                child: Row(
                  children: [
                    WidgetUtil.textAvatar(
                      s.categoryName,
                      backgroundColor: categoryColor.withOpacity(0.8),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text(
                                s.categoryName,
                                style: const TextStyle(
                                  fontWeight: FontWeight.w600,
                                  fontSize: 16,
                                ),
                              ),
                              Text(
                                CommonUtil.toCurrency(s.amount),
                                style: TextStyle(
                                  fontWeight: FontWeight.bold,
                                  color: kindColor,
                                  fontSize: 16,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 8),
                          Row(
                            children: [
                              Expanded(
                                child: ClipRRect(
                                  borderRadius: BorderRadius.circular(4),
                                  child: LinearProgressIndicator(
                                    value: percentage,
                                    backgroundColor:
                                        categoryColor.withOpacity(0.1),
                                    valueColor: AlwaysStoppedAnimation<Color>(
                                      categoryColor,
                                    ),
                                    minHeight: 6,
                                  ),
                                ),
                              ),
                              const SizedBox(width: 12),
                              Text(
                                '${(percentage * 100).toStringAsFixed(1)}%',
                                style: TextStyle(
                                  fontSize: 12,
                                  color: Theme.of(context).hintColor,
                                  fontWeight: FontWeight.w500,
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        );
      }).toList(),
    );
  }
}
