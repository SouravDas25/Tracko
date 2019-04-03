import 'package:flushbar/flushbar.dart';
import 'package:flutter/material.dart';

class FlushDialog {
  static flash(BuildContext context, String title, String message,
      [int duration]) {
    if (duration == null) duration = 3;
    Flushbar(
      title: title,
      message: message,
      duration: Duration(seconds: duration),
    )..show(context);
  }
}
