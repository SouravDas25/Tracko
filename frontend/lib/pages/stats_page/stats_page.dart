import 'package:flutter/material.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/enums.dart';
import 'package:tracko/models/category.dart';
import 'package:tracko/pages/stats_page/category_transactions_page.dart';
import 'package:tracko/pages/stats_page/components/stats_category_list.dart';
import 'package:tracko/pages/stats_page/components/stats_date_navigator.dart';
import 'package:tracko/pages/stats_page/components/stats_filter_section.dart';
import 'package:tracko/pages/stats_page/components/stats_line_chart.dart';
import 'package:tracko/pages/stats_page/components/stats_pie_chart.dart';
import 'package:tracko/pages/stats_page/controllers/stats_controller.dart';

class StatsPage extends StatefulWidget {
  const StatsPage({super.key});

  @override
  State<StatsPage> createState() => _StatsPageState();
}

class _StatsPageState extends State<StatsPage> {
  late StatsController _controller;

  @override
  void initState() {
    super.initState();
    _controller = StatsController();
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
      body: ListView(
        padding: EdgeInsets.zero,
        children: [
          StatsFilterSection(
            range: _controller.range,
            kind: _controller.kind,
            onRangeChanged: _controller.setRange,
            onKindChanged: _controller.setKind,
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
            child: Card(
              elevation: 2,
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16)),
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
          StatsDateNavigator(
            dateText: _controller.formattedDateRange,
            isLoading: _controller.loading,
            onPrevious: () => _controller.shiftAnchor(-1),
            onNext: () => _controller.shiftAnchor(1),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
            child: Text(
              '${_controller.kindLabel}: ${CommonUtil.toCurrency(_controller.total)}',
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
            child: Card(
              elevation: 2,
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16)),
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
          StatsCategoryList(
            loading: _controller.loading,
            error: _controller.error,
            stats: _controller.stats,
            total: _controller.total,
            kindColor: _controller.kindColor,
            onCategoryTap: _navigateToCategoryDetails,
          ),
        ],
      ),
    );
  }
}
