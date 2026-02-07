import 'dart:async';

import 'package:tracko/models/transaction.dart';
import 'package:tracko/models/user.dart';

class Category {
  int? id;
  String name = '';
  int? userId;
  List<Transaction> transactions = [];

  Category();

  Category.make(this.id, this.name, this.userId);

  @override
  String toString() {
    return 'Category{id: $id, name: $name, userId: $userId, transactions: $transactions}';
  }

  static int defaultCategoryId() {
    return 1;
  }
}
