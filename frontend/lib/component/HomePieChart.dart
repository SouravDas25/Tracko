import 'package:tracko/scratch/ChartUtil.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

import 'Indicator.dart';

//class CategoryChart extends StatefulWidget {
//  List<charts.Series> seriesList = [];
//
//  CategoryChart(this.seriesList);
//
//  @override
//  State<StatefulWidget> createState() {
//    return _CategoryChart();
//  }
//}

class CategoryChart extends StatelessWidget {
  final List<ChartEntry> seriesList;

  CategoryChart(this.seriesList) {
    ChartUtil.prepareForChart(seriesList);
  }

//  @override
//  void initState() {
//    super.initState();
////    initData();
//
//  }

//  @override
//  void didUpdateWidget(CategoryChart oldWidget) {
//    super.didUpdateWidget(oldWidget);
//    initData();
////    print("didUpdateWidget");
//  }

//  initData() async {
////    print("called");
//    seriesList = await _createData();
////    print("apple " + );
////    await Future.delayed(Duration(seconds: 1));
//    if(this.mounted){
//      setState(() {});
//    }
//
//  }

  @override
  Widget build(BuildContext context) {
    // seriesList is non-nullable; keep a fixed height so this widget never expands infinitely
    // inside scrollables (ListView/SmartRefresher).
    final chartHeight =
        (MediaQuery.of(context).size.height * 0.45).clamp(240.0, 520.0);
//    print(seriesList.length);
    if (seriesList.length <= 0) {
      return SizedBox(
        width: double.infinity,
        height: chartHeight,
        child: Center(
          child: Text("No Data Available"),
        ),
      );
    }

    return SizedBox(
      width: double.infinity,
      height: chartHeight,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: <Widget>[
          const SizedBox(height: 12),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: createIndicators(seriesList),
            ),
          ),
          const SizedBox(height: 12),
          Expanded(
            child: AspectRatio(
              aspectRatio: 1.8,
              child: PieChart(
                PieChartData(
                  startDegreeOffset: 180,
                  borderData: FlBorderData(
                    show: false,
                  ),
                  sectionsSpace: 1,
                  sections: showingSections(seriesList),
                  centerSpaceRadius: 40,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  List<Widget> createIndicators(List<ChartEntry> data) {
    List<Widget> indicators = [];
    for (ChartEntry ce in data) {
      indicators.add(Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8.0),
        child: Indicator(
          color: ce.color,
          text: ce.label,
          isSquare: true,
          size: 16,
          textColor: Colors.grey,
        ),
      ));
    }
    return indicators;
  }

  List<PieChartSectionData> showingSections(List<ChartEntry> data) {
    List<PieChartSectionData> pieChartData = [];

    for (ChartEntry ce in data) {
      pieChartData.add(PieChartSectionData(
        color: ce.color,
        value: ce.percentage,
        title: "${ce.percentage.toInt()}%",
        radius: 70,
        titlePositionPercentageOffset: 0.55,
      ));
    }
    return pieChartData;
  }
}
