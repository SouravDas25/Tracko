import 'package:equatable/equatable.dart';

import 'package:tracko/models/search_hit.dart';

/// Represents paginated search results with metadata.
/// 
/// Validates: Requirements 7.3, 7.4
class SearchResult extends Equatable {
  final List<SearchHit> results;
  final int totalResults;
  final int page;
  final int size;
  final int totalPages;
  final bool hasNext;
  final bool hasPrevious;
  final int searchTimeMs;
  final String query;

  const SearchResult({
    required this.results,
    required this.totalResults,
    required this.page,
    required this.size,
    required this.totalPages,
    required this.hasNext,
    required this.hasPrevious,
    required this.searchTimeMs,
    required this.query,
  });

  /// Creates an empty SearchResult for initial state.
  factory SearchResult.empty() {
    return const SearchResult(
      results: [],
      totalResults: 0,
      page: 0,
      size: 20,
      totalPages: 0,
      hasNext: false,
      hasPrevious: false,
      searchTimeMs: 0,
      query: '',
    );
  }

  factory SearchResult.fromJson(Map<String, dynamic> json) {
    var resultsList = json['results'] as List? ?? [];
    List<SearchHit> hits = resultsList
        .map((i) => SearchHit.fromJson(i as Map<String, dynamic>))
        .toList();

    return SearchResult(
      results: hits,
      totalResults: json['totalResults'] as int? ?? 0,
      page: json['page'] as int? ?? 0,
      size: json['size'] as int? ?? 20,
      totalPages: json['totalPages'] as int? ?? 0,
      hasNext: json['hasNext'] as bool? ?? false,
      hasPrevious: json['hasPrevious'] as bool? ?? false,
      searchTimeMs: json['searchTimeMs'] as int? ?? 0,
      query: json['query'] as String? ?? '',
    );
  }

  SearchResult copyWith({
    List<SearchHit>? results,
    int? totalResults,
    int? page,
    int? size,
    int? totalPages,
    bool? hasNext,
    bool? hasPrevious,
    int? searchTimeMs,
    String? query,
  }) {
    return SearchResult(
      results: results ?? this.results,
      totalResults: totalResults ?? this.totalResults,
      page: page ?? this.page,
      size: size ?? this.size,
      totalPages: totalPages ?? this.totalPages,
      hasNext: hasNext ?? this.hasNext,
      hasPrevious: hasPrevious ?? this.hasPrevious,
      searchTimeMs: searchTimeMs ?? this.searchTimeMs,
      query: query ?? this.query,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'results': results.map((e) => e.toJson()).toList(),
      'totalResults': totalResults,
      'page': page,
      'size': size,
      'totalPages': totalPages,
      'hasNext': hasNext,
      'hasPrevious': hasPrevious,
      'searchTimeMs': searchTimeMs,
      'query': query,
    };
  }

  @override
  List<Object?> get props => [
    results,
    totalResults,
    page,
    size,
    totalPages,
    hasNext,
    hasPrevious,
    searchTimeMs,
    query,
  ];

  @override
  String toString() {
    return 'SearchResult{query: $query, totalResults: $totalResults, page: $page/$totalPages, searchTimeMs: ${searchTimeMs}ms}';
  }
}
