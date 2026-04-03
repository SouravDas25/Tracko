import 'package:flutter/material.dart';
import 'package:tracko/component/amount_text.dart';
import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:tracko/pages/stats_page/controllers/stats_controller.dart';
import 'package:tracko/Utils/ChartUtil.dart';

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
          child: Text(error!, style: const TextStyle(color: Colors.red)),
        ),
      );
    }

    if (stats.isEmpty) {
      return const Padding(
        padding: EdgeInsets.only(top: 40),
        child: Center(child: Text('No data')),
      );
    }

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: Text(
              'Categories',
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: Theme.of(context).hintColor,
                letterSpacing: 0.5,
              ),
            ),
          ),
          ...stats.asMap().entries.map((entry) {
            final index = entry.key;
            final s = entry.value;
            final percentage = total > 0 ? (s.amount / total) : 0.0;
            final categoryColor = ChartUtil.getColor(index);

            return Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Material(
                color: Theme.of(context).cardColor,
                borderRadius: BorderRadius.circular(14),
                child: InkWell(
                  borderRadius: BorderRadius.circular(14),
                  onTap: () => onCategoryTap(s),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 12, vertical: 10),
                    child: Row(
                      children: [
                        Container(
                          width: 4,
                          height: 36,
                          decoration: BoxDecoration(
                            color: categoryColor,
                            borderRadius: BorderRadius.circular(2),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceBetween,
                                children: [
                                  Flexible(
                                    child: Text(
                                      s.categoryName,
                                      style: const TextStyle(
                                        fontWeight: FontWeight.w600,
                                        fontSize: 15,
                                      ),
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  AmountText(
                                    amount: s.amount,
                                    color: kindColor,
                                    fontSize: 12,
                                  ),
                                ],
                              ),
                              const SizedBox(height: 6),
                              Row(
                                children: [
                                  Expanded(
                                    child: ClipRRect(
                                      borderRadius: BorderRadius.circular(3),
                                      child: LinearProgressIndicator(
                                        value: percentage,
                                        backgroundColor:
                                            categoryColor.withOpacity(0.12),
                                        valueColor:
                                            AlwaysStoppedAnimation<Color>(
                                                categoryColor),
                                        minHeight: 5,
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 10),
                                  SizedBox(
                                    width: 42,
                                    child: Text(
                                      '${(percentage * 100).toStringAsFixed(1)}%',
                                      style: TextStyle(
                                        fontSize: 11,
                                        color: Theme.of(context).hintColor,
                                        fontWeight: FontWeight.w500,
                                      ),
                                      textAlign: TextAlign.right,
                                    ),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(width: 4),
                        Icon(Icons.chevron_right,
                            size: 18,
                            color:
                                Theme.of(context).hintColor.withOpacity(0.4)),
                      ],
                    ),
                  ),
                ),
              ),
            );
          }),
        ],
      ),
    );
  }
}
