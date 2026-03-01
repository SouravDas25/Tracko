class TransactionPeriodSummary {
  final double income;
  final double expense;
  final double netTotal;
  final int count;
  final int year;
  final int? month;

  TransactionPeriodSummary({
    required this.income,
    required this.expense,
    required this.netTotal,
    required this.count,
    required this.year,
    this.month,
  });

  factory TransactionPeriodSummary.fromJson(Map<String, dynamic> json) {
    return TransactionPeriodSummary(
      income: (json['totalIncome'] as num?)?.toDouble() ?? 0.0,
      expense: (json['totalExpense'] as num?)?.toDouble() ?? 0.0,
      netTotal: (json['netTotal'] as num?)?.toDouble() ?? 0.0,
      count: (json['transactionCount'] as num?)?.toInt() ?? 0,
      year: (json['year'] as num?)?.toInt() ?? 0,
      month: (json['month'] as num?)?.toInt(),
    );
  }
}
