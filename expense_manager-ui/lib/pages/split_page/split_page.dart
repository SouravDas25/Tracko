import 'package:expense_manager/Utils/CommonUtil.dart';
import 'package:expense_manager/Utils/WidgetUtil.dart';
import 'package:expense_manager/Utils/enums.dart';
import 'package:flutter/material.dart';

class SplitPage extends StatefulWidget {
  var data = [
    {
      "name": "Debjit Chakro",
      "amount": 56.0,
      "type": TransactionType.DEBIT,
      "date": DateTime.now()
    },
    {
      "name": "Aman Whanayak",
      "amount": 1200.0,
      "type": TransactionType.CREDIT,
      "date": DateTime.now()
    }
  ];

  @override
  State<StatefulWidget> createState() {
    return _SplitPage();
  }
}

class _SplitPage extends State<SplitPage> {
  _SplitPage() {
    initData();
  }

  void initData() async {}

  @override
  Widget build(BuildContext context) {
    return new Scaffold(
      body: ListView(
        children: widget.data.map<Widget>((dynamic item) {
          return Card(
            child: ListTile(
              title: Text(
                item["name"],
                style: TextStyle(fontSize: 22.0),
              ),
              subtitle: Text(CommonUtil.humanDate(item["date"])),
              trailing: WidgetUtil.transformAmount2TextWidget(
                  item["type"], item['amount']),
              leading: CircleAvatar(
                radius: 30.0,
                child: Image.network(
                  "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRn7T9W-rOPBsjYxw5U018Vdtd1H3UrPtF5tMK6Ssr7LDSSWCGfsA"
                ),
              ),
              contentPadding: EdgeInsets.all(8.0),
            ),
          );
        }).toList(),
      ),
    );
  }
}
