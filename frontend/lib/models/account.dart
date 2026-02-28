import 'dart:async';

import 'package:tracko/models/transaction.dart';
import 'package:tracko/models/user.dart';

class Account {
  int? id;
  String name = '';
  String currency = 'INR';
  List<Transaction> transactions = [];

  Account();

  Account.make(this.id, this.name, {this.currency = 'INR'});

  @override
  String toString() {
    return 'Account{id: $id, name: $name, currency: $currency, transactions: $transactions}';
  }

  static int defaultAccountId() {
    return 1;
  }
}
