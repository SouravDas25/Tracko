import 'package:Tracko/Utils/CommonUtil.dart';
import 'package:Tracko/component/PaddedText.dart';
import 'package:flutter/material.dart';

typedef TimeFieldExtractor = DateTime Function(int index);

class TimedList extends StatelessWidget {
  final IndexedWidgetBuilder itemBuilder;
  final TimeFieldExtractor timeField;
  final int itemCount;

  TimedList(
      {required this.itemBuilder,
      required this.itemCount,
      required this.timeField});

  List<Widget> buildChildrens(BuildContext context) {
    List<Widget> childs = [];
    String humanDate = "";
    for (int i = 0; i < itemCount; i++) {
      Widget child = this.itemBuilder(context, i);
      DateTime dt = this.timeField(i);
      String nwDate = CommonUtil.humanDate(dt);
      if (humanDate != nwDate) {
        childs.add(
          PaddedText(
            nwDate.toUpperCase(),
            horizontal: 10.0,
            vertical: 10.0,
          ),
        );
        humanDate = nwDate;
      }
      childs.add(child);
    }
    return childs;
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      primary: false,
      shrinkWrap: true,
      children: buildChildrens(context),
    );
  }
}
