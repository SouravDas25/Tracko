import 'package:another_flushbar/flushbar.dart';
import 'package:flutter/material.dart';

class FlushDialog {
  static flash(BuildContext context, String title, String message,
      [int? duration]) async {
    duration ??= 3;
    var fb = Flushbar(
      title: title,
      message: message,
      duration: Duration(seconds: duration),
    );
    await fb.show(context);
  }
}
