import 'package:flutter/material.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:intl/intl.dart';
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
  final int? accountId;

  const CategoryTransactionsPage({
    super.key,
    required this.category,
    required this.startDate,
    required this.endDate,
    required this.range,
    required this.transactionType,
    this.accountId,
  });

  @override
  State<CategoryTransactionsPage> createState() =>
      _CategoryTransactionsPageState();
}

class _CategoryTransactionsPageState extends State<CategoryTransactionsPage> {
  bool _loading = false;
  String? _error;
  List<Transaction> _transactions = [];

  double _total = 0.0;
  List<_SeriesPoint> _series = const [];
  double _seriesMaxY = 0.0;

  // Pagination
  final ScrollController _scrollController = ScrollController();
  final int _pageSize = 50;
  int _page = 0;
  bool _isLoadingMore = false;
  bool _hasMore = true;

  @override
  void initState() {
    super.initState();
    _scrollController.addListener(_onScroll);
    _load();
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels >=
        _scrollController.position.maxScrollExtent - 200) {
      _loadMore();
    }
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

    // Dynamic interval for bottom titles to prevent crowding
    // Aim for about 5-6 labels max
    double bottomInterval = 1.0;
    if (maxX > 6) {
      bottomInterval = (maxX / 6).ceilToDouble();
    }

    return SizedBox(
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
            rightTitles: AxisTitles(
              sideTitles: SideTitles(
                showTitles: true,
                reservedSize: 64,
                getTitlesWidget: (value, meta) => const SizedBox.shrink(),
              ),
            ),
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
                interval: bottomInterval,
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
      _page = 0;
      _hasMore = true;
      _transactions = [];
    });

    try {
      // 1. Load Chart Data (Summary)
      final api = ApiClient();

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
          if (widget.accountId != null) 'accountId': widget.accountId,
          if (widget.range == 'custom') ...{
            'startDate': _fmtDate(widget.startDate),
            'endDate': _fmtDate(widget.endDate),
          }
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

      if (mounted) {
        setState(() {
          _series = series;
          _seriesMaxY = maxY;
          _total = total;
        });
      }

      // 2. Load Initial Page of Transactions
      await _loadTransactions();

      if (mounted) {
        setState(() {
          _loading = false;
        });
      }
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

  Future<void> _loadMore() async {
    if (_isLoadingMore || !_hasMore) return;
    setState(() {
      _isLoadingMore = true;
    });
    await _loadTransactions();
    if (mounted) {
      setState(() {
        _isLoadingMore = false;
      });
    }
  }

  Future<void> _loadTransactions() async {
    try {
      final txRepo = TransactionRepository();
      final catId = (widget.category.id ?? 0);

      final txs = await txRepo.getAll(
        startDate: widget.startDate,
        endDate: widget.endDate,
        categoryId: catId,
        accountIds: widget.accountId != null ? [widget.accountId!] : null,
        page: _page,
        size: _pageSize,
        expand: false,
      );

      final filtered = <Transaction>[];
      for (final t in txs) {
        if (t.transactionType == TransactionType.TRANSFER) continue;
        if (t.transactionType != widget.transactionType) continue;
        if (t.categoryId != (widget.category.id ?? 0)) continue;
        // Verify account match locally if repository didn't filter strictly enough
        if (widget.accountId != null && t.accountId != widget.accountId)
          continue;

        t.category = widget.category;
        filtered.add(t);
      }

      filtered.sort((a, b) => b.date.compareTo(a.date));

      if (txs.length < _pageSize) {
        _hasMore = false;
      }

      if (mounted) {
        setState(() {
          _transactions.addAll(filtered);
          _page++;
        });
      }
    } catch (e) {
      debugPrint('Error loading more transactions: $e');
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
                          child: Column(
                            children: [
                              Text(
                                '${DateFormat('MMM dd, yyyy').format(widget.startDate)} - ${DateFormat('MMM dd, yyyy').format(widget.endDate.subtract(const Duration(days: 1)))}',
                                style: TextStyle(
                                  fontSize: 12,
                                  fontWeight: FontWeight.bold,
                                  color: Theme.of(context).hintColor,
                                ),
                              ),
                              const SizedBox(height: 12),
                              _buildLineChart(),
                            ],
                          ),
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
                      child: (_transactions.isEmpty && !_hasMore)
                          ? const Center(child: Text('No transactions'))
                          : ListView.builder(
                              controller: _scrollController,
                              itemCount:
                                  _transactions.length + (_hasMore ? 1 : 0),
                              itemBuilder: (context, index) {
                                if (index >= _transactions.length) {
                                  return const Padding(
                                    padding: EdgeInsets.all(16.0),
                                    child: Center(
                                      child: SizedBox(
                                        width: 24,
                                        height: 24,
                                        child: CircularProgressIndicator(
                                            strokeWidth: 2),
                                      ),
                                    ),
                                  );
                                }
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
