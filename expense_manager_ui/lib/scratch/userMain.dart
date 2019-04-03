


import 'package:expense_manager/Utils/Database.dart';
import 'package:expense_manager/models/user.dart';
import 'package:jaguar_query_sqflite/jaguar_query_sqflite.dart';
import 'package:sqflite/sqflite.dart';

void main() async {




  SqfliteAdapter adapter = await DatabaseUtil.getAdapter();
  await adapter.connect();
  var userBean = new UserBean(adapter);
//  await userBean.createTable(ifNotExists: true);
  User user = await userBean.find(1);
  if (user == null) {
    user = User.make(1, "Sourav Das", "8100448204", "souravbumbadas25@gmail.com");
    await userBean.insert(user);
  }
  print(await userBean.getAll());
  print(user);
//  await adapter.close();

  Database db = await DatabaseUtil.getRawDatabase();
  var result = await db.rawQuery("SELECT * FROM transactions");
  print(result);


//  db.close();


}