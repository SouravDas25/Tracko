import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/SettingUtil.dart';
import 'package:tracko/component/PageWidget.dart';
import 'package:tracko/component/PaddedText.dart';
import 'package:tracko/component/TransactionTile.dart';
import 'package:tracko/component/interfaces.dart';
import 'package:tracko/component/month_picker_dialog.dart';
import 'package:tracko/controllers/TransactionController.dart';
import 'package:tracko/models/transaction.dart';
import 'package:tracko/Utils/AppLog.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart' as DateFormatter;
import 'package:pull_to_refresh/pull_to_refresh.dart';

class TransactionListPage extends StatefulWidget {
  final List<int>? initialAccountIds;
  final bool embedded;

  const TransactionListPage({
    Key? key,
    this.initialAccountIds,
    this.embedded = false,
  }) : super(key: key);

  @override
  State<StatefulWidget> createState() {
    return _TransactionListPageState();
  }
}

class _TransactionListPageState extends RefreshableState<TransactionListPage> {
  RefreshController refreshController = new RefreshController();
  List<Transaction> transactions = [];
  List<dynamic> selections = [];
  double previousMonthAmount = 0.0;
  double totalAmount = 0.0;
  double incomeAmount = 0.0;
  double expenseAmount = 0.0;
  DateTime selectedMonth = SettingUtil.currentMonth;
  bool _isProgrammaticLoading = false;

  int _currentPage = 0;
  static const int _pageSize = 20;
  bool _hasMore = true;

  _TransactionListPageState();

  Future<void> _goToPreviousMonth() async {
    if (_isProgrammaticLoading) {
      AppLog.d('[TransactionListPage] skip prev month: already loading');
      return;
    }
    if (mounted) {
      setState(() {
        _isProgrammaticLoading = true;
        selectedMonth =
            DateTime.utc(selectedMonth.year, selectedMonth.month - 1);
      });
    } else {
      selectedMonth = DateTime.utc(selectedMonth.year, selectedMonth.month - 1);
    }
    SettingUtil.setSelectedMonth(selectedMonth);
    try {
      await refresh();
    } finally {
      if (mounted) {
        setState(() {
          _isProgrammaticLoading = false;
        });
      } else {
        _isProgrammaticLoading = false;
      }
    }
  }

  Future<void> _goToNextMonth() async {
    if (_isProgrammaticLoading) {
      AppLog.d('[TransactionListPage] skip next month: already loading');
      return;
    }
    if (mounted) {
      setState(() {
        _isProgrammaticLoading = true;
        selectedMonth =
            DateTime.utc(selectedMonth.year, selectedMonth.month + 1);
      });
    } else {
      selectedMonth = DateTime.utc(selectedMonth.year, selectedMonth.month + 1);
    }
    SettingUtil.setSelectedMonth(selectedMonth);
    try {
      await refresh();
    } finally {
      if (mounted) {
        setState(() {
          _isProgrammaticLoading = false;
        });
      } else {
        _isProgrammaticLoading = false;
      }
    }
  }

  Future<void> _selectMonth() async {
    if (_isProgrammaticLoading) {
      AppLog.d('[TransactionListPage] skip month picker: already loading');
      return;
    }
    final DateTime? picked = await showMonthPicker(
      context: context,
      initialDate: selectedMonth,
      firstDate: DateTime(2020),
      lastDate: DateTime(2030),
    );

    if (picked != null) {
      final next = DateTime.utc(picked.year, picked.month);
      if (next != selectedMonth) {
        if (mounted) {
          setState(() {
            _isProgrammaticLoading = true;
            selectedMonth = next;
          });
        } else {
          selectedMonth = next;
        }
        SettingUtil.setSelectedMonth(selectedMonth);
        try {
          await refresh();
        } finally {
          if (mounted) {
            setState(() {
              _isProgrammaticLoading = false;
            });
          } else {
            _isProgrammaticLoading = false;
          }
        }
      }
    }
  }

  @override
  void dispose() {
    refreshController.dispose();
    super.dispose();
  }

  @override
  asyncLoad() async {
    AppLog.d(
        '[TransactionListPage] asyncLoad start embedded=${widget.embedded} initialAccountIds=${widget.initialAccountIds}');
    try {
      // Pre-seed selections from initialAccountIds if provided
      if (widget.initialAccountIds != null &&
          widget.initialAccountIds!.isNotEmpty) {
        selections = List<dynamic>.from(widget.initialAccountIds!);
      }

      selectedMonth = SettingUtil.currentMonth;
      AppLog.d('[TransactionListPage] selectedMonth=$selectedMonth');

      await refresh();
      this.loadCompleteView();
      AppLog.d('[TransactionListPage] asyncLoad complete');
    } catch (e) {
      AppLog.d('[TransactionListPage] asyncLoad error: $e');
      if (mounted) {
        // If not logged in or session invalid, go to welcome/login flow.
        Navigator.pushReplacementNamed(context, '/welcome');
      }
    }
  }

  @override
  Future<void> refresh() async {
    AppLog.d('[TransactionListPage] refresh start month=$selectedMonth');
    try {
      await initTransactionData(isRefresh: true);
      if (this.mounted) {
        setState(() {
          // Ensure any programmatic loader is cleared after successful data load
          if (_isProgrammaticLoading) {
            AppLog.d(
                '[TransactionListPage] clearing programmatic loader (success)');
          }
          _isProgrammaticLoading = false;
          refreshController.refreshCompleted();
        });
      } else {
        // If not mounted, still clear the flag to avoid stale state
        _isProgrammaticLoading = false;
      }
      AppLog.d(
          '[TransactionListPage] refresh complete txCount=${transactions.length}');
    } catch (e) {
      AppLog.d('[TransactionListPage] refresh error: $e');
      // Likely unauthenticated; redirect to welcome/login.
      if (mounted) {
        setState(() {
          if (_isProgrammaticLoading) {
            AppLog.d(
                '[TransactionListPage] clearing programmatic loader (error)');
          }
          _isProgrammaticLoading = false;
        });
        Navigator.pushReplacementNamed(context, '/welcome');
      } else {
        _isProgrammaticLoading = false;
      }
    }
  }

  Future<void> _onLoading() async {
    if (!_hasMore) {
      refreshController.loadNoData();
      return;
    }
    try {
      await initTransactionData(isRefresh: false);
      if (mounted) {
        setState(() {});
      }
      refreshController.loadComplete();
    } catch (e) {
      refreshController.loadFailed();
    }
  }

  initTransactionData({bool isRefresh = true}) async {
    AppLog.d(
        '[TransactionListPage] initTransactionData start selections=$selections isRefresh=$isRefresh');
    
    if (isRefresh) {
      _currentPage = 0;
      _hasMore = true;
      transactions.clear();
      refreshController.resetNoData();
    }

    List<int> accountIds = [];
    if (selections != null && selections.length > 0) {
      int accountId;
      for (int i = 0; i < selections.length; i++) {
        accountId = int.parse(selections[i].toString());
        accountIds.add(accountId);
      }
    }
    AppLog.d(
        '[TransactionListPage] parsed accountIds=$accountIds month=$selectedMonth page=$_currentPage');
    
    // Load transactions for the selected month with pagination
    List<Transaction> newTransactions = await TransactionController.getTransactionsForSelectedMonthPaginated(
        accountIds: accountIds, month: selectedMonth, page: _currentPage, size: _pageSize);
    
    if (newTransactions.length < _pageSize) {
      _hasMore = false;
      refreshController.loadNoData();
    } else {
      _hasMore = true;
    }

    if (isRefresh) {
      transactions = newTransactions;
    } else {
      transactions.addAll(newTransactions);
    }
    
    _currentPage++;

    AppLog.d(
        '[TransactionListPage] fetched transactions count=${newTransactions.length} total=${transactions.length}');

    // Only fetch summary on refresh to avoid redundant calls
    if (isRefresh) {
      totalAmount = incomeAmount = expenseAmount = 0;

      final currentMonthDate =
          DateTime.utc(selectedMonth.year, selectedMonth.month);
      final nextMonthDate =
          DateTime.utc(selectedMonth.year, selectedMonth.month + 1);

      final summary = await TransactionController.getSummaryBetween(
        currentMonthDate,
        nextMonthDate,
        accountIds: accountIds,
      );

      incomeAmount = (summary['totalIncome'] as num?)?.toDouble() ?? 0.0;
      expenseAmount = (summary['totalExpense'] as num?)?.toDouble() ?? 0.0;
      previousMonthAmount = (summary['rolloverNet'] as num?)?.toDouble() ?? 0.0;
      totalAmount = (summary['netTotalWithRollover'] as num?)?.toDouble() ??
          (incomeAmount - expenseAmount + previousMonthAmount);

      AppLog.d(
          '[TransactionListPage] totals previous=$previousMonthAmount balance=$totalAmount');
    }
  }

  Widget _buildContent() {
    return SmartRefresher(
      controller: refreshController,
      enablePullDown: true,
      enablePullUp: true,
      onRefresh: () async {
        await refresh();
      },
      onLoading: _onLoading,
      child: CustomScrollView(
        slivers: <Widget>[
          SliverPersistentHeader(
            pinned: true,
            floating: true,
            delegate: _StickyMonthHeaderDelegate(
              context: context,
              selectedMonth: selectedMonth,
              onPrevious: _isProgrammaticLoading ? null : _goToPreviousMonth,
              onNext: _isProgrammaticLoading ? null : _goToNextMonth,
              onSelect: _selectMonth,
            ),
          ),
          SliverToBoxAdapter(
            child: Padding(
              padding:
                  const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
              child: Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [
                      Theme.of(context).cardColor,
                      Theme.of(context).cardColor.withOpacity(0.95),
                    ],
                  ),
                  borderRadius: BorderRadius.circular(24),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.03),
                      blurRadius: 15,
                      offset: const Offset(0, 8),
                    ),
                  ],
                  border: Border.all(
                    color: Theme.of(context).dividerColor.withOpacity(0.05),
                  ),
                ),
                child: Column(
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          "Total Balance",
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w500,
                            color: Theme.of(context).hintColor,
                          ),
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(
                            color:
                                Theme.of(context).primaryColor.withOpacity(0.1),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Text(
                            "Net",
                            style: TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.bold,
                              color: Theme.of(context).primaryColor,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Align(
                      alignment: Alignment.centerLeft,
                      child: Text(
                        CommonUtil.toCurrency(totalAmount),
                        style: TextStyle(
                          fontSize: 32,
                          fontWeight: FontWeight.w800,
                          color: Theme.of(context).textTheme.bodyLarge?.color,
                          letterSpacing: -0.5,
                        ),
                      ),
                    ),
                    const SizedBox(height: 24),
                    Row(
                      children: [
                        Expanded(
                          child: _buildSummaryItem(
                            context,
                            "Income",
                            incomeAmount,
                            Colors.green.shade500,
                            Icons.arrow_downward_rounded,
                          ),
                        ),
                        Container(
                          width: 1,
                          height: 40,
                          color:
                              Theme.of(context).dividerColor.withOpacity(0.2),
                        ),
                        Expanded(
                          child: _buildSummaryItem(
                            context,
                            "Expense",
                            expenseAmount,
                            Colors.red.shade400,
                            Icons.arrow_upward_rounded,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ),
          if (_isProgrammaticLoading)
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
          else if (transactions.isEmpty)
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.only(top: 48.0),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Container(
                      padding: const EdgeInsets.all(24),
                      decoration: BoxDecoration(
                        color:
                            Theme.of(context).disabledColor.withOpacity(0.05),
                        shape: BoxShape.circle,
                      ),
                      child: Icon(
                        Icons.receipt_long_rounded,
                        size: 48,
                        color: Theme.of(context).disabledColor.withOpacity(0.5),
                      ),
                    ),
                    const SizedBox(height: 16),
                    Text(
                      "No transactions yet",
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w600,
                        color: Theme.of(context).hintColor,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      "Tap + to add a new one",
                      style: TextStyle(
                        fontSize: 14,
                        color: Theme.of(context).disabledColor,
                      ),
                    ),
                  ],
                ),
              ),
            )
          else
            SliverList(
              key: ValueKey(selectedMonth.toIso8601String()),
              delegate: SliverChildBuilderDelegate(
                (context, index) {
                  final tx = transactions[index];
                  final currentHuman =
                      CommonUtil.humanDate(tx.date).toUpperCase();
                  String? prevHuman;
                  if (index > 0) {
                    prevHuman =
                        CommonUtil.humanDate(transactions[index - 1].date)
                            .toUpperCase();
                  }

                  final List<Widget> children = [];
                  if (index == 0 || prevHuman != currentHuman) {
                    children.add(
                      Padding(
                        padding: const EdgeInsets.fromLTRB(20, 24, 20, 8),
                        child: Text(
                          currentHuman,
                          style: TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.bold,
                            color: Theme.of(context).hintColor,
                            letterSpacing: 1.0,
                          ),
                        ),
                      ),
                    );
                  }
                  children.add(
                    TransactionTile(this, tx, (parent, Transaction t) async {
                      await refresh();
                    }),
                  );

                  if (children.length == 1) {
                    return children.first;
                  }
                  return Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    mainAxisSize: MainAxisSize.min,
                    children: children,
                  );
                },
                childCount: transactions.length,
              ),
            ),
          const SliverPadding(padding: EdgeInsets.only(bottom: 80)),
        ],
      ),
    );
  }

  Widget _buildSummaryItem(BuildContext context, String label, double amount,
      Color color, IconData icon) {
    return Column(
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(4),
              decoration: BoxDecoration(
                color: color.withOpacity(0.1),
                shape: BoxShape.circle,
              ),
              child: Icon(icon, size: 12, color: color),
            ),
            const SizedBox(width: 6),
            Text(
              label,
              style: TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w500,
                color: Theme.of(context).hintColor,
              ),
            ),
          ],
        ),
        const SizedBox(height: 4),
        Text(
          CommonUtil.toCurrency(amount),
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
            color: color,
          ),
        ),
      ],
    );
  }

  @override
  Widget completeWidget(BuildContext context) {
    if (widget.embedded) {
      return _buildContent();
    }
    return Scaffold(
      appBar: AppBar(
        title: const Text('Transactions'),
        centerTitle: true,
      ),
      body: _buildContent(),
      floatingActionButton: FloatingActionButton(
        onPressed: () async {
          await Navigator.pushNamed(context, '/add_item');
          await refresh();
        },
        child: const Icon(Icons.add),
      ),
    );
  }

  @override
  Widget fallbackWidget(BuildContext context) {
    return Center(
      child: Text("No Data Available."),
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
