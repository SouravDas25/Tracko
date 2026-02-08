import 'dart:async';

import 'package:tracko/models/transaction.dart';
import 'package:tracko/models/user.dart';

class Account {
  int? id;
  String name = '';
  int? userId;
  String currency = 'INR';
  List<Transaction> transactions = [];

  Account();

  Account.make(this.id, this.name, this.userId, {this.currency = 'INR'});

  @override
  String toString() {
    return 'Account{id: $id, name: $name, userId: $userId, currency: $currency, transactions: $transactions}';
  }

  static int defaultAccountId() {
    return 1;
  }
}
