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
  static prepareForChart(List<ChartEntry> data) {
    double sum = 0.0;
    Random random = new Random();
    int baseColor = 100;
    int len = data.length == 0 ? 1 : data.length;
    int diff = min(175 ~/ len, 40);

    for (ChartEntry ce in data) {
      sum += ce.value;
      ce.color = Color.fromRGBO(0, baseColor, baseColor, 1);
      baseColor = max((baseColor + diff) % 220, 100);
    }

    for (ChartEntry ce in data) {
      ce.percentage = (ce.value / sum) * 100;
    }
  }
}
