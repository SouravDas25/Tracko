import 'package:flutter/material.dart';

import 'package:tracko/di/di.dart';
import 'package:tracko/models/search_hit.dart';
import 'package:tracko/pages/add_item_page/add_item.dart';
import 'package:tracko/pages/search_page/filter_panel.dart';
import 'package:tracko/pages/search_page/search_hit_tile.dart';
import 'package:tracko/pages/search_page/search_state.dart';
import 'package:tracko/repositories/transaction_repository.dart';

/// The main search page for finding transactions.
///
/// Provides a full-text fuzzy search interface with:
/// - Search input with auto-focus and clear button
/// - Collapsible filter panel (date range, amount, accounts, category)
/// - Paginated results with infinite scroll
/// - Pull-to-refresh functionality
/// - State persistence across navigation using AutomaticKeepAliveClientMixin
///
/// Validates: Requirements 1.1, 1.2, 1.3, 1.5, 1.6, 1.7, 2.1, 2.4, 2.5,
///            4.1, 4.2, 4.3, 4.4, 4.5, 6.1, 6.2, 6.3, 6.4, 6.5,
///            8.4, 10.4, 10.5
class SearchPage extends StatefulWidget {
  const SearchPage({Key? key}) : super(key: key);

  @override
  State<SearchPage> createState() => _SearchPageState();
}

class _SearchPageState extends State<SearchPage>
    with AutomaticKeepAliveClientMixin {
  // Search state management
  late final SearchState _searchState;

  // Text editing controller for search input
  late final TextEditingController _searchController;

  // Focus node for auto-focusing search input
  late final FocusNode _searchFocusNode;

  // Scroll controller for infinite scroll detection
  late final ScrollController _scrollController;

  // Refresh indicator key for pull-to-refresh
  final GlobalKey<RefreshIndicatorState> _refreshIndicatorKey =
      GlobalKey<RefreshIndicatorState>();

  @override
  bool get wantKeepAlive => true;

  @override
  void initState() {
    super.initState();

    // Initialize search state
    _searchState = SearchState();

    // Initialize controllers
    _searchController = TextEditingController();
    _searchFocusNode = FocusNode();
    _scrollController = ScrollController();

    // Add scroll listener for infinite scroll
    _scrollController.addListener(_onScroll);

    // Auto-focus search input after frame is built
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _searchFocusNode.requestFocus();
    });
  }

  @override
  void dispose() {
    _searchState.dispose();
    _searchController.dispose();
    _searchFocusNode.dispose();
    _scrollController.removeListener(_onScroll);
    _scrollController.dispose();
    super.dispose();
  }

  // ============================================================
  // Scroll Handling
  // ============================================================

  /// Handles scroll events to detect when to load more results.
  void _onScroll() {
    if (!_scrollController.hasClients) return;

    final maxScroll = _scrollController.position.maxScrollExtent;
    final currentScroll = _scrollController.position.pixels;

    // Load more when within 200 pixels of bottom
    const threshold = 200.0;

    if (currentScroll >= maxScroll - threshold &&
        !_searchState.isLoading &&
        _searchState.hasMore) {
      _searchState.loadMore();
    }
  }

  // ============================================================
  // Search Input Handling
  // ============================================================

  /// Handles search input changes.
  void _onSearchChanged(String value) {
    _searchState.setQuery(value);
  }

  /// Clears the search query and results.
  void _onClearSearch() {
    _searchController.clear();
    _searchState.clearQuery();
    _searchFocusNode.requestFocus();
  }

  // ============================================================
  // Filter Handling
  // ============================================================

  /// Handles filter changes from the filter panel.
  void _onFiltersChanged(filters) {
    _searchState.updateFilters(filters);
  }

  /// Handles clear filters button tap.
  void _onClearFilters() {
    _searchState.clearFilters();
  }

  // ============================================================
  // Pull-to-Refresh
  // ============================================================

  /// Handles pull-to-refresh by reloading results from page 0.
  Future<void> _onRefresh() async {
    if (_searchState.query.isNotEmpty) {
      await _searchState.search(reset: true);
    }
  }

  // ============================================================
  // Search Hit Tap Handling
  // ============================================================

  /// Handles tap on a search hit tile.
  ///
  /// Fetches full transaction details, navigates to AddItemPage,
  /// and refreshes search results on save.
  Future<void> _onSearchHitTap(SearchHit hit) async {
    try {
      // Show loading indicator
      showDialog(
        context: context,
        barrierDismissible: false,
        builder: (context) => const Center(
          child: CircularProgressIndicator(),
        ),
      );

      // Fetch full transaction for editing
      final transaction =
          await sl<TransactionRepository>().getById(hit.transaction.id);

      // Close loading indicator
      if (mounted) {
        Navigator.of(context).pop();
      }

      // Navigate to AddItemPage
      final saved = await Navigator.of(context).push<bool>(
        MaterialPageRoute(
          builder: (context) => AddItemPage(transaction: transaction),
        ),
      );

      // Refresh search results on save
      if (saved == true && mounted && _searchState.query.isNotEmpty) {
        _searchState.search(reset: true);
      }
    } catch (e) {
      // Close loading indicator if still showing
      if (mounted && Navigator.canPop(context)) {
        Navigator.of(context).pop();
      }

      // Show error snackbar
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Failed to load transaction: $e'),
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    }
  }

  // ============================================================
  // Retry Handling
  // ============================================================

  /// Handles retry button tap on error state.
  void _onRetry() {
    _searchState.retry();
  }

  // ============================================================
  // Build Methods
  // ============================================================

  @override
  Widget build(BuildContext context) {
    super.build(context);

    return Scaffold(
      appBar: _buildAppBar(context),
      body: ListenableBuilder(
        listenable: _searchState,
        builder: (context, child) {
          return Column(
            children: [
              // Filter Panel
              FilterPanel(
                filters: _searchState.filters,
                onFiltersChanged: _onFiltersChanged,
                onClearFilters: _onClearFilters,
              ),

              // Search Meta Bar (when results exist)
              if (_searchState.hasResults) _buildSearchMetaBar(context),

              // Search Results
              Expanded(
                child: _buildSearchResults(context),
              ),
            ],
          );
        },
      ),
    );
  }

  /// Builds the app bar with search input.
  PreferredSizeWidget _buildAppBar(BuildContext context) {
    return AppBar(
      leading: IconButton(
        icon: const Icon(Icons.arrow_back),
        onPressed: () => Navigator.of(context).pop(),
      ),
      titleSpacing: 0,
      title: _buildSearchInput(context),
    );
  }

  /// Builds the search input field with clear button.
  Widget _buildSearchInput(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(right: 16.0),
      child: TextField(
        controller: _searchController,
        focusNode: _searchFocusNode,
        decoration: InputDecoration(
          hintText: 'Search transactions...',
          border: InputBorder.none,
          hintStyle: TextStyle(
            color: Theme.of(context).hintColor,
          ),
          suffixIcon: _searchController.text.isNotEmpty
              ? IconButton(
                  icon: const Icon(Icons.clear),
                  onPressed: _onClearSearch,
                )
              : null,
        ),
        onChanged: _onSearchChanged,
        textInputAction: TextInputAction.search,
        onSubmitted: (value) {
          // Immediate search on submit (bypass debounce)
          if (value.isNotEmpty) {
            _searchState.search(reset: true);
          }
        },
      ),
    );
  }

  /// Builds the search metadata bar showing result count and search time.
  Widget _buildSearchMetaBar(BuildContext context) {
    final resultCount = _searchState.totalResults;
    final searchTime = _searchState.searchTimeMs;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
      decoration: BoxDecoration(
        color: Theme.of(context).canvasColor,
        border: Border(
          bottom: BorderSide(
            color: Theme.of(context).dividerColor.withOpacity(0.1),
          ),
        ),
      ),
      child: Row(
        children: [
          // Result count
          Text(
            '$resultCount ${resultCount == 1 ? 'result' : 'results'}',
            style: TextStyle(
              fontSize: 13,
              color: Theme.of(context).hintColor,
            ),
          ),

          const Spacer(),

          // Search time
          if (searchTime != null)
            Text(
              '${searchTime}ms',
              style: TextStyle(
                fontSize: 12,
                color: Theme.of(context).hintColor,
              ),
            ),
        ],
      ),
    );
  }

  /// Builds the search results area with conditional states.
  Widget _buildSearchResults(BuildContext context) {
    // Loading state (initial load, no results yet)
    if (_searchState.isLoading && !_searchState.hasResults) {
      return _buildLoadingState(context);
    }

    // Error state
    if (_searchState.hasError) {
      return _buildErrorState(context);
    }

    // Empty state (search completed with no results)
    if (_searchState.isEmpty) {
      return _buildEmptyState(context);
    }

    // Results list
    if (_searchState.hasResults) {
      return _buildResultsList(context);
    }

    // Initial state (no query entered yet)
    return _buildInitialState(context);
  }

  /// Builds the loading state with shimmer/skeleton placeholders.
  Widget _buildLoadingState(BuildContext context) {
    return ListView.builder(
      itemCount: 5,
      itemBuilder: (context, index) => _buildSkeletonTile(context),
    );
  }

  /// Builds a skeleton tile for loading state.
  Widget _buildSkeletonTile(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12.0, vertical: 12.0),
      child: Row(
        children: [
          // Avatar skeleton
          Container(
            width: 32,
            height: 32,
            decoration: BoxDecoration(
              color: Theme.of(context).dividerColor.withOpacity(0.1),
              shape: BoxShape.circle,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Title skeleton
                Container(
                  height: 14,
                  width: double.infinity,
                  margin: const EdgeInsets.only(right: 100),
                  decoration: BoxDecoration(
                    color: Theme.of(context).dividerColor.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(4),
                  ),
                ),
                const SizedBox(height: 6),
                // Subtitle skeleton
                Container(
                  height: 11,
                  width: 150,
                  decoration: BoxDecoration(
                    color: Theme.of(context).dividerColor.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(4),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          // Amount skeleton
          Container(
            height: 14,
            width: 60,
            decoration: BoxDecoration(
              color: Theme.of(context).dividerColor.withOpacity(0.1),
              borderRadius: BorderRadius.circular(4),
            ),
          ),
        ],
      ),
    );
  }

  /// Builds the error state with retry button.
  Widget _buildErrorState(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.error_outline,
              size: 64,
              color: Theme.of(context).hintColor,
            ),
            const SizedBox(height: 16),
            Text(
              _searchState.errorMessage ?? 'Something went wrong',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 16,
                color: Theme.of(context).hintColor,
              ),
            ),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: _onRetry,
              icon: const Icon(Icons.refresh),
              label: const Text('Retry'),
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(
                  horizontal: 24,
                  vertical: 12,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Builds the empty state when no results are found.
  Widget _buildEmptyState(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.search_off,
              size: 64,
              color: Theme.of(context).hintColor,
            ),
            const SizedBox(height: 16),
            Text(
              'No results found',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w600,
                color: Theme.of(context).textTheme.bodyLarge?.color,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'No transactions match "${_searchState.query}"',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 14,
                color: Theme.of(context).hintColor,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'Try adjusting your search or filters',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 13,
                color: Theme.of(context).hintColor,
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Builds the initial state when no query has been entered.
  Widget _buildInitialState(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.search,
              size: 64,
              color: Theme.of(context).hintColor,
            ),
            const SizedBox(height: 16),
            Text(
              'Search your transactions',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w600,
                color: Theme.of(context).textTheme.bodyLarge?.color,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'Enter a search term to find transactions\nby name, comments, category, or account',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 14,
                color: Theme.of(context).hintColor,
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Builds the results list with infinite scroll support.
  Widget _buildResultsList(BuildContext context) {
    final results = _searchState.results;

    return RefreshIndicator(
      key: _refreshIndicatorKey,
      onRefresh: _onRefresh,
      child: ListView.builder(
        controller: _scrollController,
        itemCount: results.length + (_searchState.isLoading ? 1 : 0),
        itemBuilder: (context, index) {
          // Loading indicator at bottom when loading more
          if (index == results.length) {
            return _buildLoadMoreIndicator(context);
          }

          final hit = results[index];
          return SearchHitTile(
            hit: hit,
            onTap: () => _onSearchHitTap(hit),
          );
        },
      ),
    );
  }

  /// Builds the loading indicator at the bottom of the list.
  Widget _buildLoadMoreIndicator(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 16.0),
      child: Center(
        child: SizedBox(
          width: 24,
          height: 24,
          child: CircularProgressIndicator(
            strokeWidth: 2,
            color: Theme.of(context).primaryColor,
          ),
        ),
      ),
    );
  }
}
