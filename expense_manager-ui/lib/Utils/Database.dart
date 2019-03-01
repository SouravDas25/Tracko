
import 'package:flutter/widgets.dart';
import 'package:jaguar_query_sqflite/jaguar_query_sqflite.dart';
import 'package:path/path.dart' as path;
import 'package:sqflite/sqflite.dart';


const String databaseName = "test.db";

class Database {
  SqfliteAdapter globalAdapter;
  String databasePath ;

  Database();

  void _initDB() async {
    databasePath = await getDatabasesPath();
    databasePath = path.join(databasePath, databaseName);
    globalAdapter = SqfliteAdapter(databasePath);
  }

  static Database _instance;

  static SqfliteAdapter getAdapter() {
    if(_instance == null){
      _instance = new Database();
      _instance._initDB();
    }
    return _instance.globalAdapter;
  }

}