


import 'package:expense_manager/models/account.dart';
import 'package:expense_manager/models/category.dart';
import 'package:http/http.dart';

class PossibleTransaction {

  Category category;
  Account account;

  List<double> amounts;
  List<DateTime> dates;
  List<String>  comments;

  String type;
  bool valid;

  static fromJson(dynamic jsonResponse){
    PossibleTransaction transaction = new PossibleTransaction();
    transaction.amounts = new List<double>.from(jsonResponse['amounts']);
    transaction.comments = new List<String>.from(jsonResponse['comments']);
    transaction.dates = new List<DateTime>();
    for(dynamic date in jsonResponse['dates']){
      transaction.dates.add(DateTime.parse(date.toString()));
    }
    transaction.type = jsonResponse['type'];
    transaction.valid = jsonResponse['valid'];
    return transaction;
  }

  @override
  String toString() {
    return 'PossibleTransaction{amounts: $amounts, dates: $dates, comments: $comments, type: $type, valid: $valid}';
  }


}