import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import 'package:tracko/di/di.dart';
import 'package:tracko/exceptions/search_exception.dart';
import 'package:tracko/models/search_filters.dart';
import 'package:tracko/models/search_hit.dart';
import 'package:tracko/models/search_result.dart';
import 'package:tracko/repositories/search_repository.dart';

/// Manages search state and business logic for the transaction search feature.
///
/// This class handles:
/// - Query state with 300ms debounce for input
/// - Filter management with auto-trigger search on filter changes
/// - Results pagination with infinite scroll support
/// - Request cancellation for in-flight requests
/// - Error handling and retry logic
///
/// Validates: Requirements 1.4, 1.7, 4.1, 4.2, 4.4, 5.9, 6.4, 6.5, 10.1, 10.2, 10.3
class SearchState extends ChangeNotifier {
  // Dependencies
  final SearchRepository _repository;

  // Query state
  String _query = '';
  Timer? _debounceTimer;

  // Filter state
  SearchFilters _filters = SearchFilters.empty;

  // Results state
  List<SearchHit> _results = [];
  bool _isLoading = false;
  bool _hasMore = true;
  int _currentPage = 0;
  String? _errorMessage;
  int? _searchTimeMs;
  int _totalResults = 0;

  // Cancellation
  CancelToken? _cancelToken;

  /// Creates a SearchState with an optional repository dependency.
  ///
  /// If no repository is provided, uses the registered instance from DI.
  SearchState({SearchRepository? repository})
      : _repository = repository ?? sl<SearchRepository>();

  // ============================================================
  // Public Getters
  // ============================================================

  /// The current search query string.
  String get query => _query;

  /// The current filter configuration.
  SearchFilters get filters => _filters;

  /// The list of search results.
  List<SearchHit> get results => List.unmodifiable(_results);

  /// Whether a search request is currently in progress.
  bool get isLoading => _isLoading;

  /// Whether more results are available for pagination.
  bool get hasMore => _hasMore;

  /// Whether an error occurred during the last search.
  bool get hasError => _errorMessage != null;

  /// The error message from the last failed search, if any.
  String? get errorMessage => _errorMessage;

  /// The search execution time in milliseconds from the last successful search.
  int? get searchTimeMs => _searchTimeMs;

  /// The total number of matching results.
  int get totalResults => _totalResults;

  /// Whether there are any results in the current result set.
  bool get hasResults => _results.isNotEmpty;

  /// Whether the search has completed with no results for a non-empty query.
  ///
  /// Returns true only when:
  /// - Not currently loading
  /// - Results are empty
  /// - Query is non-empty (user has searched for something)
  bool get isEmpty => !_isLoading && _results.isEmpty && _query.isNotEmpty;

  /// The number of currently active filters.
  int get activeFilterCount => _filters.activeCount;

  // ============================================================
  // Query Management
  // ============================================================

  /// Sets the search query with 300ms debounce.
  ///
  /// If the query is empty, clears results and error state immediately.
  /// If the query changes, cancels any pending debounce timer and starts
  /// a new one that will trigger a search after 300ms.
  ///
  /// Validates: Requirements 1.4, 10.1, 10.2
  void setQuery(String query) {
    if (_query == query) return;

    _query = query;
    _debounceTimer?.cancel();

    if (query.isEmpty) {
      // Clear results and error immediately for empty query
      _results = [];
      _errorMessage = null;
      _searchTimeMs = null;
      _totalResults = 0;
      _hasMore = true;
      _currentPage = 0;
      notifyListeners();
      return;
    }

    // Start debounce timer
    _debounceTimer = Timer(const Duration(milliseconds: 300), () {
      search(reset: true);
    });

    notifyListeners();
  }

  /// Clears the query, results, and error state.
  ///
  /// Cancels any pending debounce timer and resets the search state
  /// to its initial empty state.
  ///
  /// Validates: Requirements 1.7
  void clearQuery() {
    _debounceTimer?.cancel();
    _cancelToken?.cancel();

    _query = '';
    _results = [];
    _errorMessage = null;
    _searchTimeMs = null;
    _totalResults = 0;
    _hasMore = true;
    _isLoading = false;
    _currentPage = 0;

    notifyListeners();
  }

  // ============================================================
  // Filter Management
  // ============================================================

  /// Updates the search filters and triggers a new search.
  ///
  /// If the filters have changed, automatically executes a new search
  /// with the updated filter configuration.
  ///
  /// Validates: Requirements 5.9
  void updateFilters(SearchFilters filters) {
    if (_filters == filters) return;

    _filters = filters;

    // Auto-trigger search if there's a query
    if (_query.isNotEmpty) {
      search(reset: true);
    } else {
      notifyListeners();
    }
  }

  /// Clears all filters and triggers a new search.
  ///
  /// Resets filters to empty and executes a new search if there's
  /// an active query.
  ///
  /// Validates: Requirements 5.8, 5.9
  void clearFilters() {
    if (_filters == SearchFilters.empty) return;

    _filters = SearchFilters.empty;

    // Auto-trigger search if there's a query
    if (_query.isNotEmpty) {
      search(reset: true);
    } else {
      notifyListeners();
    }
  }

  // ============================================================
  // Search Operations
  // ============================================================

  /// Executes a search with the current query and filters.
  ///
  /// Parameters:
  /// - [reset]: If true, starts from page 0 and clears existing results.
  ///            If false, appends to existing results (for pagination).
  ///
  /// Cancels any in-flight request before starting a new one.
  /// Handles errors and updates error state appropriately.
  ///
  /// Validates: Requirements 4.1, 4.2, 4.4, 10.3
  Future<void> search({bool reset = true}) async {
    // Don't search with empty query
    if (_query.isEmpty) {
      return;
    }

    // Cancel any in-flight request
    _cancelToken?.cancel();
    _cancelToken = CancelToken();

    if (reset) {
      _currentPage = 0;
      _results = [];
      _hasMore = true;
    }

    _isLoading = true;
    _errorMessage = null;
    notifyListeners();

    try {
      final result = await _repository.search(
        query: _query,
        filters: _filters,
        page: _currentPage,
        cancelToken: _cancelToken,
      );

      // Check if request was cancelled while we were waiting
      if (_cancelToken?.isCancelled ?? false) {
        return;
      }

      if (reset) {
        _results = result.results;
      } else {
        _results = [..._results, ...result.results];
      }

      _hasMore = result.hasNext;
      _searchTimeMs = result.searchTimeMs;
      _totalResults = result.totalResults;
      _currentPage = result.page;
      _errorMessage = null;
    } on SearchException catch (e) {
      // Don't show error for cancelled requests
      if (e.type != SearchErrorType.cancelled) {
        _errorMessage = e.message;
      }
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  /// Loads the next page of results.
  ///
  /// Increments the current page and appends results to the existing list.
  /// Does nothing if already loading or no more results are available.
  ///
  /// Validates: Requirements 4.2, 4.4
  Future<void> loadMore() async {
    // Don't load more if already loading or no more results
    if (_isLoading || !_hasMore) {
      return;
    }

    // Increment page for next request
    _currentPage++;

    await search(reset: false);
  }

  /// Retries the last failed search with the same query and filters.
  ///
  /// If the search was successful, this is a no-op.
  /// If an error occurred, re-executes the search from page 0.
  ///
  /// Validates: Requirements 6.4, 6.5
  Future<void> retry() async {
    // Only retry if there was an error
    if (!hasError) {
      return;
    }

    // Clear error and retry from beginning
    _errorMessage = null;
    await search(reset: true);
  }

  // ============================================================
  // Lifecycle
  // ============================================================

  /// Disposes resources held by this state.
  ///
  /// Cancels any pending debounce timer and in-flight requests.
  @override
  void dispose() {
    _debounceTimer?.cancel();
    _cancelToken?.cancel();
    super.dispose();
  }
}
