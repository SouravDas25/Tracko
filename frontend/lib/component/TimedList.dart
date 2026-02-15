import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/component/PaddedText.dart';
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

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      primary: false,
      shrinkWrap: true,
      itemCount: itemCount,
      itemBuilder: (context, index) {
        final DateTime current = timeField(index);
        final String currentHuman = CommonUtil.humanDate(current);
        String? prevHuman;
        if (index > 0) {
          prevHuman = CommonUtil.humanDate(timeField(index - 1));
        }

        final List<Widget> children = [];
        if (index == 0 || prevHuman != currentHuman) {
          children.add(
            PaddedText(
              currentHuman.toUpperCase(),
              horizontal: 10.0,
              vertical: 10.0,
            ),
          );
        }
        children.add(itemBuilder(context, index));

        if (children.length == 1) {
          return children.first;
        }
        return Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          mainAxisSize: MainAxisSize.min,
          children: children,
        );
      },
    );
  }
}
