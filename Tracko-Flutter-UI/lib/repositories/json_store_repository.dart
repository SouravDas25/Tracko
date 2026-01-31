import 'dart:convert';
import '../config/api_config.dart';
import '../models/json_store.dart';
import '../services/api_client.dart';

class JsonStoreRepository {
  final _api = ApiClient();

  Future<List<JsonStoreModel>> getAll() async {
    final res = await _api.get<List<dynamic>>(ApiConfig.jsonStore);
    return res.map((e) => JsonStoreModel.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<JsonStoreModel?> getByName(String name) async {
    try {
      final res = await _api.get<Map<String, dynamic>>("${ApiConfig.jsonStore}/$name");
      return JsonStoreModel.fromJson(res);
    } catch (_) {
      return null;
    }
  }

  Future<JsonStoreModel> save(JsonStoreModel model) async {
    final res = await _api.post<Map<String, dynamic>>(ApiConfig.jsonStore, data: model.toJson());
    return JsonStoreModel.fromJson(res);
  }

  Future<JsonStoreModel> update(String name, JsonStoreModel model) async {
    final res = await _api.put<Map<String, dynamic>>("${ApiConfig.jsonStore}/$name", data: model.toJson());
    return JsonStoreModel.fromJson(res);
  }

  Future<void> delete(String name) async {
    await _api.delete("${ApiConfig.jsonStore}/$name");
  }

  // Helpers
  Future<T?> getValue<T>(String name) async {
    final item = await getByName(name);
    if (item == null) return null;
    try {
      return jsonDecode(item.value) as T;
    } catch (_) {
      return item.value as T;
    }
  }

  Future<void> saveValue(String name, dynamic value) async {
    final stringValue = value is String ? value : jsonEncode(value);
    await save(JsonStoreModel(name: name, value: stringValue));
  }
}
