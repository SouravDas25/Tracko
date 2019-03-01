
import 'dart:async';

import 'package:expense_manager/models/account.dart';
import 'package:expense_manager/models/category.dart';
import 'package:jaguar_orm/jaguar_orm.dart';
import 'package:jaguar_query/jaguar_query.dart';

part 'transaction.jorm.dart';

class Transaction {
  Transaction();

  Transaction.make(this.id,this.name,this.date,this.amount,this.accountId,this.categoryId);

  @PrimaryKey(auto: true)
  int id;

  @Column(isNullable: false , length: 250)
  String name;

  @Column(isNullable: false)
  DateTime date;

  @Column(isNullable: false)
  double amount;

  @BelongsTo(AccountBean)
  int accountId;

  @BelongsTo(CategoryBean)
  int categoryId;

}


@GenBean()
class TransactionBean extends Bean<Transaction> with _TransactionBean {
  TransactionBean(Adapter adapter) : super(adapter);

  final String tableName = 'transactions';

  @override
  AccountBean get accountBean => new AccountBean(adapter);

  @override
  CategoryBean get categoryBean => new CategoryBean(adapter);
}