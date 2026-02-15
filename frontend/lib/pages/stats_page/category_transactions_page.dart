import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/enums.dart';
import 'package:tracko/component/horizontal_scroll_container.dart';
import 'package:tracko/component/TransactionTile.dart';
import 'package:tracko/config/api_config.dart';
import 'package:tracko/controllers/TransactionController.dart';
import 'package:tracko/models/category.dart';
import 'package:tracko/models/transaction.dart';
import 'package:tracko/repositories/transaction_repository.dart';
import 'package:tracko/services/api_client.dart';
import 'package:tracko/services/SessionService.dart';

class CategoryTransactionsPage extends StatefulWidget {
  final Category category;
  final DateTime startDate;
  final DateTime endDate;
  final String range;
  final int transactionType; // DEBIT or CREDIT

  const CategoryTransactionsPage({
    super.key,
    required this.category,
    required this.startDate,
    required this.endDate,
    required this.range,
    required this.transactionType,
  });

  @override
  State<CategoryTransactionsPage> createState() =>
      _CategoryTransactionsPageState();
}

class _CategoryTransactionsPageState extends State<CategoryTransactionsPage> {
  bool _loading = false;
  String? _error;
  List<Transaction> _transactions = const [];

  double _total = 0.0;
  List<_SeriesPoint> _series = const [];
  double _seriesMaxY = 0.0;
  final ScrollController _chartScrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _chartScrollController.dispose();
    super.dispose();
  }

  String _fmtDate(DateTime d) {
    final mm = d.month.toString().padLeft(2, '0');
    final dd = d.day.toString().padLeft(2, '0');
    return '${d.year}-$mm-$dd';
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
    final spots = (() {
      final initial = baseSpots.length == 1
          ? <FlSpot>[baseSpots[0], FlSpot(baseSpots[0].x + 1, baseSpots[0].y)]
          : baseSpots;
      if (initial.isEmpty) return initial;
      return <FlSpot>[...initial, FlSpot(initial.last.x + 1, 0)];
    })();

    final maxX = spots.length == 1 ? 1.0 : spots.last.x;
    final maxY = _seriesMaxY <= 0 ? 1.0 : _seriesMaxY;
    final leftInterval = maxY <= 0 ? 1.0 : (maxY / 4);

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
              color: Colors.blue,
              barWidth: 3,
              dotData: FlDotData(show: baseSpots.length == 1),
              belowBarData: BarAreaData(
                show: true,
                color: Colors.blue.withOpacity(0.15),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _load() async {
    if (!mounted) return;
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final api = ApiClient();
      final txRepo = TransactionRepository();
      final userId = (SessionService.currentUser().id ?? '').toString();

      final catId = (widget.category.id ?? 0);
      if (catId == 0) {
        throw Exception('Invalid category');
      }

      final statsRes = await api.get<Map<String, dynamic>>(
        '${ApiConfig.stats}/category-summary',
        query: {
          'range': widget.range,
          'transactionType': widget.transactionType,
          'categoryId': catId,
          // Use the period start as anchor so backend computes the same window.
          'date': _fmtDate(widget.startDate),
        },
      );

      final total = ((statsRes['total'] as num?) ?? 0).toDouble();
      final seriesJson = (statsRes['series'] as List?) ?? const [];
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

      final txs = await txRepo.getByUserIdAndDateRange(
        userId,
        startDate: widget.startDate,
        endDate: widget.endDate,
      );

      final filtered = <Transaction>[];
      for (final t in txs) {
        if (t.transactionType == TransactionType.TRANSFER) continue;
        if (t.transactionType != widget.transactionType) continue;
        if (t.categoryId != (widget.category.id ?? 0)) continue;
        t.category = widget.category;
        filtered.add(t);
      }

      filtered.sort((a, b) => b.date.compareTo(a.date));

      if (!mounted) return;
      setState(() {
        _transactions = filtered;
        _series = series;
        _seriesMaxY = maxY;
        _total = total;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = e.toString();
        _series = const [];
        _seriesMaxY = 0.0;
        _total = 0.0;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final title = widget.category.name;

    return Scaffold(
      appBar: AppBar(
        title: Text(title),
        centerTitle: true,
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : (_error != null)
              ? Center(child: Text(_error!))
              : Column(
                  children: [
                    Padding(
                      padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
                      child: Card(
                        elevation: 2,
                        shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12)),
                        child: Padding(
                          padding: const EdgeInsets.all(12.0),
                          child: _buildLineChart(),
                        ),
                      ),
                    ),
                    Padding(
                      padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
                      child: Align(
                        alignment: Alignment.centerLeft,
                        child: Text(
                          'Total: ${CommonUtil.toCurrency(_total)}',
                          style: const TextStyle(fontWeight: FontWeight.bold),
                        ),
                      ),
                    ),
                    Expanded(
                      child: (_transactions.isEmpty)
                          ? const Center(child: Text('No transactions'))
                          : ListView.builder(
                              itemCount: _transactions.length,
                              itemBuilder: (context, index) {
                                final tx = _transactions[index];
                                return TransactionTile(this, tx,
                                    (dynamic parent, Transaction t) async {
                                  await TransactionController.deleteById(
                                      t.id ?? 0);
                                  await _load();
                                });
                              },
                            ),
                    ),
                  ],
                ),
    );
  }
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
