

import 'package:expense_manager/Utils/CommonUtil.dart';
import 'package:expense_manager/models/transaction.dart';
import 'package:flutter/material.dart';

class WidgetUtil {
  static Text transformAmount2TextWidget(Transaction transaction){
    return Text(
      CommonUtil.toSign(transaction.transactionType) +
          CommonUtil.toCurrency(transaction.amount),
      style: TextStyle(
          fontWeight: FontWeight.w600,
          fontSize: 20,
          color: CommonUtil.toTypeColor(transaction.transactionType)),
    );
  }
}