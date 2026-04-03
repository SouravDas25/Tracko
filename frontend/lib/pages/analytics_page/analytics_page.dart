import 'package:flutter/material.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/component/amount_text.dart';
import 'package:tracko/pages/analytics_page/components/analytics_touch_details.dart';
import 'package:tracko/pages/analytics_page/components/custom_date_range_picker.dart';
import 'package:tracko/pages/analytics_page/components/analytics_empty_state.dart';
import 'package:tracko/pages/analytics_page/components/analytics_filter_section.dart';
import 'package:tracko/pages/analytics_page/components/series_legend.dart';
import 'package:tracko/pages/analytics_page/controllers/analytics_controller.dart';
import 'package:tracko/pages/analytics_page/models/analytics_models.dart';
import 'package:tracko/pages/stats_page/components/stats_line_chart.dart';
import 'package:tracko/pages/stats_page/controllers/stats_controller.dart';

class AnalyticsPage extends StatefulWidget {
  final bool showAppBar;

  const AnalyticsPage({
    super.key,
    this.showAppBar = true,
  });

  @override
  State<AnalyticsPage> createState() => _AnalyticsPageState();
}

class _AnalyticsPageState extends State<AnalyticsPage> {
  late AnalyticsController _controller;
  TouchedPointData? _touchedPoint;

  @override
  void initState() {
    super.initState();
    _controller = AnalyticsController();
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

  Color get _kindColor =>
      _controller.kind == StatsKind.expense ? Colors.red : Colors.green;

  @override
  Widget build(BuildContext context) {
    final hasError = _controller.error != null;
    final isEmpty = !_controller.loading &&
        !hasError &&
        _controller.groupedSeries.every((s) => s.points.isEmpty);
    final useMultiSeries = _controller.groupBy != GroupByMode.none &&
        _controller.groupedSeries.length > 1;

    final flatSeries = _controller.groupedSeries.isNotEmpty
        ? _controller.groupedSeries.first.points
        : <SeriesPoint>[];

    final isCustom = _controller.datePreset == DateRangePreset.custom;

    return Scaffold(
      appBar: widget.showAppBar ? AppBar(title: const Text("Analytics")) : null,
      body: CustomScrollView(
        slivers: [
          // Compact filter section (granularity + kind always visible,
          // account/category/groupBy collapsed behind "Filters" toggle)
          SliverToBoxAdapter(
            child: AnalyticsFilterSection(
              granularity: _controller.granularity,
              datePreset: _controller.datePreset,
              kind: _controller.kind,
              selectedAccount: _controller.selectedAccount,
              accounts: _controller.accounts,
              selectedCategory: _controller.selectedCategory,
              categories: _controller.categories,
              groupBy: _controller.groupBy,
              onGranularityChanged: _controller.setGranularity,
              onDatePresetChanged: (preset) async {
                if (preset == DateRangePreset.custom) {
                  final picked = await showCustomDateRangePicker(
                    context: context,
                    initialStart: _controller.startDate,
                    initialEnd: _controller.endDate,
                  );
                  if (picked != null) {
                    _controller.setCustomDateRange(picked.start, picked.end);
                  }
                } else {
                  _controller.setDatePreset(preset);
                }
              },
              onKindChanged: _controller.setKind,
              onAccountChanged: _controller.setAccount,
              onCategoryChanged: _controller.setCategory,
              onGroupByChanged: _controller.setGroupBy,
            ),
          ),

          // Total summary — compact row right above the chart
          if (!_controller.loading && !hasError && !isEmpty)
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(16, 4, 16, 8),
                child: Row(
                  children: [
                    Text(
                      'Total',
                      style: TextStyle(
                        fontSize: 13,
                        color: Theme.of(context).hintColor,
                      ),
                    ),
                    const SizedBox(width: 8),
                    AmountText(
                      amount: _controller.total,
                      color: _kindColor,
                      fontSize: 15,
                    ),
                  ],
                ),
              ),
            ),

          // Chart card with integrated date navigation header
          if (_controller.loading)
            const SliverToBoxAdapter(
              child: SizedBox(
                height: 220,
                child: Center(child: CircularProgressIndicator()),
              ),
            )
          else if (hasError)
            SliverToBoxAdapter(
              child: SizedBox(
                height: 220,
                child: Center(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        _controller.error!,
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: Theme.of(context).colorScheme.error,
                        ),
                      ),
                      const SizedBox(height: 12),
                      ElevatedButton(
                        onPressed: _controller.retry,
                        child: const Text("Retry"),
                      ),
                    ],
                  ),
                ),
              ),
            )
          else if (isEmpty)
            const SliverToBoxAdapter(
              child: AnalyticsEmptyState(),
            )
          else
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
                  child: Column(
                    children: [
                      // Date navigation row inside the card
                      _buildDateNavRow(context, isCustom),
                      Padding(
                        padding: const EdgeInsets.fromLTRB(12, 0, 12, 12),
                        child: StatsLineChart(
                          loading: false,
                          error: null,
                          series: flatSeries,
                          seriesMaxY: _controller.seriesMaxY,
                          kindColor: _kindColor,
                          multiSeries:
                              useMultiSeries ? _controller.groupedSeries : null,
                          onPointTouched: useMultiSeries
                              ? (data) => setState(() => _touchedPoint = data)
                              : null,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),

          // Touched-point detail panel
          if (_touchedPoint != null && useMultiSeries)
            SliverToBoxAdapter(
              child: AnalyticsTouchDetails(
                data: _touchedPoint!,
                kindColor: _kindColor,
                onClose: () => setState(() => _touchedPoint = null),
              ),
            ),

          // Series legend
          SliverToBoxAdapter(
            child: SeriesLegend(
              series: _controller.groupedSeries,
              groupBy: _controller.groupBy,
            ),
          ),

          const SliverPadding(padding: EdgeInsets.only(bottom: 80)),
        ],
      ),
    );
  }

  Widget _buildDateNavRow(BuildContext context, bool isCustom) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(4, 4, 4, 0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          IconButton(
            icon: Icon(
              Icons.chevron_left_rounded,
              color: isCustom
                  ? Theme.of(context).hintColor.withOpacity(0.3)
                  : Theme.of(context).hintColor,
              size: 22,
            ),
            onPressed: isCustom ? null : () => _controller.shiftDateRange(-1),
            splashRadius: 20,
          ),
          Text(
            _controller.formattedDateRange,
            style: TextStyle(
              fontSize: 15,
              fontWeight: FontWeight.w600,
              color: Theme.of(context).textTheme.bodyLarge?.color,
            ),
          ),
          IconButton(
            icon: Icon(
              Icons.chevron_right_rounded,
              color: isCustom
                  ? Theme.of(context).hintColor.withOpacity(0.3)
                  : Theme.of(context).hintColor,
              size: 22,
            ),
            onPressed: isCustom ? null : () => _controller.shiftDateRange(1),
            splashRadius: 20,
          ),
        ],
      ),
    );
  }
}
