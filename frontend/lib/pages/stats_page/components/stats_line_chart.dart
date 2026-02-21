import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/component/horizontal_scroll_container.dart';
import 'package:tracko/pages/stats_page/controllers/stats_controller.dart';

class StatsLineChart extends StatelessWidget {
  final bool loading;
  final String? error;
  final List<SeriesPoint> series;
  final double seriesMaxY;
  final Color kindColor;

  StatsLineChart({
    Key? key,
    required this.loading,
    required this.error,
    required this.series,
    required this.seriesMaxY,
    required this.kindColor,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
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

    return SizedBox(
      height: 220,
      child: LineChart(
        LineChartData(
          lineTouchData: LineTouchData(
            enabled: true,
            touchTooltipData: LineTouchTooltipData(
              // tooltipBgColor: Theme.of(context).cardColor,
              getTooltipItems: (List<LineBarSpot> touchedBarSpots) {
                return touchedBarSpots.map((barSpot) {
                  final flSpot = barSpot;
                  if (flSpot.x < 0 || flSpot.x >= series.length) {
                    return null;
                  }

                  // Don't show tooltip for the extra trailing point
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
                  // fl_chart may call this for non-integer tick values; only label exact indices.
                  if ((value - value.roundToDouble()).abs() > 0.001) {
                    return const SizedBox.shrink();
                  }
                  final idx = value.round();
                  if (idx < 0 || idx >= series.length) {
                    return const SizedBox.shrink();
                  }

                  // Skip labels if there are too many points to avoid overlapping
                  if (series.length > 7) {
                    // Calculate a step size to show max ~6-7 labels
                    int step = (series.length / 6).ceil();
                    // Always show the first and last label, and evenly spaced labels in between
                    if (idx != 0 &&
                        idx != series.length - 1 &&
                        idx % step != 0) {
                      return const SizedBox.shrink();
                    }
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
    );
  }
}
