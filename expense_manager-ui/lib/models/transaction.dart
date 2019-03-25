
import 'dart:async';

import 'package:expense_manager/Utils/CommonUtil.dart';
import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/Utils/enums.dart';
import 'package:expense_manager/models/account.dart';
import 'package:expense_manager/models/category.dart';
import 'package:jaguar_orm/jaguar_orm.dart';
import 'package:jaguar_query/jaguar_query.dart';
import 'package:jaguar_query_sqflite/jaguar_query_sqflite.dart';
import 'package:sqflite/sqflite.dart' ;

part 'transaction.jorm.dart';

class Transaction {
  Transaction();


  Transaction.make(this.id, this.name, this.logo, this.comments, this.date,
      this.amount, this.accountId, this.categoryId);

  @PrimaryKey(auto: true)
  int id;

  @Column(isNullable: false )
  int transactionType;

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
    return 'Transaction{id: $id, transactionType: ${TransactionType.stringify(transactionType)}, name: $name, logo: $logo, comments: $comments, date: $date, amount: $amount, accountId: $accountId, categoryId: $categoryId}';
  }

  Transaction.defaultObject() {
    this.transactionType = TransactionType.DEBIT;
    this.logo = CommonUtil.toImageUrl("Item");
    this.comments = "";
    this.amount = 0.0;
    this.date = DateTime.now();
    this.name = "Item";
    this.accountId = Account.defaultAccountId();
    this.categoryId = Category.defaultCategoryId();
  }

  static Future<Transaction> fromJson(dynamic jsonResponse) async {
    bool valid = jsonResponse['valid'];
    if(!valid) return null;
    Transaction transaction = new Transaction();
    transaction.amount = jsonResponse['amounts'][0];
    transaction.comments = jsonResponse['request']['text'];
    transaction.date = DateTime.parse(jsonResponse['request']['date']);
    if(jsonResponse['dates'] != null){
      transaction.date = DateTime.parse(jsonResponse['dates'][0].toString());
    }
    transaction.transactionType = TransactionType.inttify(jsonResponse['type']);

    transaction.name = "Item";
    transaction.accountId = Account.defaultAccountId();
    if(jsonResponse['entity'] != null && jsonResponse['entity'].length > 0){
      dynamic entity = jsonResponse['entity'][0];
      transaction.name = entity['name']; 
      transaction.categoryId = await Category.findOrCreateByName(entity['category']);
      transaction.logo = entity['logo'];
    }
    else {
      transaction.categoryId = Category.defaultCategoryId();
      transaction.logo = CommonUtil.toImageUrl("Item");
    }
    return transaction;
  }

}

@GenBean()
class TransactionBean extends Bean<Transaction> with _TransactionBean {
  AccountBean _accountBean;
  CategoryBean _categoryBean;
  final String tableName = 'transactions';

  TransactionBean(Adapter adapter) : super(adapter) {
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