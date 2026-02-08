import 'package:flutter/material.dart';

class LoadingDialog {
  static hide(context) {
    Navigator.pop(context);
  }

  static show(context) async {
    await showDialog(
        context: context,
        barrierDismissible: false,
        builder: (BuildContext context) {
          return Center(
            child: CircularProgressIndicator(),
          );
        });
  }

//  static processWhileShowingDialog(context, Function function) async {
//    LoadingDialog.showLoading(context);
////    print("showLoading done.");
//    try {
////      print("function loading.");
//      await function();
////      print("function executed.");
//    } catch (exception) {
//      print(exception);
//    }
////    print("hideLoading executed.");
//    LoadingDialog.hideLoading(context);
////    LoadingDialog.hideLoading(context);
////    print("hideLoading executed.");
//  }
}
