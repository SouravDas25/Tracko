import 'package:flutter/material.dart';
import 'package:tracko/Utils/CommonUtil.dart';
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

    return Card(
      margin: EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      elevation: 2,
      child: InkWell(
        onTap: onTap,
        child: Padding(
          padding: EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    category.categoryName,
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  Container(
                    padding: EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: category.remainingBalance >= 0
                          ? Colors.green.withOpacity(0.1)
                          : Colors.red.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      category.remainingBalance >= 0
                          ? '${CommonUtil.toCurrency(category.remainingBalance)} left'
                          : '${CommonUtil.toCurrency(category.remainingBalance.abs())} over',
                      style: TextStyle(
                        color: category.remainingBalance >= 0
                            ? Colors.green[700]
                            : Colors.red[700],
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ],
              ),
              SizedBox(height: 12),
              // Budget Bar
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text("Budget",
                          style:
                              TextStyle(fontSize: 12, color: Colors.grey[600])),
                      Text(CommonUtil.toCurrency(category.allocatedAmount),
                          style: TextStyle(
                              fontSize: 12, fontWeight: FontWeight.bold)),
                    ],
                  ),
                  SizedBox(height: 4),
                  LinearPercentIndicator(
                    lineHeight: 8.0,
                    percent: budgetPercent,
                    backgroundColor: Colors.grey[200],
                    progressColor: Colors.blue[300],
                    padding: EdgeInsets.zero,
                    barRadius: Radius.circular(4),
                    animation: true,
                  ),
                ],
              ),
              SizedBox(height: 12),
              // Spent Bar
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text("Spent",
                          style:
                              TextStyle(fontSize: 12, color: Colors.grey[600])),
                      Text(CommonUtil.toCurrency(category.actualSpent),
                          style: TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.bold,
                              color: spentColor)),
                    ],
                  ),
                  SizedBox(height: 4),
                  LinearPercentIndicator(
                    lineHeight: 8.0,
                    percent: spentPercent,
                    backgroundColor: Colors.grey[200],
                    progressColor: spentColor,
                    padding: EdgeInsets.zero,
                    barRadius: Radius.circular(4),
                    animation: true,
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
