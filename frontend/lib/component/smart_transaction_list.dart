import 'package:flutter/material.dart';
import 'package:pull_to_refresh/pull_to_refresh.dart';
import 'package:tracko/Utils/AppLog.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/SettingUtil.dart';
import 'package:tracko/component/interfaces.dart';
import 'package:tracko/component/month_picker_dialog.dart';
import 'package:tracko/component/transaction_list_widget.dart';
import 'package:tracko/controllers/TransactionController.dart';
import 'package:tracko/models/transaction.dart';
import 'package:tracko/repositories/transaction_repository.dart';
import 'package:tracko/di/di.dart';
import 'package:intl/intl.dart' as DateFormatter;

enum TransactionViewMode {
  monthlyNavigation,
  continuousRange,
}

class SmartTransactionList extends StatefulWidget {
  // Filters
  final List<int>? accountIds;
  final int? categoryId;
  final int? transactionType;

  // View Mode & Dates
  final TransactionViewMode viewMode;
  final DateTime? initialMonth;
  final DateTime? startDate;
  final DateTime? endDate;

  // UI
  final Widget? headerWidget;
  final Function(DateTime newMonth)? onMonthChanged;

  const SmartTransactionList({
    Key? key,
    this.accountIds,
    this.categoryId,
    this.transactionType,
    required this.viewMode,
    this.initialMonth,
    this.startDate,
    this.endDate,
    this.headerWidget,
    this.onMonthChanged,
  }) : super(key: key);

  @override
  State<SmartTransactionList> createState() => _SmartTransactionListState();
}

class _SmartTransactionListState extends State<SmartTransactionList> {
  final RefreshController _refreshController = RefreshController();
  final ScrollController _scrollController = ScrollController();
  List<Transaction> _transactions = [];
  bool _isLoading = true;
  bool _hasMore = true;
  bool _isLoadingMore = false;
  int _currentPage = 0;
  static const int _pageSize = 20;

  DateTime? _currentMonthValue;
  DateTime get _currentMonth =>
      _currentMonthValue ?? widget.initialMonth ?? SettingUtil.currentMonth;

  @override
  void initState() {
    super.initState();
    _currentMonthValue = widget.initialMonth ?? SettingUtil.currentMonth;
    _scrollController.addListener(_onScroll);
    // Trigger initial load after the widget is fully initialized
    WidgetsBinding.instance.addPostFrameCallback((_) {
      refresh();
    });
  }

  void _onScroll() {
    if (!_scrollController.hasClients) return;
    final pos = _scrollController.position;
    if (pos.pixels >= pos.maxScrollExtent - 200 &&
        _hasMore &&
        !_isLoadingMore &&
        !_isLoading) {
      _onLoading();
    }
  }

  @override
  void didUpdateWidget(covariant SmartTransactionList oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.viewMode == TransactionViewMode.monthlyNavigation &&
        widget.initialMonth != null &&
        widget.initialMonth != oldWidget.initialMonth) {
      if (_currentMonthValue != widget.initialMonth) {
        _currentMonthValue = widget.initialMonth!;
        refresh();
      }
    }
  }

  @override
  void dispose() {
    _scrollController.removeListener(_onScroll);
    _scrollController.dispose();
    _refreshController.dispose();
    super.dispose();
  }

  Future<void> refresh() async {
    try {
      AppLog.d('[SmartTransactionList] refresh started');
      if (mounted) setState(() => _isLoading = true);
      await _loadData(isRefresh: true);
      if (mounted) {
        setState(() => _isLoading = false);
        _refreshController.refreshCompleted();
        if (!_hasMore) _refreshController.loadNoData();
      }
      AppLog.d('[SmartTransactionList] refresh success');
    } catch (e, stack) {
      AppLog.d('[SmartTransactionList] refresh error: $e\n$stack');
      if (mounted) setState(() => _isLoading = false);
      _refreshController.refreshFailed();
    }
  }

  Future<void> _onLoading() async {
    if (_isLoadingMore) {
      // Already loading via scroll — don't leave the pull-up footer stuck
      _refreshController.loadComplete();
      return;
    }
    if (!_hasMore) {
      _refreshController.loadNoData();
      return;
    }
    _isLoadingMore = true;
    try {
      AppLog.d('[SmartTransactionList] loading more data...');
      await _loadData(isRefresh: false);
      if (mounted) setState(() {});
      if (!_hasMore) {
        _refreshController.loadNoData();
      } else {
        _refreshController.loadComplete();
      }
    } catch (e, stack) {
      AppLog.d('[SmartTransactionList] load more error: $e\n$stack');
      _refreshController.loadFailed();
    } finally {
      _isLoadingMore = false;
    }
  }

  Future<void> _loadData({required bool isRefresh}) async {
    AppLog.d('[SmartTransactionList] _loadData(isRefresh: $isRefresh) started');
    if (isRefresh) {
      _currentPage = 0;
      _hasMore = true;
      _transactions.clear();
      _refreshController.resetNoData();
    }

    dynamic response;

    if (widget.viewMode == TransactionViewMode.monthlyNavigation) {
      AppLog.d(
          '[SmartTransactionList] calling getTransactionsForSelectedMonthPaginated');
      final response =
          await TransactionController.getTransactionsForSelectedMonthPaginated(
        accountIds: widget.accountIds,
        month: _currentMonth,
        page: _currentPage,
        size: _pageSize,
        categoryId: widget.categoryId,
      );

      AppLog.d('[SmartTransactionList] mapping response transactions');
      List<Transaction> fetched = response.transactions;

      // Note: categoryId is already filtered by backend, but we keep this just in case?
      // Actually, we should probably remove it if we trust the backend to avoid double filtering redundancy
      // However, transactionType is NOT filtered by backend in getAll endpoint (yet), so we keep it.

      if (widget.transactionType != null) {
        fetched = fetched
            .where((t) => t.transactionType == widget.transactionType)
            .toList();
      }

      if (!response.hasNext) {
        _hasMore = false;
      }

      if (isRefresh) {
        _transactions = fetched;
      } else {
        _transactions.addAll(fetched);
      }
    } else {
      AppLog.d(
          '[SmartTransactionList] calling getAllPaginated from repo directly');
      // Continuous Range
      final txRepo = sl<TransactionRepository>();
      final txResponse = await txRepo.getAllPaginated(
        startDate: widget.startDate,
        endDate: widget.endDate,
        categoryId: widget.categoryId,
        accountIds: widget.accountIds,
        page: _currentPage,
        size: _pageSize,
        expand: true,
      );

      List<Transaction> fetched = txResponse['transactions'] ?? <Transaction>[];

      if (widget.transactionType != null) {
        fetched = fetched
            .where((t) => t.transactionType == widget.transactionType)
            .toList();
      }

      _hasMore = txResponse['hasNext'] ?? false;

      if (isRefresh) {
        _transactions = fetched;
      } else {
        _transactions.addAll(fetched);
      }
    }

    _currentPage++;
    AppLog.d(
        '[SmartTransactionList] _loadData completed, transactions count: ${_transactions.length}');
  }

  void _goToMonth(int diff) {
    setState(() {
      _currentMonthValue =
          DateTime.utc(_currentMonth.year, _currentMonth.month + diff);
    });
    SettingUtil.setSelectedMonth(_currentMonth);
    widget.onMonthChanged?.call(_currentMonth);
    refresh();
  }

  Future<void> _selectMonth() async {
    final DateTime? picked = await showMonthPicker(
      context: context,
      initialDate: _currentMonth,
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
    );

    if (picked != null) {
      final next = DateTime.utc(picked.year, picked.month);
      if (next != _currentMonth) {
        setState(() {
          _currentMonthValue = next;
        });
        SettingUtil.setSelectedMonth(_currentMonth);
        widget.onMonthChanged?.call(_currentMonth);
        refresh();
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return SmartRefresher(
      controller: _refreshController,
      enablePullDown: true,
      enablePullUp: true,
      onRefresh: refresh,
      onLoading: _onLoading,
      child: CustomScrollView(
        controller: _scrollController,
        slivers: [
          if (widget.viewMode == TransactionViewMode.monthlyNavigation)
            SliverPersistentHeader(
              pinned: true,
              floating: true,
              delegate: _StickyMonthHeaderDelegate(
                context: context,
                selectedMonth: _currentMonth,
                onPrevious: () => _goToMonth(-1),
                onNext: () => _goToMonth(1),
                onSelect: _selectMonth,
              ),
            ),
          if (widget.headerWidget != null)
            SliverToBoxAdapter(
              child: widget.headerWidget!,
            ),
          if (_isLoading)
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.all(32.0),
                child: Center(
                  child: SizedBox(
                    width: 32,
                    height: 32,
                    child: CircularProgressIndicator(strokeWidth: 3),
                  ),
                ),
              ),
            )
          else
            TransactionListWidget(
              transactions: _transactions,
              hasMore: _hasMore,
              isSliver: true,
              parent: this,
              onDelete: (t) async {
                await TransactionController.deleteById(t.id ?? 0);
                await refresh();
              },
            ),
          const SliverPadding(padding: EdgeInsets.only(bottom: 80)),
        ],
      ),
    );
  }
}

class _StickyMonthHeaderDelegate extends SliverPersistentHeaderDelegate {
  final BuildContext context;
  final DateTime selectedMonth;
  final VoidCallback? onPrevious;
  final VoidCallback? onNext;
  final VoidCallback onSelect;

  _StickyMonthHeaderDelegate({
    required this.context,
    required this.selectedMonth,
    this.onPrevious,
    this.onNext,
    required this.onSelect,
  });

  @override
  Widget build(
      BuildContext context, double shrinkOffset, bool overlapsContent) {
    return Container(
      color: Theme.of(context).appBarTheme.backgroundColor ??
          Theme.of(context).primaryColor,
      child: SafeArea(
        top: false,
        bottom: false,
        child: SizedBox(
          height: 56.0, // Standard AppBar height
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              IconButton(
                icon: Icon(Icons.chevron_left, color: Colors.white),
                onPressed: onPrevious,
                tooltip: 'Previous month',
              ),
              Expanded(
                child: GestureDetector(
                  onTap: onSelect,
                  behavior: HitTestBehavior.opaque,
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        DateFormatter.DateFormat('MMMM yyyy')
                            .format(selectedMonth),
                        style: TextStyle(
                          fontSize: 18,
                          color: Colors.white,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                      const SizedBox(width: 4),
                      Icon(Icons.arrow_drop_down, color: Colors.white),
                    ],
                  ),
                ),
              ),
              IconButton(
                icon: Icon(Icons.chevron_right, color: Colors.white),
                onPressed: onNext,
                tooltip: 'Next month',
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  double get maxExtent => 56.0;

  @override
  double get minExtent => 56.0;

  @override
  bool shouldRebuild(_StickyMonthHeaderDelegate oldDelegate) {
    return selectedMonth != oldDelegate.selectedMonth ||
        onPrevious != oldDelegate.onPrevious ||
        onNext != oldDelegate.onNext;
  }
}
