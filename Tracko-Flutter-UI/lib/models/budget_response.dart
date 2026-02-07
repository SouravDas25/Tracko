import 'package:tracko/models/budget_category.dart';

class BudgetResponse {
  int month;
  int year;
  double totalBudget;
  double totalSpent;
  double totalIncome;
  double rolloverAmount;
  double availableToAssign;
  bool isClosed;
  List<BudgetCategory> categories;

  BudgetResponse({
    required this.month,
    required this.year,
    required this.totalBudget,
    required this.totalSpent,
    required this.totalIncome,
    required this.rolloverAmount,
    required this.availableToAssign,
    required this.isClosed,
    required this.categories,
  });

  factory BudgetResponse.fromJson(Map<String, dynamic> json) {
    var categoriesList = json['categories'] as List;
    List<BudgetCategory> categories = categoriesList
        .map((i) => BudgetCategory.fromJson(i as Map<String, dynamic>))
        .toList();

    return BudgetResponse(
      month: json['month'] as int,
      year: json['year'] as int,
      totalBudget: (json['totalBudget'] as num).toDouble(),
      totalSpent: (json['totalSpent'] as num).toDouble(),
      totalIncome: (json['totalIncome'] as num?)?.toDouble() ?? 0.0,
      rolloverAmount: (json['rolloverAmount'] as num).toDouble(),
      availableToAssign: (json['availableToAssign'] as num).toDouble(),
      isClosed: json['isClosed'] as bool? ?? false,
      categories: categories,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'month': month,
      'year': year,
      'totalBudget': totalBudget,
      'totalSpent': totalSpent,
      'totalIncome': totalIncome,
      'rolloverAmount': rolloverAmount,
      'availableToAssign': availableToAssign,
      'isClosed': isClosed,
      'categories': categories.map((e) => e.toJson()).toList(),
    };
  }
}
