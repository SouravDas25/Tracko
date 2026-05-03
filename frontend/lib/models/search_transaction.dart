import 'package:equatable/equatable.dart';

import 'package:tracko/models/search_account.dart';
import 'package:tracko/models/search_category.dart';

/// Represents a transaction in search results with lightweight nested objects.
/// 
/// Validates: Requirements 7.3, 7.4, 5.6
class SearchTransaction extends Equatable {
  final int id;
  final int transactionType;
  final String name;
  final String? comments;
  final DateTime date;
  final double amount;
  final String? originalCurrency;
  final double? originalAmount;
  final int accountId;
  final int categoryId;
  final SearchCategory? category;
  final SearchAccount? account;

  const SearchTransaction({
    required this.id,
    required this.transactionType,
    required this.name,
    this.comments,
    required this.date,
    required this.amount,
    this.originalCurrency,
    this.originalAmount,
    required this.accountId,
    required this.categoryId,
    this.category,
    this.account,
  });

  /// Returns true if this transaction is a debit (transactionType == 1).
  bool get isDebit => transactionType == 1;

  /// Returns true if this transaction is a credit (transactionType == 2).
  bool get isCredit => transactionType == 2;

  /// Returns true if this transaction is a transfer (transactionType == 3).
  bool get isTransfer => transactionType == 3;

  factory SearchTransaction.fromJson(Map<String, dynamic> json) {
    return SearchTransaction(
      id: json['id'] as int,
      transactionType: json['transactionType'] as int? ?? 1,
      name: json['name'] as String? ?? '',
      comments: json['comments'] as String?,
      date: json['date'] != null 
          ? DateTime.parse(json['date'] as String) 
          : DateTime.now(),
      amount: (json['amount'] as num?)?.toDouble() ?? 0.0,
      originalCurrency: json['originalCurrency'] as String?,
      originalAmount: json['originalAmount'] != null 
          ? (json['originalAmount'] as num).toDouble() 
          : null,
      accountId: json['accountId'] as int? ?? 0,
      categoryId: json['categoryId'] as int? ?? 0,
      category: json['category'] != null 
          ? SearchCategory.fromJson(json['category'] as Map<String, dynamic>) 
          : null,
      account: json['account'] != null 
          ? SearchAccount.fromJson(json['account'] as Map<String, dynamic>) 
          : null,
    );
  }

  SearchTransaction copyWith({
    int? id,
    int? transactionType,
    String? name,
    String? comments,
    DateTime? date,
    double? amount,
    String? originalCurrency,
    double? originalAmount,
    int? accountId,
    int? categoryId,
    SearchCategory? category,
    SearchAccount? account,
  }) {
    return SearchTransaction(
      id: id ?? this.id,
      transactionType: transactionType ?? this.transactionType,
      name: name ?? this.name,
      comments: comments ?? this.comments,
      date: date ?? this.date,
      amount: amount ?? this.amount,
      originalCurrency: originalCurrency ?? this.originalCurrency,
      originalAmount: originalAmount ?? this.originalAmount,
      accountId: accountId ?? this.accountId,
      categoryId: categoryId ?? this.categoryId,
      category: category ?? this.category,
      account: account ?? this.account,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'transactionType': transactionType,
      'name': name,
      'comments': comments,
      'date': date.toIso8601String(),
      'amount': amount,
      'originalCurrency': originalCurrency,
      'originalAmount': originalAmount,
      'accountId': accountId,
      'categoryId': categoryId,
      'category': category?.toJson(),
      'account': account?.toJson(),
    };
  }

  @override
  List<Object?> get props => [
    id,
    transactionType,
    name,
    comments,
    date,
    amount,
    originalCurrency,
    originalAmount,
    accountId,
    categoryId,
    category,
    account,
  ];

  @override
  String toString() {
    return 'SearchTransaction{id: $id, transactionType: $transactionType, name: $name, amount: $amount, date: $date}';
  }
}
