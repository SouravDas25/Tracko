import 'package:equatable/equatable.dart';

/// Represents filter criteria for transaction search.
/// 
/// Validates: Requirements 7.3, 7.4
class SearchFilters extends Equatable {
  final DateTime? startDate;
  final DateTime? endDate;
  final double? minAmount;
  final double? maxAmount;
  final List<int>? accountIds;
  final int? categoryId;

  const SearchFilters({
    this.startDate,
    this.endDate,
    this.minAmount,
    this.maxAmount,
    this.accountIds,
    this.categoryId,
  });

  /// Returns the number of non-null filter values that are actively filtering.
  int get activeCount {
    int count = 0;
    if (startDate != null) count++;
    if (endDate != null) count++;
    if (minAmount != null) count++;
    if (maxAmount != null) count++;
    if (accountIds != null && accountIds!.isNotEmpty) count++;
    if (categoryId != null) count++;
    return count;
  }

  /// Returns true if any filter is active.
  bool get hasActiveFilters => activeCount > 0;

  factory SearchFilters.fromJson(Map<String, dynamic> json) {
    return SearchFilters(
      startDate: json['startDate'] != null 
          ? DateTime.parse(json['startDate'] as String) 
          : null,
      endDate: json['endDate'] != null 
          ? DateTime.parse(json['endDate'] as String) 
          : null,
      minAmount: json['minAmount'] != null 
          ? (json['minAmount'] as num).toDouble() 
          : null,
      maxAmount: json['maxAmount'] != null 
          ? (json['maxAmount'] as num).toDouble() 
          : null,
      accountIds: json['accountIds'] != null 
          ? List<int>.from(json['accountIds'] as List) 
          : null,
      categoryId: json['categoryId'] as int?,
    );
  }

  SearchFilters copyWith({
    DateTime? startDate,
    DateTime? endDate,
    double? minAmount,
    double? maxAmount,
    List<int>? accountIds,
    int? categoryId,
    bool clearStartDate = false,
    bool clearEndDate = false,
    bool clearMinAmount = false,
    bool clearMaxAmount = false,
    bool clearAccountIds = false,
    bool clearCategoryId = false,
  }) {
    return SearchFilters(
      startDate: clearStartDate ? null : (startDate ?? this.startDate),
      endDate: clearEndDate ? null : (endDate ?? this.endDate),
      minAmount: clearMinAmount ? null : (minAmount ?? this.minAmount),
      maxAmount: clearMaxAmount ? null : (maxAmount ?? this.maxAmount),
      accountIds: clearAccountIds ? null : (accountIds ?? this.accountIds),
      categoryId: clearCategoryId ? null : (categoryId ?? this.categoryId),
    );
  }

  /// Converts filters to query parameters for API requests.
  /// Only non-null values are included.
  Map<String, dynamic> toQueryParams() {
    final params = <String, dynamic>{};
    
    if (startDate != null) {
      params['startDate'] = startDate!.toIso8601String().split('T')[0];
    }
    if (endDate != null) {
      params['endDate'] = endDate!.toIso8601String().split('T')[0];
    }
    if (minAmount != null) {
      params['minAmount'] = minAmount;
    }
    if (maxAmount != null) {
      params['maxAmount'] = maxAmount;
    }
    if (accountIds != null && accountIds!.isNotEmpty) {
      params['accountIds'] = accountIds!.join(',');
    }
    if (categoryId != null) {
      params['categoryId'] = categoryId;
    }
    
    return params;
  }

  Map<String, dynamic> toJson() {
    return {
      'startDate': startDate?.toIso8601String(),
      'endDate': endDate?.toIso8601String(),
      'minAmount': minAmount,
      'maxAmount': maxAmount,
      'accountIds': accountIds,
      'categoryId': categoryId,
    };
  }

  /// Creates empty filters with no active constraints.
  static const empty = SearchFilters();

  @override
  List<Object?> get props => [
    startDate,
    endDate,
    minAmount,
    maxAmount,
    accountIds,
    categoryId,
  ];

  @override
  String toString() {
    return 'SearchFilters{activeCount: $activeCount, startDate: $startDate, endDate: $endDate, minAmount: $minAmount, maxAmount: $maxAmount, accountIds: $accountIds, categoryId: $categoryId}';
  }
}
