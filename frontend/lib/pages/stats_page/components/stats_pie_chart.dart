import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:tracko/component/Indicator.dart';
import 'package:tracko/scratch/ChartUtil.dart';

class StatsPieChart extends StatelessWidget {
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
  Widget build(BuildContext context) {
    final chartHeight =
        (MediaQuery.of(context).size.height * 0.4).clamp(240.0, 520.0);
    
    if (loading) {
      return const SizedBox(
        height: 260,
        child: Center(child: CircularProgressIndicator()),
      );
    }
    if (error != null) {
      return const SizedBox.shrink();
    }
    if (pieSeries.isEmpty) {
      return const SizedBox(
        height: 260,
        child: Center(child: Text('No data')),
      );
    }

    // Clone list to avoid modifying the controller's list during preparation
    final seriesList = List<ChartEntry>.from(pieSeries);
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
}
