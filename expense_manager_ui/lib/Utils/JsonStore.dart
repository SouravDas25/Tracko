

import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart' as path;
import 'package:sembast/sembast_io.dart' as JsonStoreIO;

const String jsonStoreName = "jsonStore.db";
class JsonStore {
  String jsonStorePath;

  String databasePath;

  _initDB() async {
    databasePath = await getDatabasesPath();
    jsonStorePath = path.join(databasePath,jsonStoreName);
  }

  static JsonStore _instance;

  static getJsonStore() async {
    return await JsonStoreIO.databaseFactoryIo.openDatabase(_instance.jsonStorePath);
  }
}