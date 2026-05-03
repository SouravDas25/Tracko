import 'package:equatable/equatable.dart';

import 'package:tracko/models/search_transaction.dart';

/// Represents a single search hit with relevance scoring and highlighting.
/// 
/// Validates: Requirements 7.3, 7.4
class SearchHit extends Equatable {
  final SearchTransaction transaction;
  final double relevanceScore;
  final Map<String, String> highlights;
  final List<String> matchedFields;

  const SearchHit({
    required this.transaction,
    required this.relevanceScore,
    required this.highlights,
    required this.matchedFields,
  });

  /// Returns the highlighted snippet for the given field, or null if not available.
  String? getHighlight(String field) => highlights[field];

  /// Returns true if the given field was matched in the search query.
  bool isFieldMatched(String field) => matchedFields.contains(field);

  factory SearchHit.fromJson(Map<String, dynamic> json) {
    return SearchHit(
      transaction: SearchTransaction.fromJson(
        json['transaction'] as Map<String, dynamic>,
      ),
      relevanceScore: (json['relevanceScore'] as num?)?.toDouble() ?? 0.0,
      highlights: json['highlights'] != null
          ? Map<String, String>.from(json['highlights'] as Map)
          : {},
      matchedFields: json['matchedFields'] != null
          ? List<String>.from(json['matchedFields'] as List)
          : [],
    );
  }

  SearchHit copyWith({
    SearchTransaction? transaction,
    double? relevanceScore,
    Map<String, String>? highlights,
    List<String>? matchedFields,
  }) {
    return SearchHit(
      transaction: transaction ?? this.transaction,
      relevanceScore: relevanceScore ?? this.relevanceScore,
      highlights: highlights ?? this.highlights,
      matchedFields: matchedFields ?? this.matchedFields,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'transaction': transaction.toJson(),
      'relevanceScore': relevanceScore,
      'highlights': highlights,
      'matchedFields': matchedFields,
    };
  }

  @override
  List<Object?> get props => [transaction, relevanceScore, highlights, matchedFields];

  @override
  String toString() {
    return 'SearchHit{transactionId: ${transaction.id}, relevanceScore: $relevanceScore, matchedFields: $matchedFields}';
  }
}
