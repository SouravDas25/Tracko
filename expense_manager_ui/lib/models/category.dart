
import 'dart:async';

import 'package:expense_manager/Utils/Database.dart';
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

  static int defaultCategoryId() {
    return 1;
  }

  static Future<int> findOrCreateByName(String name) async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    CategoryBean categoryBean = new CategoryBean(adapter);
    List<Category> categories = await categoryBean.getAll();
    Category category;
    for(Category cat in categories){
      if(cat.name.toLowerCase().compareTo(name.toLowerCase()) == 0) {
        category = cat;
      }
    }
    if(category == null) {
      category = new Category();
      category.name = name;
      category.userId = 1;
      category.id = await categoryBean.insert(category);
    }
    return category.id;
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