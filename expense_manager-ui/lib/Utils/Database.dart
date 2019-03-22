import 'package:expense_manager/models/account.dart';
import 'package:expense_manager/models/category.dart';
import 'package:expense_manager/models/setting.dart';
import 'package:expense_manager/models/transaction.dart';
import 'package:expense_manager/models/user.dart';
import 'package:jaguar_query_sqflite/jaguar_query_sqflite.dart';
import 'package:path/path.dart' as path;
import 'package:sqflite/sqflite.dart';
import 'package:sembast/sembast.dart' as JsonStore;
import 'package:sembast/sembast_io.dart' as JsonStoreIO;

const String databaseName = "test.db";
const String jsonStoreName = "jsonStore.db";

class DatabaseUtil {
  String databasePath;
  String jsonStorePath;

  DatabaseUtil();

  _initDB() async {
    databasePath = await getDatabasesPath();
    databasePath = path.join(databasePath, databaseName);
    jsonStorePath = path.join(databasePath,jsonStoreName);
  }

  static DatabaseUtil _instance;

  static getJsonStore() async {
    return await JsonStoreIO.databaseFactoryIo.openDatabase(_instance.jsonStorePath);
  }

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
    Category category = await categoryBean.find(1);
    if(category == null){
      category = Category.make(1, "Not Categorised", 1);
      await categoryBean.upsert(category);
    }
//    print(user);
  }
}
