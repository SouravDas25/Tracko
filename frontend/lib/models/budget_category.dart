class BudgetCategory {
  int categoryId;
  String categoryName;
  double allocatedAmount;
  double actualSpent;
  double remainingBalance;

  BudgetCategory({
    required this.categoryId,
    required this.categoryName,
    required this.allocatedAmount,
    required this.actualSpent,
    required this.remainingBalance,
  });

  factory BudgetCategory.fromJson(Map<String, dynamic> json) {
    return BudgetCategory(
      categoryId: json['categoryId'] as int,
      categoryName: json['categoryName'] as String,
      allocatedAmount: (json['allocatedAmount'] as num).toDouble(),
      actualSpent: (json['actualSpent'] as num).toDouble(),
      remainingBalance: (json['remainingBalance'] as num).toDouble(),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'categoryId': categoryId,
      'categoryName': categoryName,
      'allocatedAmount': allocatedAmount,
      'actualSpent': actualSpent,
      'remainingBalance': remainingBalance,
    };
  }
}
