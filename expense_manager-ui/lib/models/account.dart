


import 'dart:async';

import 'package:expense_manager/models/transaction.dart';
import 'package:expense_manager/models/user.dart';
import 'package:jaguar_orm/jaguar_orm.dart';
import 'package:jaguar_query/jaguar_query.dart';

part 'account.jorm.dart';


class Account {
  Account();

  Account.make(this.id,this.name,this.userId);

  @PrimaryKey(auto: true)
  int id;

  @Column(isNullable: false , length: 250)
  String name;

  @BelongsTo(UserBean)
  int userId;

  @HasMany(TransactionBean)
  List<Transaction> transactions;

}

@GenBean()
class AccountBean extends Bean<Account> with _AccountBean {
  AccountBean(Adapter adapter) : super(adapter);

  final String tableName = 'accounts';

  @override
  TransactionBean get transactionBean => new TransactionBean(adapter);

  @override
  UserBean get userBean => new UserBean(adapter);
}