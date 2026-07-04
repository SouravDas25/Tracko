/// One page of transaction history, as returned by the paged
/// `GET /api/transactions/history` endpoint.
class TransactionHistoryPageResult {
  final List<TransactionHistoryEntry> entries;
  final int page;
  final int totalPages;
  final int totalElements;
  final bool hasNext;
  final bool hasPrevious;

  TransactionHistoryPageResult({
    required this.entries,
    required this.page,
    required this.totalPages,
    required this.totalElements,
    required this.hasNext,
    required this.hasPrevious,
  });

  factory TransactionHistoryPageResult.fromJson(Map<String, dynamic> json) {
    final rows = (json['history'] as List<dynamic>?) ?? const <dynamic>[];
    return TransactionHistoryPageResult(
      entries: rows
          .map((e) =>
              TransactionHistoryEntry.fromJson(e as Map<String, dynamic>))
          .toList(),
      page: (json['page'] as num?)?.toInt() ?? 0,
      totalPages: (json['totalPages'] as num?)?.toInt() ?? 0,
      totalElements: (json['totalElements'] as num?)?.toInt() ?? 0,
      hasNext: json['hasNext'] as bool? ?? false,
      hasPrevious: json['hasPrevious'] as bool? ?? false,
    );
  }
}

/// One before → after field change within an edit, used to render the history diff
/// (e.g. `Type: DEBIT → CREDIT`). Values are already formatted by the backend.
class HistoryFieldChange {
  final String field;
  final String? before;
  final String? after;

  HistoryFieldChange({required this.field, this.before, this.after});

  factory HistoryFieldChange.fromJson(Map<String, dynamic> json) =>
      HistoryFieldChange(
        field: (json['field'] ?? '').toString(),
        before: json['before']?.toString(),
        after: json['after']?.toString(),
      );
}

/// A single change-history entry for a transaction, as returned by
/// `GET /api/transactions/{id}/history` and (inside a page) `GET /api/transactions/history`.
class TransactionHistoryEntry {
  final int? id;
  final int? transactionId;
  final String? operation; // CREATE | UPDATE | DELETE | REVERT
  final DateTime? changedAt;
  final int? linkedTransactionId;
  final String? name;
  final double? amount;
  final String? originalCurrency;
  final DateTime? date;
  final int? transactionType;

  /// For UPDATE entries in a single transaction's timeline: the fields this edit changed.
  /// Empty for other operations and for the global/recycle-bin listings.
  final List<HistoryFieldChange> changes;

  TransactionHistoryEntry({
    this.id,
    this.transactionId,
    this.operation,
    this.changedAt,
    this.linkedTransactionId,
    this.name,
    this.amount,
    this.originalCurrency,
    this.date,
    this.transactionType,
    this.changes = const [],
  });

  factory TransactionHistoryEntry.fromJson(Map<String, dynamic> json) {
    DateTime? parseDate(dynamic v) =>
        v == null ? null : DateTime.tryParse(v.toString());
    return TransactionHistoryEntry(
      id: (json['id'] as num?)?.toInt(),
      transactionId: (json['transactionId'] as num?)?.toInt(),
      operation: json['operation'] as String?,
      changedAt: parseDate(json['changedAt']),
      linkedTransactionId: (json['linkedTransactionId'] as num?)?.toInt(),
      name: json['name'] as String?,
      amount: (json['amount'] as num?)?.toDouble(),
      originalCurrency: json['originalCurrency'] as String?,
      date: parseDate(json['date']),
      transactionType: (json['transactionType'] as num?)?.toInt(),
      changes: ((json['changes'] as List<dynamic>?) ?? const <dynamic>[])
          .map((e) => HistoryFieldChange.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}
