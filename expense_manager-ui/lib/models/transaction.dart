
import 'dart:async';

import 'package:expense_manager/models/account.dart';
import 'package:expense_manager/models/category.dart';
import 'package:jaguar_orm/jaguar_orm.dart';
import 'package:jaguar_query/jaguar_query.dart';
import 'package:jaguar_query_sqflite/jaguar_query_sqflite.dart';

part 'transaction.jorm.dart';

class Transaction {
  Transaction();

  Transaction.make(this.id,this.comments,this.date,this.amount,this.accountId,this.categoryId);

  @PrimaryKey(auto: true)
  int id;

  @Column(isNullable: false , length: 500)
  String comments;

  @Column(isNullable: false)
  DateTime date;

  @Column(isNullable: false)
  double amount;

  @BelongsTo(AccountBean)
  int accountId;

  @BelongsTo(CategoryBean)
  int categoryId;

  Category category;

  Account account;

  @override
  String toString() {
    return 'Transaction{id: $id, comments: $comments, date: $date, amount: $amount, accountId: $accountId, categoryId: $categoryId, category: $category, account: $account}';
  }


}


@GenBean()
class TransactionBean extends Bean<Transaction> with _TransactionBean {
  TransactionBean(Adapter adapter) : super(adapter);

  final String tableName = 'transactions';

  @override
  AccountBean get accountBean => new AccountBean(adapter);

  @override
  CategoryBean get categoryBean => new CategoryBean(adapter);

  static preLoadMappings(List<Transaction> transactions,SqfliteAdapter adapter) async {
    var cb = new CategoryBean(adapter);
    var ab = new AccountBean(adapter);
    for(int i = 0 ; i < transactions.length ; i++) {
      transactions[i].category = await cb.find(transactions[i].categoryId);
      transactions[i].account = await ab.find(transactions[i].accountId);
    }
  }

}