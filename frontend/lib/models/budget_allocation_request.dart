class BudgetAllocationRequest {
  int month;
  int year;
  int categoryId;
  double amount;

  BudgetAllocationRequest({
    required this.month,
    required this.year,
    required this.categoryId,
    required this.amount,
  });

  Map<String, dynamic> toJson() {
    return {
      'month': month,
      'year': year,
      'categoryId': categoryId,
      'amount': amount,
    };
  }
}
