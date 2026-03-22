import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/component/Indicator.dart';
import 'package:tracko/Utils/ChartUtil.dart';

class StatsPieChart extends StatefulWidget {
  final bool loading;
  final String? error;
  final List<ChartEntry> pieSeries;
  final double total;

  const StatsPieChart({
    Key? key,
    required this.loading,
    required this.error,
    required this.pieSeries,
    this.total = 0,
  }) : super(key: key);

  @override
  State<StatsPieChart> createState() => _StatsPieChartState();
}

class _StatsPieChartState extends State<StatsPieChart> {
  int touchedIndex = -1;

  @override
  Widget build(BuildContext context) {
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
      return SizedBox(
        height: 200,
        child: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.pie_chart_outline, size: 48,
                  color: Theme.of(context).hintColor.withOpacity(0.3)),
              const SizedBox(height: 8),
              Text('No data for this period',
                  style: TextStyle(
                    color: Theme.of(context).hintColor.withOpacity(0.6),
                    fontSize: 14,
                  )),
            ],
          ),
        ),
      );
    }

    final seriesList = List<ChartEntry>.from(widget.pieSeries);
    ChartUtil.prepareForChart(seriesList);
    final totalValue = seriesList.fold(0.0, (sum, item) => sum + item.value);

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        const SizedBox(height: 8),
        // Legend on top as horizontal scroll
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.symmetric(horizontal: 8),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: seriesList.asMap().entries.map((e) {
              final idx = e.key;
              final ce = e.value;
              final isTouched = idx == touchedIndex;
              return Padding(
                padding: const EdgeInsets.symmetric(horizontal: 8.0),
                child: Opacity(
                  opacity: touchedIndex == -1 || isTouched ? 1.0 : 0.4,
                  child: GestureDetector(
                    onTap: () {
                      setState(() {
                        touchedIndex = touchedIndex == idx ? -1 : idx;
                      });
                    },
                    child: Indicator(
                      color: ce.color,
                      text: ce.label,
                      isSquare: true,
                      size: isTouched ? 16 : 14,
                      textColor: isTouched
                          ? Theme.of(context).textTheme.bodyLarge!.color!
                          : Colors.grey,
                    ),
                  ),
                ),
              );
            }).toList(),
          ),
        ),
        const SizedBox(height: 16),
        // Pie chart with center text
        SizedBox(
          height: 220,
          child: Stack(
            alignment: Alignment.center,
            children: [
              PieChart(
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
                  centerSpaceRadius: 56,
                  sections: _showingSections(seriesList),
                ),
              ),
              Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    touchedIndex != -1 && touchedIndex < seriesList.length
                        ? seriesList[touchedIndex].label
                        : 'Total',
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: Theme.of(context).hintColor,
                    ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 2),
                  Text(
                    touchedIndex != -1 && touchedIndex < seriesList.length
                        ? CommonUtil.toCurrency(
                            seriesList[touchedIndex].value.toDouble())
                        : CommonUtil.toCurrency(totalValue),
                    style: TextStyle(
                      fontSize: 16,
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
        const SizedBox(height: 8),
      ],
    );
  }

  List<PieChartSectionData> _showingSections(List<ChartEntry> data) {
    return List.generate(data.length, (i) {
      final isTouched = i == touchedIndex;
      final fontSize = isTouched ? 16.0 : 12.0;
      final radius = isTouched ? 55.0 : 45.0;
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
        badgeWidget: null,
      );
    });
  }
}
