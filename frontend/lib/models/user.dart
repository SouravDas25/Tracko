import 'dart:async';

import 'package:tracko/models/account.dart';
import 'package:tracko/models/category.dart';
import 'package:tracko/models/split.dart';
import 'package:tracko/models/user_currency.dart';

// The model
class User {
  int? id;
  String profilePic = '';
  String name = '';
  String phoneNo = '';
  String email = '';
  String fireBaseId = '';
  String globalId = '';
  String baseCurrency = 'INR';
  List<Account> accounts = [];
  List<Category> categories = [];
  List<Split> splits = [];
  List<UserCurrency> secondaryCurrencies = [];

  User();

  User.make(this.id, this.name, this.phoneNo, this.email);

  @override
  String toString() {
    return "{$id , $name , $email , $phoneNo , $globalId}";
  }
}
