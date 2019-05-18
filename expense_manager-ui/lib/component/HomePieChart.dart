import 'package:charts_flutter/flutter.dart' as charts;
import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/Utils/enums.dart';
import 'package:expense_manager/models/category.dart';
import 'package:expense_manager/models/transaction.dart';
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
  List<Entry> data = [];

  @override
  void initState() {
    super.initState();
    this.initData();
  }

  initData() async {
    seriesList = await _createData();
//    print("apple " + seriesList.toString());
    setState(() {});
  }

  Future<List<charts.Series<Entry, int>>> _createData() async {
    var adapter = await DatabaseUtil.getAdapter();
    CategoryBean categoryBean = new CategoryBean(adapter);
    TransactionBean transactionBean = new TransactionBean(adapter);
    List<Category> categories = await categoryBean.getAll();
    for (Category category in categories) {
      var tmp = await transactionBean.findByCategory(category.id);
      tmp.retainWhere(
          (element) => element.transactionType == TransactionType.DEBIT);
      double amount = tmp.fold(0.0,
          (double previous, Transaction element) => previous + element.amount);
//      print("amount : "+amount.toString());
      if (amount > 0.0) {
        data.add(Entry(category.id, category.name, amount.toInt()));
      }
    }
    data = data.reversed.toList();
//    print("data : " + (data.length.toString()));
    return [
      new charts.Series<Entry, int>(
        id: 'Category Expenses',
        domainFn: (Entry point, _) => point.key,
        measureFn: (Entry point, _) => point.value,
        labelAccessorFn: (Entry point, _) => point.label,
        data: data,
      )
    ];
  }

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
    if (data.isEmpty) {
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
        height: 250,
        child: new charts.PieChart(seriesList,
            defaultRenderer: new charts.ArcRendererConfig(
                arcWidth: 60,
                arcRendererDecorators: [
                  new charts.ArcLabelDecorator(
                      labelPosition: charts.ArcLabelPosition.auto)
                ]),
            animate: true));
  }
}
