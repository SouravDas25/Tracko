import 'package:charts_flutter/flutter.dart' as charts;
import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/scratch/ChartUtil.dart';
import 'package:flutter/material.dart';

class CategoryChart extends StatefulWidget {
  @override
  State<StatefulWidget> createState() {
    return _CategoryChart();
  }
}

class _CategoryChart extends State<CategoryChart> {
  List<charts.Series> seriesList;

  @override
  void initState() {
    super.initState();
    initData();
  }

  initData() async {
    seriesList = await _createData();
  }

  Future<List<charts.Series<Point, int>>> _createData() async {
    var adapter = await DatabaseUtil.getAdapter();
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

  @override
  Widget build(BuildContext context) {
    if(seriesList == null){
      return Container(
          width: double.infinity,
          height: 250,
          child: Center(
            widthFactor: 2.0,
            child: CircularProgressIndicator(),
          )
      );
    }
    return Container(
        width: double.infinity,
        height: 250,
        child: new charts.PieChart(seriesList,
            defaultRenderer: new charts.ArcRendererConfig(
                arcWidth: 60,
                arcRendererDecorators: [new charts.ArcLabelDecorator()]),
            animate: true));
  }
}
