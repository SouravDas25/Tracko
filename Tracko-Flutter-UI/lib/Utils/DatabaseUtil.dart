import 'dart:math';

import 'package:tracko/Utils/JsonStore.dart';
import 'package:tracko/Utils/migrations/migrations.dart';
import 'package:tracko/controllers/TransactionController.dart';
import 'package:tracko/models/account.dart';
import 'package:tracko/models/category.dart';
import 'package:tracko/models/chats.dart';
import 'package:tracko/models/split.dart';
import 'package:tracko/models/transaction.dart';
import 'package:tracko/models/user.dart';
// // import 'package:jaguar_query_sqflite/jaguar_query_sqflite.dart'; // Removed - migrating to plain sqflite // TODO: Replace with plain sqflite DAOs
import 'package:path/path.dart' as path;

import 'migrations/migration_control.dart';

const String databaseName = "test.db";

class DatabaseUtil {
  String? databasePath;
  dynamic rawDatabase;

  DatabaseUtil();

  static DatabaseUtil _instance = new DatabaseUtil();

  static isNotConnected() {
    return _instance.rawDatabase == null || !(_instance.rawDatabase?.isOpen ?? false);
  }

  static Future<String> fetchDatabasePath() async {
    if (_instance.databasePath == null) {
      _instance.databasePath = path.join('', databaseName);
    }
    return _instance.databasePath ?? '';
  }

  static _connect() async {
    return null;
  }

  static closeDatabase() async {
    return null;
  }

  static getRawDatabase() async {
    if (isNotConnected()) {
      await DatabaseUtil._connect();
    }
    return _instance.rawDatabase;
  }

  static getAdapter({dynamic database}) async {
    // TODO: Replace with proper DAO pattern. For now, return raw Database.
    if (database != null) {
      return database;
    }
    if (isNotConnected()) {
      await DatabaseUtil._connect();
    }
    return _instance.rawDatabase;
  }

  static dropTables(dynamic adapter) async {
    // TODO: Implement with raw SQL or DAOs when ORM is replaced
    await JsonStore.deleteAll();
  }

  static reset() async {
    await JsonStore.deleteAll();
    // Local DB disabled: nothing else to reset.
  }

  static seedTables(dynamic adapter) async {
    // TODO: Implement seeding with raw SQL INSERT statements
    // For now, skip seeding to allow app to compile and run
    dynamic db = adapter ?? _instance.rawDatabase;
    if (db == null) return;
    
    // Check if accounts table has data
    try {
      var accountCount = await db.rawQuery('SELECT COUNT(*) as count FROM accounts');
      if (accountCount.isNotEmpty && accountCount[0]['count'] == 0) {
        await db.insert('accounts', {'id': 1, 'name': 'Savings', 'userId': 1});
        await db.insert('accounts', {'id': 2, 'name': 'Cash', 'userId': 1});
      }
    } catch (e) {
      print('Seeding accounts failed or table does not exist: $e');
    }

    try {
      var categoryCount = await db.rawQuery('SELECT COUNT(*) as count FROM categories');
      if (categoryCount.isNotEmpty && categoryCount[0]['count'] == 0) {
        await db.insert('categories', {'id': 1, 'name': 'Default', 'userId': 1});
        await db.insert('categories', {'id': 2, 'name': 'Food', 'userId': 1});
        await db.insert('categories', {'id': 3, 'name': 'Travel', 'userId': 1});
        await db.insert('categories', {'id': 4, 'name': 'Misc', 'userId': 1});
        await db.insert('categories', {'id': 5, 'name': 'Personal', 'userId': 1});
        await db.insert('categories', {'id': 6, 'name': 'Salary', 'userId': 1});
        await db.insert('categories', {'id': 7, 'name': 'Grocery', 'userId': 1});
      }
    } catch (e) {
      print('Seeding categories failed or table does not exist: $e');
    }
  }

  static void runQuery(String query) async {
    final db = await DatabaseUtil.getRawDatabase();
    await db?.execute(query);
  }

  static Future<void> clearTransaction() async {
    // await TransactionController.clear();
    // TODO: Implement clear method in TransactionController
  }

  static Future<void> seedTransaction() async {
    var rng = new Random();

    var category = [
      {
        "id": 1,
        "name": "Swiggy",
        "domain": "swiggy.com",
        "logo": "https://logo.clearbit.com/swiggy.com",
        "entity_type": "Primary",
        "data": null,
        "category": "Food"
      },
      {
        "id": 3,
        "name": "Paytm",
        "domain": "paytm.com",
        "logo": "https://logo.clearbit.com/Paytm.com",
        "entity_type": "Secondary",
        "data": null,
        "category": "Payment App"
      }
    ];


    for (int i = 0; i < 50; i++) {
      category.shuffle(rng);
      var obj = {
        "amounts": [rng.nextInt(500).toDouble()],
        "type": "DEBIT",
        "comments": ["INR", "SWIGGY", "Valid", "OTP"],
        "entity": category,
        "valid": true,
        "request": {
          "text": "This is a Seeding SMS",
          "address": "TB-Paytm",
          "date": DateTime.now()
              .subtract(Duration(days: rng.nextInt(15)))
              .toString()
        }
      };
      var transaction = await TransactionController.fromJson(obj);
      await TransactionController.saveTransaction(transaction);
    }
  }
}
