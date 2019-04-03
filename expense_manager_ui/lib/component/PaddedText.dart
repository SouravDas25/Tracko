import 'package:flutter/material.dart';

class PaddedText extends StatelessWidget {
  double vertical = 0.0;
  double horizontal = 0.0;
  final TextStyle style;
  final TextAlign textAlign;
  final String data;

  PaddedText(this.data,
      {this.vertical, this.horizontal, this.style, this.textAlign});

  @override
  Widget build(BuildContext context) {
    horizontal = horizontal == null ? 0.0 : horizontal;
    vertical = vertical == null ? 0.0 : vertical;
    return Padding(
      padding: EdgeInsets.symmetric(
          horizontal: this.horizontal, vertical: this.vertical),
      child: Text(
        this.data,
        style: this.style,
        textAlign: this.textAlign,
      ),
    );
  }
}
