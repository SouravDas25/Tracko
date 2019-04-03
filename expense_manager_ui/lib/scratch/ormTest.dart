

import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/models/transaction.dart';
import 'package:jaguar_query_sqflite/jaguar_query_sqflite.dart';

void main() async {
  SqfliteAdapter adapter = await DatabaseUtil.getAdapter();
  await adapter.connect();
  TransactionBean transactionBean = new TransactionBean(adapter);

  Find query = transactionBean.finder;
  query.limit(5);
  var result = await transactionBean.findMany(query);
  print(result);

}