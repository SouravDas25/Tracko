import 'dart:async';

import 'package:Tracko/models/transaction.dart';
import 'package:Tracko/orm_stub.dart';
import 'package:sqflite/sqflite.dart' hide Transaction;
import 'package:Tracko/models/user.dart';
// import 'package:jaguar_orm/jaguar_orm.dart'; // Removed - migrating to plain sqflite
// import 'package:jaguar_query/jaguar_query.dart'; // Removed - migrating to plain sqflite

// ORM code generation removed

class Account {
  int? id;
  String name = '';
  int? userId;
  List<Transaction> transactions = [];

  Account();

  Account.make(this.id, this.name, this.userId);

  @override
  String toString() {
    return 'Account{id: $id, name: $name, userId: $userId, transactions: $transactions}';
  }

  static int defaultAccountId() {
    return 1;
  }
}

@GenBean()
class AccountBean extends Bean<Account> {
  AccountBean(dynamic adapter)
      : super(adapter is Adapter ? adapter : Adapter(adapter as Database));

  final String tableName = 'accounts';

  @override
  TransactionBean get transactionBean => new TransactionBean(adapter);

  @override
  UserBean get userBean => new UserBean(adapter);
}
