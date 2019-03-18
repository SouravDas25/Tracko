import 'package:flutter/material.dart';
import 'package:expense_manager/component/menu_bar.dart';

class Screen extends StatefulWidget {
  final Widget body;
  Widget title;

  Screen({this.body, this.title});

  @override
  ScreenState createState() {
    return new ScreenState();
  }
}

class ScreenState extends State<Screen> {
  @override
  Widget build(BuildContext context) {
    this.widget.title = this.widget.title == null ? MenuBar() : this.widget.title;
    return Scaffold(
      appBar: this.widget.title,
      body: Padding(
        padding: EdgeInsets.symmetric(vertical: 20,horizontal: 20),
        child: this.widget.body,
      ),
    );
  }
}
