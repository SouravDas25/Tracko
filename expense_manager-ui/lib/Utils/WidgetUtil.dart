

import 'package:expense_manager/Utils/CommonUtil.dart';
import 'package:expense_manager/Utils/enums.dart';
import 'package:expense_manager/models/transaction.dart';
import 'package:flutter/material.dart';

class WidgetUtil {
  static Text transformTransaction2TextWidget(Transaction transaction){
    return transformAmount2TextWidget(transaction.transactionType,transaction.amount);
  }

  static Text transformAmount2TextWidget(int transactionType, double amount){
    return Text(
      CommonUtil.toSign(transactionType) +
          CommonUtil.toCurrency(amount),
      style: TextStyle(
          fontWeight: FontWeight.w600,
          fontSize: 20,
          color: CommonUtil.toTypeColor(transactionType)),
    );
  }
}