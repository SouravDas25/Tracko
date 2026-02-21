import 'package:flutter/material.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/enums.dart';
import 'package:tracko/models/category.dart';
import 'package:tracko/pages/stats_page/category_transactions_page.dart';
import 'package:tracko/pages/stats_page/components/stats_category_list.dart';
import 'package:tracko/pages/stats_page/components/stats_filter_section.dart';
import 'package:tracko/pages/stats_page/components/stats_line_chart.dart';
import 'package:tracko/pages/stats_page/components/stats_pie_chart.dart';
import 'package:tracko/pages/stats_page/controllers/stats_controller.dart';

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
        ),
      ),
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
                  final DateTimeRange? picked = await showDateRangePicker(
                    context: context,
                    firstDate: DateTime(2000),
                    lastDate: DateTime.now().add(const Duration(days: 365)),
                    initialDateRange: _controller.range == StatsRange.custom &&
                            _controller.customStartDate != null &&
                            _controller.customEndDate != null
                        ? DateTimeRange(
                            start: _controller.customStartDate!,
                            end: _controller.customEndDate!)
                        : null,
                    builder: (context, child) {
                      return Theme(
                        data: Theme.of(context).copyWith(
                          colorScheme: ColorScheme.light(
                            primary: Theme.of(context).primaryColor,
                            onPrimary: Colors.white,
                            surface: Theme.of(context).cardColor,
                            onSurface:
                                Theme.of(context).textTheme.bodyLarge?.color ??
                                    Colors.black,
                          ),
                        ),
                        child: child!,
                      );
                    },
                  );
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
                  child: StatsLineChart(
                    loading: _controller.loading,
                    error: _controller.error,
                    series: _controller.series,
                    seriesMaxY: _controller.seriesMaxY,
                    kindColor: _controller.kindColor,
                  ),
                ),
              ),
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
