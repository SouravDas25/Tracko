import 'dart:math';

import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/component/horizontal_scroll_container.dart';
import 'package:tracko/pages/analytics_page/models/analytics_models.dart';
import 'package:tracko/pages/stats_page/controllers/stats_controller.dart';

const double _kLeftReserved = 64.0;
const double _kPointSpacing = 50.0;
const double _kMinPointSpacing = 20.0;
const double _kMaxPointSpacing = 120.0;

/// Data for a single touched point across all series.
class TouchedPointData {
  final int xIndex;
  final String label;
  final List<TouchedSeriesValue> values;

  const TouchedPointData({
    required this.xIndex,
    required this.label,
    required this.values,
  });
}

class TouchedSeriesValue {
  final String name;
  final double value;
  final Color color;

  const TouchedSeriesValue({
    required this.name,
    required this.value,
    required this.color,
  });
}

class StatsLineChart extends StatefulWidget {
  final bool loading;
  final String? error;
  final List<SeriesPoint> series;
  final double seriesMaxY;
  final Color kindColor;
  final List<NamedSeries>? multiSeries;
  final ValueChanged<TouchedPointData?>? onPointTouched;

  const StatsLineChart({
    Key? key,
    required this.loading,
    required this.error,
    required this.series,
    required this.seriesMaxY,
    required this.kindColor,
    this.multiSeries,
    this.onPointTouched,
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

  bool get _isMultiSeries =>
      widget.multiSeries != null && widget.multiSeries!.isNotEmpty;

  /// The effective number of data points along the x-axis.
  /// In multi-series mode this is the length of the longest series.
  int get _effectiveSeriesLength {
    if (_isMultiSeries) {
      return widget.multiSeries!
          .fold<int>(0, (prev, s) => max(prev, s.points.length));
    }
    return widget.series.length;
  }

  @override
  void didUpdateWidget(covariant StatsLineChart oldWidget) {
    super.didUpdateWidget(oldWidget);
    final changed = widget.series != oldWidget.series ||
        widget.multiSeries != oldWidget.multiSeries;
    if (changed && _effectiveSeriesLength > 0) {
      WidgetsBinding.instance.addPostFrameCallback((_) => _scrollToEnd());
    }
  }

  void _scrollToEnd() {
    if (_scrollCtrl.hasClients && _scrollCtrl.position.maxScrollExtent > 0) {
      _scrollCtrl.jumpTo(_scrollCtrl.position.maxScrollExtent);
    }
  }

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
    final seriesLen = _effectiveSeriesLength;
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

  /// Ensure fl_chart gets at least two spots so it can draw a line.
  static List<FlSpot> _ensureMinSpots(List<FlSpot> baseSpots) {
    if (baseSpots.isEmpty) return <FlSpot>[];
    if (baseSpots.length == 1) {
      return <FlSpot>[
        baseSpots[0],
        FlSpot(baseSpots[0].x + 1, baseSpots[0].y),
      ];
    }
    return baseSpots;
  }

  @override
  Widget build(BuildContext context) {
    final loading = widget.loading;
    final error = widget.error;
    final series = widget.series;
    final seriesMaxY = widget.seriesMaxY;
    final kindColor = widget.kindColor;
    final useMulti = _isMultiSeries;

    if (loading) {
      return const SizedBox(
        height: 220,
        child: Center(child: CircularProgressIndicator()),
      );
    }
    if (error != null) {
      return const SizedBox.shrink();
    }

    // Empty-data guard for both modes.
    if (!useMulti && series.isEmpty) {
      return const SizedBox(
        height: 220,
        child: Center(child: Text('No chart data')),
      );
    }
    if (useMulti && widget.multiSeries!.every((s) => s.points.isEmpty)) {
      return const SizedBox(
        height: 220,
        child: Center(child: Text('No chart data')),
      );
    }

    final int effectiveLen = _effectiveSeriesLength;

    // For x-axis labels use the first series' points (longest in multi-mode
    // would also work, but the first is the convention from the task spec).
    final List<SeriesPoint> labelSeries =
        useMulti ? widget.multiSeries!.first.points : series;

    // --- Build line bars & compute maxX / maxY ---
    late final double maxX;
    late final double maxY;
    late final List<LineChartBarData> lineBarsData;

    if (useMulti) {
      double computedMaxX = 0;
      double computedMaxY = 0;
      final List<LineChartBarData> bars = [];

      for (final ns in widget.multiSeries!) {
        final baseSpots =
            ns.points.map((p) => FlSpot(p.x, p.y)).toList(growable: false);
        final spots = _ensureMinSpots(baseSpots);
        if (spots.isNotEmpty) {
          computedMaxX = max(computedMaxX, spots.last.x);
          for (final s in spots) {
            computedMaxY = max(computedMaxY, s.y);
          }
        }
        bars.add(
          LineChartBarData(
            spots: spots,
            isCurved: true,
            color: ns.color,
            barWidth: 3,
            isStrokeCapRound: true,
            dotData: FlDotData(
              show: baseSpots.length == 1,
              getDotPainter: (spot, percent, barData, index) {
                return FlDotCirclePainter(
                  radius: 4,
                  color: ns.color,
                  strokeWidth: 2,
                  strokeColor: Colors.white,
                );
              },
            ),
            belowBarData: BarAreaData(show: false),
          ),
        );
      }
      maxX = computedMaxX <= 0 ? 1.0 : computedMaxX;
      maxY = computedMaxY <= 0 ? 1.0 : computedMaxY;
      lineBarsData = bars;
    } else {
      // Original single-series path.
      final baseSpots =
          series.map((p) => FlSpot(p.x, p.y)).toList(growable: false);
      final spots = _ensureMinSpots(baseSpots);
      maxX = spots.length == 1 ? 1.0 : (spots.isEmpty ? 1.0 : spots.last.x);
      maxY = seriesMaxY <= 0 ? 1.0 : seriesMaxY;
      lineBarsData = [
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
      ];
    }

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
            WidgetsBinding.instance.addPostFrameCallback((_) {
              if (mounted) {
                setState(() => _availableWidth = availableWidth);
                _scrollToEnd();
              }
            });
          }
          final minRequired = effectiveLen * _pointSpacing;
          final chartWidth = max(availableWidth, minRequired);
          final plotWidth = chartWidth - 128.0;
          final pointGap =
              effectiveLen > 1 ? plotWidth / (effectiveLen - 1) : plotWidth;
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
                      touchCallback: useMulti && widget.onPointTouched != null
                          ? (FlTouchEvent event, LineTouchResponse? response) {
                              if (event is FlTapUpEvent ||
                                  event is FlLongPressEnd ||
                                  event is FlPanEndEvent) {
                                // Touch ended — keep showing last data
                                return;
                              }
                              if (response == null ||
                                  response.lineBarSpots == null ||
                                  response.lineBarSpots!.isEmpty) {
                                widget.onPointTouched!(null);
                                return;
                              }
                              final spots = response.lineBarSpots!;
                              // Use the X-index from the first touched spot (they should all align vertically)
                              final xIdx = spots.first.x.toInt();

                              String label = '';
                              final values = <TouchedSeriesValue>[];

                              // Iterate through ALL series to find values at this X index
                              for (int i = 0;
                                  i < widget.multiSeries!.length;
                                  i++) {
                                final ns = widget.multiSeries![i];
                                // Find the point with x == xIdx
                                // Assuming points are sorted by x or x is the index.
                                // Safe way: look for it or direct access if valid.
                                // Since we built points with x=index, direct access is likely safe
                                // IF list is complete. Let's start with safe access.

                                SeriesPoint? point;
                                if (xIdx >= 0 && xIdx < ns.points.length) {
                                  // Check if the point at index actually matches the x we want
                                  // (Handling potential sparse data or unsorted, though controller generates sorted)
                                  if (ns.points[xIdx].x.toInt() == xIdx) {
                                    point = ns.points[xIdx];
                                  } else {
                                    // Fallback search
                                    try {
                                      point = ns.points.firstWhere(
                                          (p) => p.x.toInt() == xIdx);
                                    } catch (_) {}
                                  }
                                }

                                if (point != null) {
                                  // Use the label from the first series we find (or any valid one)
                                  if (label.isEmpty) {
                                    label = point.label;
                                  }
                                  // Only add if value is not 0? Or show all?
                                  // Usually showing all is better for comparison, or maybe filter 0s.
                                  // The user said "only show few", implying they want to see more.
                                  // Let's show all, but maybe sort them by value descending?

                                  values.add(TouchedSeriesValue(
                                    name: ns.name,
                                    value: point.y,
                                    color: ns.color,
                                  ));
                                }
                              }

                              // Optional: Sort by value descending for better readability
                              values.sort((a, b) => b.value.compareTo(a.value));

                              widget.onPointTouched!(TouchedPointData(
                                xIndex: xIdx,
                                label: label,
                                values: values,
                              ));
                            }
                          : null,
                      touchTooltipData: LineTouchTooltipData(
                        // Hide built-in tooltip in multi-series mode
                        getTooltipColor:
                            useMulti && widget.onPointTouched != null
                                ? (_) => Colors.transparent
                                : (_) => Colors.blueGrey,
                        tooltipPadding:
                            useMulti && widget.onPointTouched != null
                                ? EdgeInsets.zero
                                : const EdgeInsets.all(8),
                        getTooltipItems: (List<LineBarSpot> touchedBarSpots) {
                          if (useMulti && widget.onPointTouched != null) {
                            return touchedBarSpots
                                .map((_) => null as LineTooltipItem?)
                                .toList();
                          }
                          return touchedBarSpots.map((barSpot) {
                            if (useMulti) {
                              return _multiSeriesTooltipItem(barSpot);
                            }
                            // Single-series tooltip (original).
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
                      topTitles: const AxisTitles(
                          sideTitles: SideTitles(showTitles: false)),
                      leftTitles: const AxisTitles(
                        sideTitles:
                            SideTitles(showTitles: false, reservedSize: 0),
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
                            if (idx < 0 || idx >= labelSeries.length) {
                              return const SizedBox.shrink();
                            }
                            if (idx % labelStep != 0) {
                              return const SizedBox.shrink();
                            }
                            return SideTitleWidget(
                              axisSide: meta.axisSide,
                              space: 8,
                              child: Text(
                                labelSeries[idx].label,
                                style: const TextStyle(fontSize: 10),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            );
                          },
                        ),
                      ),
                    ),
                    lineBarsData: lineBarsData,
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

  /// Build a tooltip item for a touched spot in multi-series mode.
  LineTooltipItem? _multiSeriesTooltipItem(LineBarSpot barSpot) {
    final multiSeries = widget.multiSeries!;
    final seriesIndex = barSpot.barIndex;
    if (seriesIndex < 0 || seriesIndex >= multiSeries.length) return null;
    final ns = multiSeries[seriesIndex];
    final xIdx = barSpot.x.toInt();
    final String label =
        (xIdx >= 0 && xIdx < ns.points.length) ? ns.points[xIdx].label : '';
    return LineTooltipItem(
      '${ns.name}\n',
      TextStyle(
        color: ns.color,
        fontWeight: FontWeight.bold,
      ),
      children: [
        TextSpan(
          text: '$label  ${CommonUtil.toCurrency(barSpot.y)}',
          style: const TextStyle(
            color: Colors.black,
            fontWeight: FontWeight.w500,
          ),
        ),
      ],
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
