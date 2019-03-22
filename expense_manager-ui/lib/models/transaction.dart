
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

  @Column(isNullable: false , length: 128)
  String name;

  @Column(isNullable: true , length: 512)
  String logo;

  @Column(isNullable: true , length: 512)
  String comments;

  @Column(isNullable: false)
  DateTime date;

  @Column(isNullable: false)
  double amount;

  @BelongsTo(AccountBean)
  int accountId;

  @BelongsTo(CategoryBean)
  int categoryId;

  @override
  String toString() {
    return 'Transaction{id: $id, name: $name, comments: $comments, date: $date, amount: $amount, accountId: $accountId, categoryId: $categoryId}';
  }


//  @IgnoreColumn()
//  Category category;
//
//  @IgnoreColumn()
//  Account account;




}


@GenBean()
class TransactionBean extends Bean<Transaction> with _TransactionBean {
  AccountBean _accountBean;
  CategoryBean _categoryBean;
  final String tableName = 'transactions';

  TransactionBean(Adapter adapter) : super(adapter){
    _accountBean = new AccountBean(adapter);
    _categoryBean = new CategoryBean(adapter);
  }

  @override
  AccountBean get accountBean => _accountBean;

  @override
  CategoryBean get categoryBean => _categoryBean;

//  preloadAllMappings(List<Transaction> transactions) async {
//    for(int i =0;i<transactions.length;i++){
//      transactions[i].category = await categoryBean.find(transactions[i].categoryId);
//      print(transactions[i]);
//      transactions[i].account = await accountBean.find(transactions[i].accountId);
//    }
//  }

}