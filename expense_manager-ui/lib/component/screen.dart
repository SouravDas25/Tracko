import 'package:flutter/material.dart';
import 'package:expense_manager/component/menu_bar.dart';

class Screen extends StatefulWidget {
  final Widget body;
  final Widget title;

  Screen({this.body, this.title});

  @override
  ScreenState createState() {
    return new ScreenState();
  }
}

class ScreenState extends State<Screen> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: MenuBar(),
      body: Padding(
        padding: EdgeInsets.symmetric(vertical: 20,horizontal: 30),
        child: this.widget.body,
      ),
    );
  }
}
