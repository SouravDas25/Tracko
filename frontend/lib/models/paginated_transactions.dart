import 'package:tracko/models/transaction.dart';

class PaginatedTransactions {
  final List<Transaction> transactions;
  final bool hasNext;
  final bool hasPrevious;
  final int page;
  final int size;
  final int totalPages;
  final int totalElements;

  PaginatedTransactions({
    required this.transactions,
    required this.hasNext,
    required this.hasPrevious,
    required this.page,
    required this.size,
    required this.totalPages,
    required this.totalElements,
  });

  factory PaginatedTransactions.empty() {
    return PaginatedTransactions(
      transactions: [],
      hasNext: false,
      hasPrevious: false,
      page: 0,
      size: 0,
      totalPages: 0,
      totalElements: 0,
    );
  }
}
