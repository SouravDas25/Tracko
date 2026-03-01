import 'dart:convert';
import 'package:tracko/models/json_store.dart';
import 'package:tracko/repositories/json_store_repository.dart';
import 'package:tracko/di/di.dart';

class JsonStore {
  static JsonStoreRepository get _repo => sl<JsonStoreRepository>();

  static Future<bool?> has(String key) async {
    final obj = await get(key);
    return obj != null;
  }

  static Future<String?> get(String key) async {
    final item = await _repo.getByName(key);
    return item?.value;
  }

  static put(String key, String value) async {
    await _repo.save(JsonStoreModel(name: key, value: value));
  }

  static deleteAll() async {
    final all = await _repo.getAll();
    for (final item in all) {
      await _repo.delete(item.name);
    }
  }
}
