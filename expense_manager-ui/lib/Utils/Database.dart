import 'package:expense_manager/models/account.dart';
import 'package:expense_manager/models/category.dart';
import 'package:expense_manager/models/setting.dart';
import 'package:expense_manager/models/transaction.dart';
import 'package:expense_manager/models/user.dart';
import 'package:jaguar_query_sqflite/jaguar_query_sqflite.dart';
import 'package:path/path.dart' as path;
import 'package:sqflite/sqflite.dart';

const String databaseName = "test.db";

class DatabaseUtil {
  String databasePath;

  DatabaseUtil();

  _initDB() async {
    databasePath = await getDatabasesPath();
    databasePath = path.join(databasePath, databaseName);
  }

  static DatabaseUtil _instance;

  static getRawDatabase() async {
    Database db = (await openDatabase(_instance.databasePath));
    return db;
  }

  static getAdapter() async {
    if (_instance == null) {
      _instance = new DatabaseUtil();
      await _instance._initDB();
    }
    return new SqfliteAdapter(_instance.databasePath);
  }

  static dropTables(adapter) async {
    var userBean = new UserBean(adapter);
    var accountBean = new AccountBean(adapter);
    var categoryBean = new CategoryBean(adapter);
    var transactionBean = new TransactionBean(adapter);
    var settingBean = new SettingBean(adapter);
    await userBean.drop();
    await accountBean.drop();
    await categoryBean.drop();
    await transactionBean.drop();
    await settingBean.drop();
  }

  static createTables(adapter) async {
    var userBean = new UserBean(adapter);
    var accountBean = new AccountBean(adapter);
    var categoryBean = new CategoryBean(adapter);
    var transactionBean = new TransactionBean(adapter);
    var settingBean = new SettingBean(adapter);
    await userBean.createTable(ifNotExists: true);
    await accountBean.createTable(ifNotExists: true);
    await categoryBean.createTable(ifNotExists: true);
    await transactionBean.createTable(ifNotExists: true);
    await settingBean.createTable(ifNotExists: true);
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
