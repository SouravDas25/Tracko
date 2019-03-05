import 'package:expense_manager/models/account.dart';
import 'package:expense_manager/models/category.dart';
import 'package:expense_manager/models/transaction.dart';
import 'package:expense_manager/models/user.dart';
import 'package:jaguar_query_sqflite/jaguar_query_sqflite.dart';
import 'package:path/path.dart' as path;
import 'package:sqflite/sqflite.dart';

const String databaseName = "test.db";

class Database {
  String databasePath;

  Database();

  _initDB() async {
    databasePath = await getDatabasesPath();
    databasePath = path.join(databasePath, databaseName);
  }

  static Database _instance;

  static getAdapter() async {
    if (_instance == null) {
      _instance = new Database();
      await _instance._initDB();
    }
    return new SqfliteAdapter(_instance.databasePath);
  }

  static createTables(adapter) async {
    var userBean = new UserBean(adapter);
    var accountBean = new AccountBean(adapter);
    var categoryBean = new CategoryBean(adapter);
    var transactionBean = new TransactionBean(adapter);
    await userBean.createTable(ifNotExists: true);
    await accountBean.createTable(ifNotExists: true);
    await categoryBean.createTable(ifNotExists: true);
    await transactionBean.createTable(ifNotExists: true);
    User user = await userBean.find(1);
//    print(user);
    if (user == null) {
      user = User.make(1, "Sourav Das", "8100448204", "souravbumbadas25@gmail.com");
      await userBean.insert(user);
    }
    Account savingsAccount = await accountBean.find(1);
    if(savingsAccount == null){
      savingsAccount = Account.make(1, "Savings", 1);
      await accountBean.insert(savingsAccount);
    }
//    print(user);
  }
}
