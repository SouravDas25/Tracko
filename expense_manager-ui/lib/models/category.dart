
import 'dart:async';

import 'package:expense_manager/models/transaction.dart';
import 'package:expense_manager/models/user.dart';
import 'package:jaguar_orm/jaguar_orm.dart';
import 'package:jaguar_query/jaguar_query.dart';

part 'category.jorm.dart';

class Category {

  Category();

  Category.make(this.id,this.name,this.userId);

  @PrimaryKey(auto: true)
  int id;

  @Column(isNullable: false , length: 250)
  String name;

  @BelongsTo(UserBean)
  int userId;

  @HasMany(TransactionBean)
  List<Transaction> transactions;

  @override
  String toString() {
    return 'Category{id: $id, name: $name, userId: $userId, transactions: $transactions}';
  }

  static int defaultCategory() {
    return 1;
  }


}

@GenBean()
class CategoryBean extends Bean<Category> with _CategoryBean {
  CategoryBean(Adapter adapter) : super(adapter);

  final String tableName = 'categories';

  @override
  TransactionBean get transactionBean => new TransactionBean(adapter);

  @override
  UserBean get userBean => new UserBean(adapter);
}