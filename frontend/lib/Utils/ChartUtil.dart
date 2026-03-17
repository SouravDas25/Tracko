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
    Color(0xFFE53935), // red
    Color(0xFF1E88E5), // blue
    Color(0xFF43A047), // green
    Color(0xFFFB8C00), // orange
    Color(0xFF8E24AA), // purple
    Color(0xFF00ACC1), // cyan
    Color(0xFFFFB300), // amber
    Color(0xFFD81B60), // pink
    Color(0xFF3949AB), // indigo
    Color(0xFF00897B), // teal
    Color(0xFF7CB342), // light green
    Color(0xFFF4511E), // deep orange
    Color(0xFF6D4C41), // brown
    Color(0xFF546E7A), // blue grey
    Color(0xFFC0CA33), // lime
    Color(0xFF5C6BC0), // indigo light
    Color(0xFFEC407A), // pink light
    Color(0xFF26A69A), // teal light
    Color(0xFFAB47BC), // purple light
    Color(0xFF42A5F5), // blue light
    Color(0xFFEF6C00), // orange dark
    Color(0xFF2E7D32), // green dark
    Color(0xFFAD1457), // pink dark
    Color(0xFF0277BD), // light blue dark
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
