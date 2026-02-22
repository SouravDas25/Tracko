import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/component/Indicator.dart';
import 'package:tracko/scratch/ChartUtil.dart';

class StatsPieChart extends StatefulWidget {
  final bool loading;
  final String? error;
  final List<ChartEntry> pieSeries;

  const StatsPieChart({
    Key? key,
    required this.loading,
    required this.error,
    required this.pieSeries,
  }) : super(key: key);

  @override
  State<StatsPieChart> createState() => _StatsPieChartState();
}

class _StatsPieChartState extends State<StatsPieChart> {
  int touchedIndex = -1;

  @override
  Widget build(BuildContext context) {
    final chartHeight =
        (MediaQuery.of(context).size.height * 0.4).clamp(300.0, 520.0);

    if (widget.loading) {
      return const SizedBox(
        height: 260,
        child: Center(child: CircularProgressIndicator()),
      );
    }
    if (widget.error != null) {
      return const SizedBox.shrink();
    }
    if (widget.pieSeries.isEmpty) {
      return const SizedBox(
        height: 260,
        child: Center(child: Text('No data')),
      );
    }

    // Clone list to avoid modifying the controller's list during preparation
    final seriesList = List<ChartEntry>.from(widget.pieSeries);
    ChartUtil.prepareForChart(seriesList);

    final totalValue = seriesList.fold(0.0, (sum, item) => sum + item.value);

    return SizedBox(
      width: double.infinity,
      height: chartHeight,
      child: Column(
        children: [
          const SizedBox(height: 12),
          // Legend
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: seriesList.asMap().entries.map((e) {
                final idx = e.key;
                final ce = e.value;
                final isTouched = idx == touchedIndex;
                return Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 8.0),
                  child: Opacity(
                    opacity: touchedIndex == -1 || isTouched ? 1.0 : 0.3,
                    child: Indicator(
                      color: ce.color,
                      text: ce.label,
                      isSquare: true,
                      size: isTouched ? 18 : 16,
                      textColor: isTouched
                          ? Theme.of(context).textTheme.bodyLarge!.color!
                          : Colors.grey,
                    ),
                  ),
                );
              }).toList(),
            ),
          ),
          const SizedBox(height: 24),
          // Chart Area with Center Text
          Expanded(
            child: Stack(
              alignment: Alignment.center,
              children: [
                AspectRatio(
                  aspectRatio: 1,
                  child: PieChart(
                    PieChartData(
                      pieTouchData: PieTouchData(
                        touchCallback: (FlTouchEvent event, pieTouchResponse) {
                          setState(() {
                            if (!event.isInterestedForInteractions ||
                                pieTouchResponse == null ||
                                pieTouchResponse.touchedSection == null) {
                              touchedIndex = -1;
                              return;
                            }
                            touchedIndex = pieTouchResponse
                                .touchedSection!.touchedSectionIndex;
                          });
                        },
                      ),
                      startDegreeOffset: 270,
                      borderData: FlBorderData(show: false),
                      sectionsSpace: 2,
                      centerSpaceRadius: 60, // Donut hole radius
                      sections: _showingSections(seriesList),
                    ),
                  ),
                ),
                // Center Text
                Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      touchedIndex != -1 && touchedIndex < seriesList.length
                          ? seriesList[touchedIndex].label
                          : 'Total',
                      style: TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                        color: Theme.of(context).hintColor,
                      ),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 4),
                    Text(
                      touchedIndex != -1 && touchedIndex < seriesList.length
                          ? CommonUtil.toCurrency(
                              seriesList[touchedIndex].value.toDouble())
                          : CommonUtil.toCurrency(totalValue),
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w900,
                        color: touchedIndex != -1 &&
                                touchedIndex < seriesList.length
                            ? seriesList[touchedIndex].color
                            : Theme.of(context).textTheme.bodyLarge?.color,
                      ),
                      textAlign: TextAlign.center,
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  List<PieChartSectionData> _showingSections(List<ChartEntry> data) {
    return List.generate(data.length, (i) {
      final isTouched = i == touchedIndex;
      final fontSize = isTouched ? 18.0 : 14.0;
      final radius = isTouched ? 60.0 : 50.0;
      final entry = data[i];

      return PieChartSectionData(
        color: entry.color,
        value: entry.percentage,
        title: '${entry.percentage.toInt()}%',
        radius: radius,
        titleStyle: TextStyle(
          fontSize: fontSize,
          fontWeight: FontWeight.bold,
          color: const Color(0xffffffff),
          shadows: const [Shadow(color: Colors.black26, blurRadius: 2)],
        ),
        badgeWidget: isTouched
            ? Container(
                padding: const EdgeInsets.all(4),
                decoration: BoxDecoration(
                  color: Colors.white,
                  shape: BoxShape.circle,
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.2),
                      blurRadius: 4,
                    ),
                  ],
                ),
                child: Icon(
                  Icons.star,
                  size: 16,
                  color: entry.color,
                ),
              )
            : null,
        badgePositionPercentageOffset: .98,
      );
    });
  }
}
