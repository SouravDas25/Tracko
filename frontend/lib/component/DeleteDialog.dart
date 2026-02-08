import 'package:tracko/Utils/WidgetUtil.dart';
import 'package:flutter/material.dart';

class DeleteDialog {
  static Future<T> show<T>(
      {required context,
      required String title,
      required String message,
      required Function deleteCallback}) async {
    return await showDialog(
      context: context,
      builder: (BuildContext context) {
        // return object of type Dialog
        return AlertDialog(
          title: new Text(title),
          content: new Text(message),
          actions: <Widget>[
            new TextButton(
              child: new Text("No"),
              onPressed: () {
                Navigator.pop(context);
              },
            ),
            new TextButton(
              child: new Text("Yes"),
              onPressed: () {
                try {
                  deleteCallback();
                } catch (e) {
                  WidgetUtil.toast("Some error occured.");
                } finally {
                  Navigator.pop(context);
                }
              },
            ),
          ],
        );
      },
    );
  }
}
