import 'package:flutter/material.dart';

class LabelWidget extends StatelessWidget {
  final String value;
  final String label;
  final Color color;

  LabelWidget(this.label, this.value, this.color);

  @override
  Widget build(BuildContext context) {
    return Row(
      children: <Widget>[
        Expanded(
          child: Padding(
            padding: const EdgeInsets.all(8.0),
            child: Text(
              this.label,
              style: TextStyle(fontSize: 15),
            ),
          ),
          flex: 5,
        ),
        Expanded(
          child: Padding(
            padding: const EdgeInsets.all(8.0),
            child: Text(this.value,
                style: TextStyle(
                    fontSize: 16,
                    color: this.color,
                    fontWeight: FontWeight.w700)),
          ),
          flex: 5,
        )
      ],
    );
  }
}
