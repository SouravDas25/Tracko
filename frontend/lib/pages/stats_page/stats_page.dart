import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/enums.dart';
import 'package:tracko/models/category.dart';
import 'package:tracko/pages/stats_page/category_transactions_page.dart';
import 'package:tracko/pages/stats_page/components/stats_category_list.dart';
import 'package:tracko/pages/stats_page/components/stats_filter_section.dart';
import 'package:tracko/pages/stats_page/components/stats_pie_chart.dart';
import 'package:tracko/pages/stats_page/controllers/stats_controller.dart';

/// The main Statistics dashboard page.
///
/// This page displays an overview of the user's financial statistics over a selected
/// date range (e.g., month, year, or custom range). It includes:
/// - A filter section to toggle between income/expense and select accounts.
/// - A pie chart showing the breakdown of expenses/income by category.
/// - A list of categories with their respective total amounts and progress bars.
///
/// The line chart has been relocated to the dedicated Analytics Page.
///
/// Tapping on a category in the list navigates to the [CategoryTransactionsPage]
/// for a detailed view of that category's transactions.
class StatsPage extends StatefulWidget {
  final DateTime? initialDate;
  final StatsKind? initialKind;
  final int? initialAccountId;
  final bool showAppBar;

  const StatsPage({
    super.key,
    this.initialDate,
    this.initialKind,
    this.initialAccountId,
    this.showAppBar = false,
  });

  @override
  State<StatsPage> createState() => _StatsPageState();
}

class _StatsPageState extends State<StatsPage> {
  late StatsController _controller;

  @override
  void initState() {
    super.initState();
    _controller = StatsController(
      initialDate: widget.initialDate,
      initialKind: widget.initialKind,
      initialAccountId: widget.initialAccountId,
    );
    _controller.addListener(_onControllerUpdate);
  }

  void _onControllerUpdate() {
    if (mounted) setState(() {});
  }

  @override
  void dispose() {
    _controller.removeListener(_onControllerUpdate);
    _controller.dispose();
    super.dispose();
  }

  void _navigateToCategoryDetails(CategoryStat stat) {
    if (_controller.periodStart == null ||
        _controller.periodEndExclusive == null) return;

    final cat = Category();
    cat.id = stat.categoryId;
    cat.name = stat.categoryName;

    // Use transaction type based on current stats kind
    final txType = _controller.kind == StatsKind.expense
        ? TransactionType.DEBIT
        : TransactionType.CREDIT;

    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => CategoryTransactionsPage(
          category: cat,
          startDate: _controller.periodStart!,
          endDate: _controller.periodEndExclusive!,
          range: _controller.range.name,
          transactionType: txType,
          accountId: _controller.selectedAccount?.id,
          accountName: _controller.selectedAccount?.name,
        ),
      ),
    );
  }

  Future<DateTimeRange?> _pickCustomRange(BuildContext context) async {
    DateTime start = _controller.customStartDate ??
        DateTime(DateTime.now().year, DateTime.now().month, 1);
    DateTime end = _controller.customEndDate ?? DateTime.now();
    bool isYearMode = false;

    // Generate year list (2000 to Current + 1)
    final currentYear = DateTime.now().year;
    final years = List.generate(currentYear - 2000 + 2, (index) => 2000 + index)
        .reversed
        .toList();

    return await showDialog<DateTimeRange>(
      context: context,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setState) {
            return AlertDialog(
              title: const Text("Select Date Range"),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  SwitchListTile(
                    title: const Text("Select entire years"),
                    value: isYearMode,
                    activeColor: Theme.of(context).primaryColor,
                    onChanged: (val) {
                      setState(() {
                        isYearMode = val;
                        if (isYearMode) {
                          // Snap to year boundaries
                          start = DateTime(start.year, 1, 1);
                          end = DateTime(end.year, 12, 31);
                        }
                      });
                    },
                  ),
                  const Divider(),
                  if (isYearMode) ...[
                    // YEAR SELECTION MODE
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 16.0),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          const Text("From Year:",
                              style: TextStyle(fontWeight: FontWeight.w600)),
                          DropdownButton<int>(
                            value: years.contains(start.year)
                                ? start.year
                                : years.first,
                            items: years.map((y) {
                              return DropdownMenuItem(
                                value: y,
                                child: Text(y.toString()),
                              );
                            }).toList(),
                            onChanged: (val) {
                              if (val != null) {
                                setState(() {
                                  start = DateTime(val, 1, 1);
                                  if (end.year < val) {
                                    end = DateTime(val, 12, 31);
                                  }
                                });
                              }
                            },
                          ),
                        ],
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 16.0),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          const Text("To Year:",
                              style: TextStyle(fontWeight: FontWeight.w600)),
                          DropdownButton<int>(
                            value: years.contains(end.year)
                                ? end.year
                                : years.first,
                            items: years.map((y) {
                              return DropdownMenuItem(
                                value: y,
                                child: Text(y.toString()),
                              );
                            }).toList(),
                            onChanged: (val) {
                              if (val != null) {
                                setState(() {
                                  end = DateTime(val, 12, 31);
                                  if (start.year > val) {
                                    start = DateTime(val, 1, 1);
                                  }
                                });
                              }
                            },
                          ),
                        ],
                      ),
                    ),
                  ] else ...[
                    // SPECIFIC DATE MODE
                    ListTile(
                      title: const Text("Start Date"),
                      subtitle: Text(DateFormat('MMM dd, yyyy').format(start)),
                      trailing: const Icon(Icons.calendar_today),
                      onTap: () async {
                        final d = await showDatePicker(
                          context: context,
                          initialDate: start,
                          firstDate: DateTime(2000),
                          lastDate:
                              DateTime.now().add(const Duration(days: 365)),
                        );
                        if (d != null) {
                          setState(() {
                            start = d;
                            if (end.isBefore(start)) {
                              end = start;
                            }
                          });
                        }
                      },
                    ),
                    ListTile(
                      title: const Text("End Date"),
                      subtitle: Text(DateFormat('MMM dd, yyyy').format(end)),
                      trailing: const Icon(Icons.calendar_today),
                      onTap: () async {
                        final d = await showDatePicker(
                          context: context,
                          initialDate: end,
                          firstDate: DateTime(2000),
                          lastDate:
                              DateTime.now().add(const Duration(days: 365)),
                        );
                        if (d != null) {
                          setState(() {
                            end = d;
                            if (start.isAfter(end)) {
                              start = end;
                            }
                          });
                        }
                      },
                    ),
                  ],
                ],
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: const Text("Cancel"),
                ),
                TextButton(
                  onPressed: () => Navigator.pop(
                      context, DateTimeRange(start: start, end: end)),
                  child: const Text("Apply"),
                ),
              ],
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: CustomScrollView(
        slivers: [
          if (widget.showAppBar)
            const SliverAppBar(
              title: Text("Statistics"),
              pinned: true,
            ),
          SliverToBoxAdapter(
            child: StatsFilterSection(
              range: _controller.range,
              kind: _controller.kind,
              selectedAccount: _controller.selectedAccount,
              accounts: _controller.accounts,
              onRangeChanged: (range) async {
                if (range == StatsRange.custom) {
                  final DateTimeRange? picked = await _pickCustomRange(context);
                  if (picked != null) {
                    _controller.setCustomRange(picked.start, picked.end);
                  }
                } else {
                  _controller.setRange(range);
                }
              },
              onKindChanged: _controller.setKind,
              onAccountChanged: _controller.setAccount,
            ),
          ),
          SliverPersistentHeader(
            pinned: true,
            delegate: _StickyStatsHeaderDelegate(
              context: context,
              dateText: _controller.formattedDateRange,
              isLoading: _controller.loading,
              disableNavigation: _controller.range == StatsRange.custom,
              onPrevious: () => _controller.shiftAnchor(-1),
              onNext: () => _controller.shiftAnchor(1),
            ),
          ),
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
              child: Text(
                '${_controller.kindLabel}: ${CommonUtil.toCurrency(_controller.total)}',
                style: const TextStyle(fontWeight: FontWeight.bold),
              ),
            ),
          ),
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
              child: Container(
                decoration: BoxDecoration(
                  color: Theme.of(context).cardColor,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(
                      color: Theme.of(context).dividerColor.withOpacity(0.1)),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.02),
                      blurRadius: 8,
                      offset: const Offset(0, 2),
                    )
                  ],
                ),
                child: Padding(
                  padding: const EdgeInsets.all(12.0),
                  child: StatsPieChart(
                    loading: _controller.loading,
                    error: _controller.error,
                    pieSeries: _controller.pieSeries,
                  ),
                ),
              ),
            ),
          ),
          SliverToBoxAdapter(
            child: StatsCategoryList(
              loading: _controller.loading,
              error: _controller.error,
              stats: _controller.stats,
              total: _controller.total,
              kindColor: _controller.kindColor,
              onCategoryTap: _navigateToCategoryDetails,
            ),
          ),
          const SliverPadding(padding: EdgeInsets.only(bottom: 80)),
        ],
      ),
    );
  }
}

class _StickyStatsHeaderDelegate extends SliverPersistentHeaderDelegate {
  final BuildContext context;
  final String dateText;
  final bool isLoading;
  final bool disableNavigation;
  final VoidCallback onPrevious;
  final VoidCallback onNext;

  _StickyStatsHeaderDelegate({
    required this.context,
    required this.dateText,
    required this.isLoading,
    this.disableNavigation = false,
    required this.onPrevious,
    required this.onNext,
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
          height: 56.0,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              IconButton(
                icon: Icon(
                  Icons.chevron_left_rounded,
                  color: disableNavigation ? Colors.white38 : Colors.white,
                ),
                onPressed: isLoading || disableNavigation ? null : onPrevious,
              ),
              Text(
                dateText,
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w500,
                  color: Colors.white,
                ),
              ),
              IconButton(
                icon: Icon(
                  Icons.chevron_right_rounded,
                  color: disableNavigation ? Colors.white38 : Colors.white,
                ),
                onPressed: isLoading || disableNavigation ? null : onNext,
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
  bool shouldRebuild(_StickyStatsHeaderDelegate oldDelegate) {
    return dateText != oldDelegate.dateText ||
        isLoading != oldDelegate.isLoading ||
        disableNavigation != oldDelegate.disableNavigation ||
        onPrevious != oldDelegate.onPrevious ||
        onNext != oldDelegate.onNext;
  }
}
