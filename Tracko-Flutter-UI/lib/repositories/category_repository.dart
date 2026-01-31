import 'package:dio/dio.dart';
import '../config/api_config.dart';
import '../models/category.dart' as legacy;
import '../services/api_client.dart';

class CategoryRepository {
  final _api = ApiClient();

  Future<List<legacy.Category>> getAll() async {
    final res = await _api.get<List<dynamic>>(ApiConfig.categories);
    return res.map((e) => _toLegacyCategory(e as Map<String, dynamic>)).toList();
  }

  Future<legacy.Category> getById(int id) async {
    final res = await _api.get<Map<String, dynamic>>("${ApiConfig.categories}/$id");
    return _toLegacyCategory(res);
  }

  Future<legacy.Category> create(String name, {String? userId}) async {
    final body = {
      'name': name,
      if (userId != null) 'userId': userId,
    };
    final res = await _api.post<Map<String, dynamic>>(ApiConfig.categories, data: body);
    return _toLegacyCategory(res);
  }

  Future<legacy.Category> update(int id, String name, {String? userId}) async {
    final body = {
      'name': name,
      if (userId != null) 'userId': userId,
    };
    final res = await _api.put<Map<String, dynamic>>('${ApiConfig.categories}/$id', data: body);
    return _toLegacyCategory(res);
  }

  Future<void> delete(int id) async {
    await _api.delete<void>("${ApiConfig.categories}/$id");
  }

  Future<legacy.Category> findOrCreateByName(String name, {String? userId}) async {
    final all = await getAll();
    final existing = all.firstWhere(
      (c) => (c.name).toLowerCase() == name.toLowerCase(),
      orElse: () => legacy.Category(),
    );
    if (existing.id != null) return existing;
    return await create(name, userId: userId);
  }

  legacy.Category _toLegacyCategory(Map<String, dynamic> json) {
    final c = legacy.Category();
    c.id = (json['id'] as num?)?.toInt();
    c.name = (json['name'] as String?) ?? '';
    // legacy model expects int? userId; backend has String -> leave null
    return c;
  }
}
