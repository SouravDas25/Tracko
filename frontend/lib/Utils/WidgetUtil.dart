import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/models/transaction.dart';
import 'package:flutter/material.dart';
import 'package:flutter_spinkit/flutter_spinkit.dart';
import 'package:fluttertoast/fluttertoast.dart';

class WidgetUtil {
  static Text transformTransaction2TextWidget(Transaction transaction) {
    final oc = transaction.originalCurrency;
    final oa = transaction.originalAmount;
    if (oc != null && oc.isNotEmpty && oa != null) {
      final sign = CommonUtil.toSign(transaction.transactionType);
      final originalText = CommonUtil.toCurrency(oa, currencyCode: oc);
      final baseText = CommonUtil.toCurrency(transaction.amount);
      return Text(
        '$sign$originalText / $baseText',
        style: TextStyle(
            fontWeight: FontWeight.w600,
            fontSize: 20,
            color: transaction.amount == 0
                ? null
                : CommonUtil.toTypeColor(transaction.transactionType)),
      );
    }
    return transformAmount2TextWidget(
      transaction.transactionType,
      transaction.amount,
    );
  }

  static Text transformAmount2TextWidget(int transactionType, double amount,
      {bool addSign = true, String? currencyCode}) {
    String text = "";
    if (addSign) {
      text += CommonUtil.toSign(transactionType);
    }
    return Text(
      text + CommonUtil.toCurrency(amount, currencyCode: currencyCode),
      style: TextStyle(
          fontWeight: FontWeight.w600,
          fontSize: 20,
          color: amount == 0 ? null : CommonUtil.toTypeColor(transactionType)),
    );
  }

  static CircleAvatar getCircleAvatar(
      var data, String name, IMG_TYPE imageType) {
    if (data != null && data.length > 0) {
      ImageProvider image;
      switch (imageType) {
        case IMG_TYPE.MEMORY:
          image = MemoryImage(data);
          break;
        case IMG_TYPE.NETWORK:
          image = NetworkImage(data);
          break;
        case IMG_TYPE.LOCAL:
          image = AssetImage(data);
          break;
      }
      return CircleAvatar(backgroundImage: image);
    }
    return CircleAvatar(
      child: Text(CommonUtil.getInitials(name),
          style: TextStyle(color: Colors.white)),
    );
  }

  static Widget spinLoader() {
    return SpinKitWave(
      itemBuilder: (_, int index) {
        return DecoratedBox(
          decoration: BoxDecoration(
            color: index.isEven ? Colors.blueAccent : Colors.teal,
          ),
        );
      },
      type: SpinKitWaveType.center,
      size: 40.0,
    );
  }

  static TextStyle defaultTextStyle() {
    return TextStyle(fontSize: 20.0);
  }

  static CircleAvatar textAvatar(String name, {Color? backgroundColor}) {
    String initials = CommonUtil.getInitials(name);
    return CircleAvatar(
        backgroundColor: backgroundColor,
        child: Text(initials, style: TextStyle(color: Colors.white)));
  }

  static toast(String message) {
    Fluttertoast.showToast(
        msg: message,
        toastLength: Toast.LENGTH_SHORT,
        gravity: ToastGravity.BOTTOM,
        timeInSecForIosWeb: 1,
        backgroundColor: Colors.grey.shade700,
        textColor: Colors.white,
        fontSize: 16.0);
  }

  static State? globalHomeTabState;

  static setHomeTab(State homeTab) {
    globalHomeTabState = homeTab;
  }
}

enum IMG_TYPE { MEMORY, NETWORK, LOCAL }
