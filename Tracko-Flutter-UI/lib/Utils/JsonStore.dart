import 'package:Tracko/Utils/DatabaseUtil.dart';
import 'package:sqflite/sqflite.dart';

class JsonStore {
  static Future<bool?> has(String key) async {
    var obj = await get(key);
    return obj != null ? true : false;
  }

  static Future<String?> get(String key) async {
    Database database = await DatabaseUtil.getRawDatabase();
    List<dynamic> result = await database
        .rawQuery("SELECT value FROM json_store WHERE name = '$key';");
    if (result.length > 0) return result.first['value'];
    return null;
  }

  static put(String key, String value) async {
    Database database = await DatabaseUtil.getRawDatabase();
    await database.execute("""
        INSERT OR REPLACE INTO json_store(name, value) VALUES('$key','$value');
        """);
  }

  static deleteAll() async {
    Database database = await DatabaseUtil.getRawDatabase();
    await database.execute("""
        DELETE FROM json_store WHERE 1;
        """);
  }
}
