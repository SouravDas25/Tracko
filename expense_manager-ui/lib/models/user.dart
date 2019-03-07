

import 'dart:async';

import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/models/account.dart';
import 'package:expense_manager/models/category.dart';
import 'package:jaguar_orm/jaguar_orm.dart';
import 'package:jaguar_query/jaguar_query.dart';

part 'user.jorm.dart';

// The model
class User {
  User();

  User.make(this.id, this.name, this.phoneNo, this.email);

  @PrimaryKey(auto: true)
  int id;

  @Column(isNullable: false , length: 250)
  String name;

  @Column(isNullable: false , length: 10)
  String phoneNo;

  @Column(isNullable: true , length: 250)
  String email;

  @HasMany(AccountBean)
  List<Account> accounts;

  @HasMany(CategoryBean)
  List<Category> categories;

  @override
  String toString() {
    return "$id , $name , $email , $phoneNo ";
  }
}

@GenBean()
class UserBean extends Bean<User> with _UserBean {
  UserBean(Adapter adapter) : super(adapter);

  final String tableName = 'users';

  @override
  // TODO: implement accountBean
  AccountBean get accountBean => new AccountBean(adapter);

  @override
  // TODO: implement categoryBean
  CategoryBean get categoryBean => null;

  static Future<User> getCurrentUser({adapter}) async {
    User user;
    if(adapter == null){
      adapter = await DatabaseUtil.getAdapter();
      await adapter.connect();
      user = await UserBean(adapter).find(1);
      await adapter.close();
    }
    else {
      user = await UserBean(adapter).find(1);
    }
    return user;
  }
}