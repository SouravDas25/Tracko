

import 'dart:async';

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
}

@GenBean()
class UserBean extends Bean<User> with _UserBean {
  UserBean(Adapter adapter) : super(adapter);

  final String tableName = 'users';
}