


import 'package:expense_manager/Utils/Database.dart';

class InitializeApp {
  static init() async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
//    await DatabaseUtil.dropTables(adapter);
    await DatabaseUtil.createTables(adapter);
//    await adapter.close();
  }
}