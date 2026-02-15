import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/enums.dart';
import 'package:tracko/component/horizontal_scroll_container.dart';
import 'package:tracko/component/Indicator.dart';
import 'package:tracko/models/category.dart';
import 'package:tracko/config/api_config.dart';
import 'package:tracko/services/api_client.dart';
import 'package:tracko/scratch/ChartUtil.dart';

import 'category_transactions_page.dart';

enum StatsRange { weekly, monthly, yearly }

enum StatsKind { expense, income }

class StatsPage extends StatefulWidget {
  const StatsPage({super.key});

  @override
  State<StatsPage> createState() => _StatsPageState();
}

class _StatsPageState extends State<StatsPage> {
  StatsRange _range = StatsRange.monthly;
  StatsKind _kind = StatsKind.expense;

  DateTime _anchorDate = DateTime.now();

  final ScrollController _chartScrollController = ScrollController();

  bool _loading = false;
  String? _error;

  double _total = 0.0;
  List<_CategoryStat> _stats = const [];
  List<_SeriesPoint> _series = const [];
  double _seriesMaxY = 0.0;

  List<ChartEntry> _pieSeries = const [];

  DateTime? _periodStart;
  DateTime? _periodEndExclusive;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Widget _buildPieChart() {
    final chartHeight =
        (MediaQuery.of(context).size.height * 0.4).clamp(240.0, 520.0);
    if (_loading) {
      return const SizedBox(
        height: 260,
        child: Center(child: CircularProgressIndicator()),
      );
    }
    if (_error != null) {
      return const SizedBox.shrink();
    }
    if (_pieSeries.isEmpty) {
      return const SizedBox(
        height: 260,
        child: Center(child: Text('No data')),
      );
    }

    final seriesList = List<ChartEntry>.from(_pieSeries);
    ChartUtil.prepareForChart(seriesList);

    final indicators = seriesList
        .map(
          (ce) => Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8.0),
            child: Indicator(
              color: ce.color,
              text: ce.label,
              isSquare: true,
              size: 16,
              textColor: Colors.grey,
            ),
          ),
        )
        .toList(growable: false);

    final sections = seriesList
        .map(
          (ce) => PieChartSectionData(
            color: ce.color,
            value: ce.percentage,
            title: '${ce.percentage.toInt()}%',
            radius: 70,
            titlePositionPercentageOffset: 0.55,
          ),
        )
        .toList(growable: false);

    return SizedBox(
      width: double.infinity,
      height: chartHeight,
      child: Column(
        children: [
          const SizedBox(height: 12),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: indicators,
            ),
          ),
          const SizedBox(height: 12),
          Expanded(
            child: AspectRatio(
              aspectRatio: 1.8,
              child: PieChart(
                PieChartData(
                  startDegreeOffset: 180,
                  borderData: FlBorderData(show: false),
                  sectionsSpace: 1,
                  sections: sections,
                  centerSpaceRadius: 40,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  @override
  void dispose() {
    _chartScrollController.dispose();
    super.dispose();
  }

  DateTime _shiftAnchor(DateTime anchor, int delta) {
    // delta: -1 previous period, +1 next period
    switch (_range) {
      case StatsRange.weekly:
        return anchor.add(Duration(days: 7 * delta));
      case StatsRange.monthly:
        // Use a safe day-of-month so we don't overflow on months with fewer days.
        return DateTime(anchor.year, anchor.month + delta, 15);
      case StatsRange.yearly:
        // Use a safe day-of-month so we don't overflow (e.g., Feb 29/30/31).
        return DateTime(anchor.year + delta, anchor.month, 15);
    }
  }

  String _fmtDate(DateTime d) {
    final mm = d.month.toString().padLeft(2, '0');
    final dd = d.day.toString().padLeft(2, '0');
    return '${d.year}-$mm-$dd';
  }

  String _monthLabel(int month) {
    const months = [
      'Jan',
      'Feb',
      'Mar',
      'Apr',
      'May',
      'Jun',
      'Jul',
      'Aug',
      'Sep',
      'Oct',
      'Nov',
      'Dec'
    ];
    if (month < 1 || month > 12) return '';
    return months[month - 1];
  }

  Widget _buildLineChart() {
    if (_loading) {
      return const SizedBox(
        height: 220,
        child: Center(child: CircularProgressIndicator()),
      );
    }
    if (_error != null) {
      return const SizedBox.shrink();
    }
    if (_series.isEmpty) {
      return const SizedBox(
        height: 220,
        child: Center(child: Text('No chart data')),
      );
    }

    final baseSpots =
        _series.map((p) => FlSpot(p.x, p.y)).toList(growable: false);

    // fl_chart won't render a useful line if minX == maxX (single point).
    final spots = (() {
      final initial = baseSpots.length == 1
          ? <FlSpot>[baseSpots[0], FlSpot(baseSpots[0].x + 1, baseSpots[0].y)]
          : baseSpots;
      if (initial.isEmpty) return initial;
      // Add a trailing zero point so the chart visually ends at baseline.
      return <FlSpot>[...initial, FlSpot(initial.last.x + 1, 0)];
    })();

    final maxX = spots.length == 1 ? 1.0 : spots.last.x;
    final maxY = _seriesMaxY <= 0 ? 1.0 : _seriesMaxY;
    final leftInterval = maxY <= 0 ? 1.0 : (maxY / 4);

    const bottomInterval = 1.0;

    // Calculate dynamic width: minimum 60px per datapoint for readable spacing
    final chartWidth = (_series.length * 60.0).clamp(300.0, double.infinity);

    return HorizontalScrollContainer(
      controller: _chartScrollController,
      width: chartWidth,
      height: 220,
      child: LineChart(
        LineChartData(
          lineTouchData: LineTouchData(enabled: false),
          minX: 0,
          maxX: maxX,
          minY: 0,
          maxY: maxY,
          gridData: FlGridData(show: true, drawVerticalLine: false),
          borderData: FlBorderData(show: false),
          titlesData: FlTitlesData(
            rightTitles:
                const AxisTitles(sideTitles: SideTitles(showTitles: false)),
            topTitles:
                const AxisTitles(sideTitles: SideTitles(showTitles: false)),
            leftTitles: AxisTitles(
              sideTitles: SideTitles(
                showTitles: true,
                reservedSize: 64,
                interval: leftInterval,
                getTitlesWidget: (value, meta) {
                  return SideTitleWidget(
                    axisSide: meta.axisSide,
                    space: 6,
                    child: Text(
                      CommonUtil.toCurrency(value),
                      style: const TextStyle(fontSize: 9),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  );
                },
              ),
            ),
            bottomTitles: AxisTitles(
              sideTitles: SideTitles(
                showTitles: true,
                reservedSize: 52,
                interval: 1,
                getTitlesWidget: (value, meta) {
                  // fl_chart may call this for non-integer tick values; only label exact indices.
                  if ((value - value.roundToDouble()).abs() > 0.001) {
                    return const SizedBox.shrink();
                  }
                  final idx = value.round();
                  if (idx < 0 || idx >= _series.length)
                    return const SizedBox.shrink();
                  return SideTitleWidget(
                    axisSide: meta.axisSide,
                    space: 6,
                    child: Transform.rotate(
                      angle: -0.6,
                      child: Text(
                        _series[idx].label,
                        style: const TextStyle(fontSize: 9),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  );
                },
              ),
            ),
          ),
          lineBarsData: [
            LineChartBarData(
              spots: spots,
              isCurved: true,
              color: _kindColor(),
              barWidth: 3,
              dotData: FlDotData(show: baseSpots.length == 1),
              belowBarData: BarAreaData(
                show: true,
                color: _kindColor().withOpacity(0.15),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _load({DateTime? anchorOverride}) async {
    if (!mounted) return;

    final anchor = anchorOverride ?? _anchorDate;
    debugPrint(
        'StatsPage load: range=${_range.name} kind=${_kind.name} anchor=${_fmtDate(anchor)}');
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final kindType = _kind == StatsKind.expense
          ? TransactionType.DEBIT
          : TransactionType.CREDIT;
      final api = ApiClient();
      final res = await api.get<Map<String, dynamic>>(
        '${ApiConfig.stats}/summary',
        query: {
          'range': _range.name,
          'transactionType': kindType,
          'date': _fmtDate(anchor),
        },
      );

      final total = ((res['total'] as num?) ?? 0).toDouble();
      final periodStartStr = (res['periodStart'] as String?) ?? '';
      final periodEndStr = (res['periodEnd'] as String?) ?? '';
      debugPrint(
          'StatsPage resp: periodStart=$periodStartStr periodEnd=$periodEndStr seriesLen=${(res['series'] as List?)?.length ?? 0}');

      DateTime? periodStart;
      DateTime? periodEndExclusive;
      try {
        if (periodStartStr.isNotEmpty) {
          periodStart = DateTime.parse(periodStartStr);
        }
        if (periodEndStr.isNotEmpty) {
          final endInclusive = DateTime.parse(periodEndStr);
          periodEndExclusive = endInclusive.add(const Duration(days: 1));
        }
      } catch (_) {
        // ignore
      }

      final seriesJson = (res['series'] as List?) ?? const [];
      final series = <_SeriesPoint>[];
      for (int i = 0; i < seriesJson.length; i++) {
        final item = seriesJson[i];
        if (item is! Map) continue;
        final label = (item['label'] as String?) ?? '';
        final value = ((item['value'] as num?) ?? 0).toDouble();
        series.add(_SeriesPoint(x: i.toDouble(), y: value, label: label));
      }
      final maxY = series.isEmpty
          ? 0.0
          : series.map((p) => p.y).reduce((a, b) => a > b ? a : b);

      final catsJson = (res['categories'] as List?) ?? const [];
      final stats = <_CategoryStat>[];
      for (final item in catsJson) {
        if (item is! Map) continue;
        final cid = (item['categoryId'] as num?)?.toInt() ?? 0;
        if (cid == 0) continue;
        final name = (item['categoryName'] as String?) ?? 'Category $cid';
        final amount = ((item['amount'] as num?) ?? 0).toDouble();
        stats.add(
            _CategoryStat(categoryId: cid, categoryName: name, amount: amount));
      }

      final pieSeries = <ChartEntry>[];
      for (final s in stats) {
        final v = s.amount.round();
        if (v <= 0) continue;
        pieSeries.add(ChartEntry(s.categoryId, s.categoryName, v));
      }
      pieSeries.sort((a, b) => b.value.compareTo(a.value));
      final limitedPieSeries =
          pieSeries.length > 6 ? pieSeries.sublist(0, 6) : pieSeries;

      if (!mounted) return;
      setState(() {
        _stats = stats;
        _total = total;
        _series = series;
        _seriesMaxY = maxY;
        _pieSeries = limitedPieSeries;
        _periodStart = periodStart;
        _periodEndExclusive = periodEndExclusive;
        _anchorDate = anchor;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = e.toString();
        _series = const [];
        _seriesMaxY = 0.0;
        _periodStart = null;
        _periodEndExclusive = null;
        _pieSeries = const [];
      });
    }
  }

  String _rangeLabel(StatsRange r) {
    switch (r) {
      case StatsRange.weekly:
        return 'Weekly';
      case StatsRange.monthly:
        return 'Monthly';
      case StatsRange.yearly:
        return 'Yearly';
    }
  }

  String _kindLabel(StatsKind k) {
    switch (k) {
      case StatsKind.expense:
        return 'Expense';
      case StatsKind.income:
        return 'Income';
    }
  }

  Color _kindColor() {
    return _kind == StatsKind.expense ? Colors.red : Colors.green;
  }

  @override
  Widget build(BuildContext context) {
    final rangeStart = _periodStart;
    final rangeEndExclusive = _periodEndExclusive;
    final rangeText = (rangeStart != null && rangeEndExclusive != null)
        ? '${rangeStart.toIso8601String().split('T').first} to ${rangeEndExclusive.subtract(const Duration(days: 1)).toIso8601String().split('T').first}'
        : '';

    return ListView(
      padding: EdgeInsets.zero,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
          child: Row(
            children: [
              Expanded(
                child: DropdownButton<StatsRange>(
                  isExpanded: true,
                  value: _range,
                  items: StatsRange.values
                      .map(
                        (r) => DropdownMenuItem(
                          value: r,
                          child: Text(_rangeLabel(r)),
                        ),
                      )
                      .toList(),
                  onChanged: (v) {
                    if (v == null) return;
                    setState(() {
                      _range = v;
                      _anchorDate = DateTime.now();
                    });
                    _load();
                  },
                ),
              ),
              const SizedBox(width: 12),
              ToggleButtons(
                isSelected: [
                  _kind == StatsKind.expense,
                  _kind == StatsKind.income,
                ],
                onPressed: (idx) {
                  setState(() {
                    _kind = idx == 0 ? StatsKind.expense : StatsKind.income;
                  });
                  _load();
                },
                children: const [
                  Padding(
                    padding: EdgeInsets.symmetric(horizontal: 12),
                    child: Text('Expense'),
                  ),
                  Padding(
                    padding: EdgeInsets.symmetric(horizontal: 12),
                    child: Text('Income'),
                  ),
                ],
              ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
          child: Card(
            elevation: 2,
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            child: Padding(
              padding: const EdgeInsets.all(12.0),
              child: _buildLineChart(),
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
          child: Row(
            children: [
              IconButton(
                icon: const Icon(Icons.chevron_left),
                onPressed: _loading
                    ? null
                    : () {
                        final nextAnchor = _shiftAnchor(_anchorDate, -1);
                        setState(() {
                          _anchorDate = nextAnchor;
                        });
                        _load(anchorOverride: nextAnchor);
                      },
              ),
              Expanded(
                child: Align(
                  alignment: Alignment.center,
                  child: Text(
                    rangeText,
                    style: TextStyle(color: Colors.grey.shade600),
                  ),
                ),
              ),
              IconButton(
                icon: const Icon(Icons.chevron_right),
                onPressed: _loading
                    ? null
                    : () {
                        final nextAnchor = _shiftAnchor(_anchorDate, 1);
                        setState(() {
                          _anchorDate = nextAnchor;
                        });
                        _load(anchorOverride: nextAnchor);
                      },
              ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
          child: Text(
            '${_kindLabel(_kind)}: ${CommonUtil.toCurrency(_total)}',
            style: const TextStyle(fontWeight: FontWeight.bold),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
          child: Card(
            elevation: 2,
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            child: Padding(
              padding: const EdgeInsets.all(12.0),
              child: _buildPieChart(),
            ),
          ),
        ),
        if (_loading)
          const Padding(
            padding: EdgeInsets.only(top: 40),
            child: Center(child: CircularProgressIndicator()),
          )
        else if (_error != null)
          Padding(
            padding: const EdgeInsets.only(top: 40),
            child: Center(
              child: Text(
                _error!,
                style: const TextStyle(color: Colors.red),
              ),
            ),
          )
        else if (_stats.isEmpty)
          const Padding(
            padding: EdgeInsets.only(top: 40),
            child: Center(child: Text('No data')),
          )
        else
          ..._stats.map((s) {
            final cid = s.categoryId;
            final cat = Category();
            cat.id = cid;
            cat.name = s.categoryName;
            return Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
              child: Card(
                elevation: 1,
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16)),
                child: ListTile(
                  title: Text(s.categoryName),
                  trailing: Text(
                    CommonUtil.toCurrency(s.amount),
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      color: _kindColor(),
                    ),
                  ),
                  onTap: () {
                    if (rangeStart == null || rangeEndExclusive == null) return;
                    final txType = _kind == StatsKind.expense
                        ? TransactionType.DEBIT
                        : TransactionType.CREDIT;
                    Navigator.of(context).push(
                      MaterialPageRoute(
                        builder: (context) => CategoryTransactionsPage(
                          category: cat,
                          startDate: rangeStart,
                          endDate: rangeEndExclusive,
                          range: _range.name,
                          transactionType: txType,
                        ),
                      ),
                    );
                  },
                ),
              ),
            );
          }).toList(growable: false),
      ],
    );
  }
}

class _CategoryStat {
  final int categoryId;
  final String categoryName;
  final double amount;

  const _CategoryStat({
    required this.categoryId,
    required this.categoryName,
    required this.amount,
  });
}

class _SeriesPoint {
  final double x;
  final double y;
  final String label;

  const _SeriesPoint({
    required this.x,
    required this.y,
    required this.label,
  });
}
