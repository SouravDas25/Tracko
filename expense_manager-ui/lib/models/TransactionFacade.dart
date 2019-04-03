

import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/Utils/enums.dart';
import 'package:sqflite/sqlite_api.dart';

class TransactionFacade {

  static resolveAdapter(var adapter){
    if(adapter == null) return DatabaseUtil.getAdapter();
    return adapter;
  }

  static Future<double> getCurrentAmount([int accountId]) async {
    Database db = await DatabaseUtil.getRawDatabase();
    // SELECT SUM(amount) AS amount FROM transactions WHERE transaction_type = 'DEBIT'
    // SELECT SUM(amount) AS amount FROM transactions WHERE transaction_type = 'CREDIT'
    String query = "SELECT SUM( "
        "CASE transaction_type "
        "WHEN ${TransactionType.DEBIT} THEN amount*-1 "
        "WHEN ${TransactionType.CREDIT} THEN amount "
        "END "
        ") AS amount FROM transactions ";
    if(accountId!= null){
      query+=" WHERE account_id = $accountId ";
    }
    var tmp = (await db.rawQuery(query)).toList();
    print(tmp);
    return tmp.first['amount'];
  }
}