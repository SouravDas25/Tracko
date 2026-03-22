import 'dart:ui';

import 'package:tracko/Utils/ChartUtil.dart';
import 'package:tracko/pages/stats_page/controllers/stats_controller.dart';

enum GroupByMode { none, category, account, description }

enum AnalyticsGranularity { weekly, monthly, yearly }

enum DateRangePreset { thisMonth, last3Months, last6Months, thisYear, last5Years, custom }

class NamedSeries {
  final String name;
  final List<SeriesPoint> points;
  final Color color;

  const NamedSeries({
    required this.name,
    required this.points,
    required this.color,
  });
}

/// DTO returned by [AnalyticsRepository.getChartData].
class AnalyticsChartResponse {
  final double total;
  final DateTime? periodStart;
  final DateTime? periodEnd;
  final List<NamedSeries> groupedSeries;
  final double seriesMaxY;

  const AnalyticsChartResponse({
    required this.total,
    required this.periodStart,
    required this.periodEnd,
    required this.groupedSeries,
    required this.seriesMaxY,
  });

  factory AnalyticsChartResponse.fromJson(Map<String, dynamic> json) {
    final total = ((json['total'] as num?) ?? 0).toDouble();

    final periodStartStr = (json['periodStart'] as String?) ?? '';
    final periodEndStr = (json['periodEnd'] as String?) ?? '';

    DateTime? pStart;
    DateTime? pEnd;
    try {
      if (periodStartStr.isNotEmpty) pStart = DateTime.parse(periodStartStr);
      if (periodEndStr.isNotEmpty) pEnd = DateTime.parse(periodEndStr);
    } catch (_) {}

    final groupedJson = (json['groupedSeries'] as List?) ?? const [];
    final parsedSeries = <NamedSeries>[];

    for (int gi = 0; gi < groupedJson.length; gi++) {
      final entry = groupedJson[gi];
      if (entry is! Map) continue;
      final name = (entry['name'] as String?) ?? 'Series $gi';
      final seriesJson = (entry['series'] as List?) ?? const [];
      final points = <SeriesPoint>[];
      for (int i = 0; i < seriesJson.length; i++) {
        final item = seriesJson[i];
        if (item is! Map) continue;
        final label = (item['label'] as String?) ?? '';
        final value = ((item['value'] as num?) ?? 0).toDouble();
        points.add(SeriesPoint(x: i.toDouble(), y: value, label: label));
      }
      parsedSeries.add(NamedSeries(
        name: name,
        points: points,
        color: ChartUtil.getColor(gi),
      ));
    }

    double maxY = 0;
    for (final ns in parsedSeries) {
      for (final p in ns.points) {
        if (p.y > maxY) maxY = p.y;
      }
    }

    return AnalyticsChartResponse(
      total: total,
      periodStart: pStart,
      periodEnd: pEnd,
      groupedSeries: parsedSeries,
      seriesMaxY: maxY,
    );
  }
}
