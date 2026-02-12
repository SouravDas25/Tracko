import 'dart:async';

import 'package:tracko/Utils/enums.dart';
import 'package:tracko/dtos/TrackoContact.dart';
import 'package:tracko/models/account.dart';
import 'package:tracko/models/category.dart';
import 'package:tracko/models/split.dart';

class Transaction {
  int? id;
  int transactionType = TransactionType.DEBIT;
  String name = '';
  String comments = '';
  DateTime date = DateTime.now();
  double amount = 0.0;
  int isCountable = 1;
  int accountId = 0;
  int? transferFromAccountId;
  int? transferToAccountId;
  int categoryId = 0;
  List<Split> splits = [];
  Set<TrakoContact> contacts = {};
  Category? category;

  // Currency Support
  String? originalCurrency;
  double? originalAmount;
  double? exchangeRate;

  Transaction();

  Transaction.make(this.id, this.name, this.comments, this.date, this.amount,
      this.accountId, this.categoryId);

  @override
  String toString() {
    return 'Transaction{id: $id, transactionType: ${TransactionType.stringify(transactionType)}, name: $name, comments: $comments, date: $date, amount: $amount, accountId: $accountId, categoryId: $categoryId, category: ${category.toString()}, originalCurrency: $originalCurrency, originalAmount: $originalAmount, exchangeRate: $exchangeRate}';
  }

  Transaction.defaultObject() {
    this.transactionType = TransactionType.DEBIT;
    this.comments = "";
    this.amount = 0.0;
    this.date = DateTime.now();
    this.name = "";
    this.accountId = 0;
    this.categoryId = 0;
  }
}
