import 'dart:math';

import 'package:flutter/material.dart';

class ChartEntry {
  final int key;
  final int value;
  final String label;
  late double percentage;
  late Color color;

  ChartEntry(this.key, this.label, this.value);

  @override
  String toString() {
    return "{${this.key} : ${this.label} -> ${this.value}}";
  }
}

class ChartUtil {
  static const List<Color> _palette = [
    Colors.redAccent,
    Colors.orange,
    Colors.amber,
    Colors.green,
    Colors.teal,
    Colors.blue,
    Colors.indigo,
    Colors.purple,
    Colors.pink,
    Colors.brown,
  ];

  static prepareForChart(List<ChartEntry> data) {
    double sum = 0.0;

    for (int i = 0; i < data.length; i++) {
      ChartEntry ce = data[i];
      sum += ce.value;
      ce.color = getColor(i);
    }

    for (ChartEntry ce in data) {
      ce.percentage = sum == 0 ? 0.0 : (ce.value / sum) * 100;
    }
  }

  static Color getColor(int index) {
    return _palette[index % _palette.length];
  }
}
