import 'package:charts_flutter/flutter.dart' as charts;
import 'package:flutter/material.dart';

class CategoryChart extends StatelessWidget {
  final List<charts.Series> seriesList = _createSampleData();
  final bool animate = true;

  @override
  Widget build(BuildContext context) {
    return Container(
        width: double.infinity,
        height: 250,
        child: new charts.PieChart(seriesList, animate: animate));
  }

  static List<charts.Series<Point, int>> _createSampleData() {
    final data = [
      new Point(0, 100),
      new Point(1, 75),
      new Point(2, 25),
      new Point(3, 5),
    ];

    return [
      new charts.Series<Point, int>(
        id: 'Category Expenses',
        domainFn: (Point point, _) => point.x,
        measureFn: (Point point, _) => point.y,
        data: data,
      )
    ];
  }
}

class Point {
  final int x;
  final int y;

  Point(this.x, this.y);
}
