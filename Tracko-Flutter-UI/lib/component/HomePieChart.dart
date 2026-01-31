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
    if (seriesList == null) {
      return Container(
          width: double.infinity,
          height: 250,
          child: Center(
            widthFactor: 2.0,
            child: CircularProgressIndicator(),
          ));
    }
//    print(seriesList.length);
    if (seriesList.length <= 0) {
      return Container(
          width: double.infinity,
          height: 250,
          child: Center(
            child: Text("No Data Available"),
          ));
    }

    return Container(
//        padding: EdgeInsets.all(8.0),
        width: double.infinity,
        child: Column(
          children: <Widget>[
            const SizedBox(
              height: 28,
            ),
            SingleChildScrollView(
              child: Row(
                mainAxisSize: MainAxisSize.max,
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: createIndicators(seriesList),
              ),
              scrollDirection: Axis.horizontal,
            ),
            PieChart(PieChartData(
              startDegreeOffset: 180,
              borderData: FlBorderData(
                show: false,
              ),
              sectionsSpace: 1,
              sections: showingSections(seriesList),
              centerSpaceRadius: 40,
            )),
          ],
        ));
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
