


import 'package:expense_manager/Utils/Database.dart';

class InitializeApp {
  static init() async {
    var adapter = await Database.getAdapter();
    await adapter.connect();
    await Database.createTables(adapter);
    await adapter.close();
  }
}