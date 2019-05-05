import 'package:flutter/material.dart';

class MenuBar extends AppBar {
  var titleName;

  MenuBar({this.titleName})
      : super(
            title: titleName != null ? Text(titleName) : Text("Tracko"),
            centerTitle: true);
}
