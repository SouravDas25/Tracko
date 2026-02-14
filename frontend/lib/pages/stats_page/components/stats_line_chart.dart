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

  // Internal controller for the scrollable chart
  final ScrollController _chartScrollController = ScrollController();

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
      final initial = baseSpots.length == 1
          ? <FlSpot>[baseSpots[0], FlSpot(baseSpots[0].x + 1, baseSpots[0].y)]
          : baseSpots;
      if (initial.isEmpty) return initial;
      // Add a trailing zero point so the chart visually ends at baseline.
      return <FlSpot>[...initial, FlSpot(initial.last.x + 1, 0)];
    })();

    final maxX = spots.length == 1 ? 1.0 : spots.last.x;
    final maxY = seriesMaxY <= 0 ? 1.0 : seriesMaxY;
    final leftInterval = maxY <= 0 ? 1.0 : (maxY / 4);

    // Calculate dynamic width: minimum 60px per datapoint for readable spacing
    final chartWidth = (series.length * 60.0).clamp(300.0, double.infinity);

    return HorizontalScrollContainer(
      controller: _chartScrollController,
      width: chartWidth,
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
                  if (idx < 0 || idx >= series.length)
                    return const SizedBox.shrink();
                  return SideTitleWidget(
                    axisSide: meta.axisSide,
                    space: 6,
                    child: Transform.rotate(
                      angle: -0.6,
                      child: Text(
                        series[idx].label,
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
