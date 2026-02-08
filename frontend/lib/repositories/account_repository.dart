import 'package:dio/dio.dart';
import '../config/api_config.dart';
import '../models/account.dart' as legacy;
import '../services/api_client.dart';

class AccountRepository {
  final _api = ApiClient();

  Future<List<legacy.Account>> getAllAccounts() async {
    final res = await _api.get<List<dynamic>>(ApiConfig.accounts);
    return res.map((e) => _toLegacyAccount(e as Map<String, dynamic>)).toList();
  }

  Future<Map<int, double>> getAccountBalances() async {
    final res = await _api.get<Map<String, dynamic>>(ApiConfig.accountBalances);
    final out = <int, double>{};
    res.forEach((key, value) {
      final id = int.tryParse(key);
      if (id == null) return;
      if (value is num) {
        out[id] = value.toDouble();
      }
    });
    return out;
  }

  Future<void> deleteAccount(int id) async {
    try {
      await _api.delete<void>('${ApiConfig.accounts}/$id');
    } on DioException catch (e) {
      final status = e.response?.statusCode ?? 0;
      if (status == 400 || status == 409) {
        rethrow; // caller can map to domain exception
      }
      rethrow;
    }
  }

  Future<legacy.Account> createAccount(
      String name, String userId, String currency) async {
    final res = await _api.post<Map<String, dynamic>>(ApiConfig.accounts,
        data: {'name': name, 'userId': userId, 'currency': currency});
    return _toLegacyAccount(res);
  }

  Future<legacy.Account> updateAccount(
      int id, String name, String userId, String currency) async {
    final res = await _api.put<Map<String, dynamic>>(
        '${ApiConfig.accounts}/$id',
        data: {'name': name, 'userId': userId, 'currency': currency});
    return _toLegacyAccount(res);
  }

  legacy.Account _toLegacyAccount(Map<String, dynamic> json) {
    final a = legacy.Account();
    a.id = (json['id'] as num?)?.toInt();
    a.name = (json['name'] as String?) ?? '';
    a.currency = (json['currency'] as String?) ?? 'INR';
    // Backend uses userId as String; legacy model expects int? -> leave null
    return a;
  }
}
