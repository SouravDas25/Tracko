import 'dart:math';

import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/component/horizontal_scroll_container.dart';
import 'package:tracko/pages/stats_page/controllers/stats_controller.dart';

const double _kLeftReserved = 64.0;
const double _kPointSpacing = 50.0;
const double _kMinPointSpacing = 20.0;
const double _kMaxPointSpacing = 120.0;

class StatsLineChart extends StatefulWidget {
  final bool loading;
  final String? error;
  final List<SeriesPoint> series;
  final double seriesMaxY;
  final Color kindColor;

  const StatsLineChart({
    Key? key,
    required this.loading,
    required this.error,
    required this.series,
    required this.seriesMaxY,
    required this.kindColor,
  }) : super(key: key);

  @override
  State<StatsLineChart> createState() => _StatsLineChartState();
}

class _StatsLineChartState extends State<StatsLineChart> {
  final ScrollController _scrollCtrl = ScrollController();
  double _pointSpacing = _kPointSpacing;

  // The usable chart width from the last LayoutBuilder pass.
  // Used to compute the minimum point spacing (= fit-all-in-screen).
  double _availableWidth = 0;

  // Multi-touch pinch tracking (Listener-based, no gesture arena).
  final Map<int, Offset> _pointers = {};
  double? _pinchStartDistance;
  double _spacingAtPinchStart = _kPointSpacing;

  @override
  void dispose() {
    _scrollCtrl.dispose();
    super.dispose();
  }

  void _onPointerDown(PointerDownEvent e) {
    _pointers[e.pointer] = e.localPosition;
    if (_pointers.length == 2) {
      final pts = _pointers.values.toList();
      _pinchStartDistance = (pts[0] - pts[1]).distance;
      _spacingAtPinchStart = _pointSpacing;
    }
  }

  void _onPointerMove(PointerMoveEvent e) {
    _pointers[e.pointer] = e.localPosition;
    if (_pointers.length == 2 && _pinchStartDistance != null) {
      final pts = _pointers.values.toList();
      final dist = (pts[0] - pts[1]).distance;
      final scale = dist / _pinchStartDistance!;
      final newSpacing = (_spacingAtPinchStart * scale)
          .clamp(_minPointSpacing, _kMaxPointSpacing);
      if ((newSpacing - _pointSpacing).abs() > 0.5) {
        setState(() => _pointSpacing = newSpacing);
      }
    }
  }

  void _onPointerUp(PointerUpEvent e) {
    _pointers.remove(e.pointer);
    if (_pointers.length < 2) _pinchStartDistance = null;
  }

  void _onPointerCancel(PointerCancelEvent e) {
    _pointers.remove(e.pointer);
    if (_pointers.length < 2) _pinchStartDistance = null;
  }

  double get _minPointSpacing {
    final seriesLen = widget.series.length;
    if (seriesLen <= 0 || _availableWidth <= 0) return _kMinPointSpacing;
    // Zoom out floor: the spacing at which the whole chart fits in the view.
    return max(_availableWidth / seriesLen, 1.0);
  }

  void _applyZoomDelta(double delta) {
    // delta > 0 → zoom out; delta < 0 → zoom in
    const sensitivity = 0.005;
    final factor = 1.0 - delta * sensitivity;
    final newSpacing =
        (_pointSpacing * factor).clamp(_minPointSpacing, _kMaxPointSpacing);
    if ((newSpacing - _pointSpacing).abs() > 0.3) {
      setState(() => _pointSpacing = newSpacing);
    }
  }

  @override
  Widget build(BuildContext context) {
    final loading = widget.loading;
    final error = widget.error;
    final series = widget.series;
    final seriesMaxY = widget.seriesMaxY;
    final kindColor = widget.kindColor;
    if (loading) {
      return const SizedBox(
        height: 220,
        child: Center(child: CircularProgressIndicator()),
      );
    }
    if (error != null) {
      return const SizedBox.shrink();
    }
    if (series.isEmpty) {
      return const SizedBox(
        height: 220,
        child: Center(child: Text('No chart data')),
      );
    }

    final baseSpots =
        series.map((p) => FlSpot(p.x, p.y)).toList(growable: false);

    // fl_chart won't render a useful line if minX == maxX (single point).
    final spots = (() {
      if (baseSpots.isEmpty) return <FlSpot>[];
      if (baseSpots.length == 1) {
        return <FlSpot>[
          baseSpots[0],
          FlSpot(baseSpots[0].x + 1, baseSpots[0].y)
        ];
      }
      return baseSpots;
    })();

    final maxX = spots.length == 1 ? 1.0 : spots.last.x;
    final maxY = seriesMaxY <= 0 ? 1.0 : seriesMaxY;
    final leftInterval = maxY <= 0 ? 1.0 : (maxY / 4);

    return Listener(
      onPointerDown: _onPointerDown,
      onPointerMove: _onPointerMove,
      onPointerUp: _onPointerUp,
      onPointerCancel: _onPointerCancel,
      child: LayoutBuilder(
        builder: (context, constraints) {
          final availableWidth = constraints.maxWidth - _kLeftReserved;
          if (_availableWidth != availableWidth) {
            // Store for use in _applyZoomDelta (post-frame to avoid
            // setState during build).
            WidgetsBinding.instance.addPostFrameCallback((_) {
              if (mounted) setState(() => _availableWidth = availableWidth);
            });
          }
          final minRequired = series.length * _pointSpacing;
          final chartWidth = max(availableWidth, minRequired);
          // Actual pixel gap between adjacent data points inside the plot area.
          // fl_chart reserves 64px on each side for axes, leaving chartWidth-128
          // for the data. With N points maxX = N-1 intervals.
          final plotWidth = chartWidth - 128.0;
          final pointGap = series.length > 1
              ? plotWidth / (series.length - 1)
              : plotWidth;
          // Minimum comfortable gap between two label centres (px).
          const double kMinLabelSpacing = 48.0;
          final labelStep = max(1, (kMinLabelSpacing / pointGap).ceil());

        return Stack(
          children: [
            HorizontalScrollContainer(
          controller: _scrollCtrl,
          width: chartWidth,
          height: 220,
          onCtrlScroll: _applyZoomDelta,
          child: LineChart(
            LineChartData(
              lineTouchData: LineTouchData(
                enabled: true,
                touchTooltipData: LineTouchTooltipData(
                  getTooltipItems: (List<LineBarSpot> touchedBarSpots) {
                    return touchedBarSpots.map((barSpot) {
                      final flSpot = barSpot;
                      if (flSpot.x < 0 || flSpot.x >= series.length) {
                        return null;
                      }
                      if (flSpot.x.toInt() >= series.length) return null;
                      final point = series[flSpot.x.toInt()];
                      return LineTooltipItem(
                        '${point.label}\n',
                        const TextStyle(
                          color: Colors.black,
                          fontWeight: FontWeight.bold,
                        ),
                        children: [
                          TextSpan(
                            text: CommonUtil.toCurrency(point.y),
                            style: TextStyle(
                              color: kindColor,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ],
                      );
                    }).toList();
                  },
                ),
              ),
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
                    reservedSize: 32,
                    interval: 1,
                    getTitlesWidget: (value, meta) {
                      if ((value - value.roundToDouble()).abs() > 0.001) {
                        return const SizedBox.shrink();
                      }
                      final idx = value.round();
                      if (idx < 0 || idx >= series.length) {
                        return const SizedBox.shrink();
                      }
                      // Show every labelStep-th label only.
                      // idx 0 always passes (0 % anything == 0).
                      if (idx % labelStep != 0) {
                        return const SizedBox.shrink();
                      }
                      return SideTitleWidget(
                        axisSide: meta.axisSide,
                        space: 8,
                        child: Text(
                          series[idx].label,
                          style: const TextStyle(fontSize: 10),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
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
                  gradient: LinearGradient(
                    colors: [
                      kindColor.withOpacity(0.5),
                      kindColor,
                    ],
                  ),
                  barWidth: 4,
                  isStrokeCapRound: true,
                  dotData: FlDotData(
                    show: baseSpots.length == 1,
                    getDotPainter: (spot, percent, barData, index) {
                      return FlDotCirclePainter(
                        radius: 4,
                        color: kindColor,
                        strokeWidth: 2,
                        strokeColor: Colors.white,
                      );
                    },
                  ),
                  belowBarData: BarAreaData(
                    show: true,
                    gradient: LinearGradient(
                      colors: [
                        kindColor.withOpacity(0.25),
                        kindColor.withOpacity(0.0),
                      ],
                      begin: Alignment.topCenter,
                      end: Alignment.bottomCenter,
                    ),
                  ),
                ),
              ],
            ),
          ),
            ),
            // Zoom buttons overlay
            Positioned(
              top: 4,
              right: 4,
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  _ZoomButton(
                    icon: Icons.remove,
                    onTap: () => _applyZoomDelta(60),
                  ),
                  const SizedBox(width: 2),
                  _ZoomButton(
                    icon: Icons.add,
                    onTap: () => _applyZoomDelta(-60),
                  ),
                ],
              ),
            ),
          ],
        );
        },
      ),
    );
  }
}

class _ZoomButton extends StatelessWidget {
  final IconData icon;
  final VoidCallback onTap;

  const _ZoomButton({required this.icon, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.black.withOpacity(0.15),
      borderRadius: BorderRadius.circular(4),
      child: InkWell(
        borderRadius: BorderRadius.circular(4),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(4),
          child: Icon(icon, size: 14, color: Colors.white),
        ),
      ),
    );
  }
}
