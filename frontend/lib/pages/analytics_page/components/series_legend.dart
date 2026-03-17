import 'package:flutter/material.dart';
import 'package:tracko/pages/analytics_page/models/analytics_models.dart';

class SeriesLegend extends StatelessWidget {
  final List<NamedSeries> series;
  final GroupByMode groupBy;

  const SeriesLegend({
    Key? key,
    required this.series,
    required this.groupBy,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    if (groupBy == GroupByMode.none || series.isEmpty) {
      return const SizedBox.shrink();
    }

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: LayoutBuilder(
        builder: (context, constraints) {
          return Wrap(
            spacing: 16,
            runSpacing: 8,
            children: series.map((s) {
              return ConstrainedBox(
                constraints: BoxConstraints(
                  maxWidth: constraints.maxWidth,
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      width: 12,
                      height: 12,
                      decoration: BoxDecoration(
                        color: s.color,
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 6),
                    Flexible(
                      child: Text(
                        s.name,
                        style: TextStyle(
                          fontSize: 13,
                          color: Theme.of(context).textTheme.bodyMedium?.color,
                        ),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ],
                ),
              );
            }).toList(),
          );
        },
      ),
    );
  }
}
