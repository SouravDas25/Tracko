import 'package:Tracko/component/menu_bar.dart' as TrackoMenuBar;
import 'package:flutter/material.dart';

class Screen extends StatefulWidget {
  final Widget? body;
  Widget? title;
  String? titleName;

  Screen({this.body, this.title, this.titleName});

  @override
  ScreenState createState() {
    return new ScreenState();
  }
}

class ScreenState extends State<Screen> {
  @override
  Widget build(BuildContext context) {
    PreferredSizeWidget? appBar = this.widget.title == null
        ? TrackoMenuBar.MenuBar(titleName: widget.titleName) as PreferredSizeWidget?
        : this.widget.title as PreferredSizeWidget?;
    return Scaffold(
      appBar: appBar,
      body: Padding(
        padding: EdgeInsets.symmetric(vertical: 20, horizontal: 20),
        child: this.widget.body,
      ),
    );
  }
}
