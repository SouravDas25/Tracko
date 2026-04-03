import 'package:flutter/material.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/component/amount_text.dart';
import 'package:tracko/models/budget_category.dart';
import 'package:percent_indicator/linear_percent_indicator.dart';
import 'dart:math' as math;

class BudgetCategoryTile extends StatelessWidget {
  final BudgetCategory category;
  final VoidCallback onTap;

  const BudgetCategoryTile({
    Key? key,
    required this.category,
    required this.onTap,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    // Determine the max value for the scale (max of budget or spent)
    double maxValue = math.max(category.allocatedAmount, category.actualSpent);
    if (maxValue == 0) maxValue = 1.0; // Avoid division by zero

    double budgetPercent =
        (category.allocatedAmount / maxValue).clamp(0.0, 1.0);
    double spentPercent = (category.actualSpent / maxValue).clamp(0.0, 1.0);

    // Spent color: Green if under/equal budget, Red if over
    Color spentColor = category.actualSpent > category.allocatedAmount
        ? Colors.red
        : Colors.green;

    return InkWell(
      onTap: onTap,
      child: Container(
        padding: EdgeInsets.symmetric(horizontal: 16, vertical: 10),
        decoration: BoxDecoration(
          border: Border(
            bottom: BorderSide(
              color: Theme.of(context).dividerColor.withOpacity(0.08),
              width: 0.5,
            ),
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  category.categoryName,
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                Text(
                  category.remainingBalance >= 0
                      ? '${CommonUtil.toCurrency(category.remainingBalance)} left'
                      : '${CommonUtil.toCurrency(category.remainingBalance.abs())} over',
                  style: TextStyle(
                    fontSize: 11,
                    color: category.remainingBalance >= 0
                        ? Colors.green[700]
                        : Colors.red[700],
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
            SizedBox(height: 6),
            Row(
              children: [
                Text("Budget ", style: TextStyle(fontSize: 10, color: Colors.grey[600])),
                Text(CommonUtil.toCurrency(category.allocatedAmount),
                    style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold)),
                Spacer(),
                Text("Spent ", style: TextStyle(fontSize: 10, color: Colors.grey[600])),
                Text(CommonUtil.toCurrency(category.actualSpent),
                    style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: spentColor)),
              ],
            ),
            SizedBox(height: 4),
            ClipRRect(
              borderRadius: BorderRadius.circular(3),
              child: SizedBox(
                height: 5,
                child: Stack(
                  children: [
                    LinearPercentIndicator(
                      lineHeight: 5.0,
                      percent: budgetPercent,
                      backgroundColor: Colors.grey[800],
                      progressColor: Colors.blue[300]!.withOpacity(0.3),
                      padding: EdgeInsets.zero,
                      barRadius: Radius.circular(3),
                    ),
                    LinearPercentIndicator(
                      lineHeight: 5.0,
                      percent: spentPercent,
                      backgroundColor: Colors.transparent,
                      progressColor: spentColor,
                      padding: EdgeInsets.zero,
                      barRadius: Radius.circular(3),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
