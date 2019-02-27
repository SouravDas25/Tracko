
import 'dart:async';

import 'package:jaguar_orm/jaguar_orm.dart';
import 'package:jaguar_query/jaguar_query.dart';

class Transaction {
  Transaction();

  Transaction.make();

  @PrimaryKey(auto: true)
  int id;

  @Column(isNullable: false , length: 250)
  String name;

  @Column(isNullable: false)
  DateTime date;

  @Column(isNullable: false)
  double amount;


}