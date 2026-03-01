import '../config/api_config.dart';
import '../models/split.dart' as legacy;
import '../services/api_client.dart';

class SplitRepository {
  final ApiClient _api;

  SplitRepository({ApiClient? api}) : _api = api ?? ApiClient();

  int? _asInt(dynamic v) {
    if (v == null) return null;
    if (v is int) return v;
    if (v is num) return v.toInt();
    if (v is String) return int.tryParse(v);
    return null;
  }

  Future<List<legacy.Split>> getByTransactionId(int transactionId) async {
    final res = await _api
        .get<List<dynamic>>("${ApiConfig.splits}/transaction/$transactionId");
    return res.map((e) => _toLegacy(e as Map<String, dynamic>)).toList();
  }

  Future<List<legacy.Split>> getByUserId(String userId) async {
    final res =
        await _api.get<List<dynamic>>("${ApiConfig.splits}/user/$userId");
    return res.map((e) => _toLegacy(e as Map<String, dynamic>)).toList();
  }

  Future<List<legacy.Split>> getUnsettledByUserId(String userId) async {
    final res = await _api
        .get<List<dynamic>>("${ApiConfig.splits}/user/$userId/unsettled");
    return res.map((e) => _toLegacy(e as Map<String, dynamic>)).toList();
  }

  Future<List<legacy.Split>> getByContactId(int contactId) async {
    final res =
        await _api.get<List<dynamic>>("${ApiConfig.splits}/contact/$contactId");
    return res.map((e) => _toLegacy(e as Map<String, dynamic>)).toList();
  }

  Future<List<legacy.Split>> getUnsettledByContactId(int contactId) async {
    final res = await _api
        .get<List<dynamic>>("${ApiConfig.splits}/contact/$contactId/unsettled");
    return res.map((e) => _toLegacy(e as Map<String, dynamic>)).toList();
  }

  Future<legacy.Split> create(legacy.Split split) async {
    final res = await _api.post<Map<String, dynamic>>(ApiConfig.splits,
        data: _fromLegacy(split));
    return _toLegacy(res);
  }

  Future<legacy.Split> update(int id, legacy.Split split) async {
    final res = await _api.put<Map<String, dynamic>>("${ApiConfig.splits}/$id",
        data: _fromLegacy(split));
    return _toLegacy(res);
  }

  Future<void> settle(int splitId) async {
    await _api.patch<void>("${ApiConfig.splits}/settle/$splitId");
  }

  Future<void> unsettle(int splitId) async {
    await _api.patch<void>("${ApiConfig.splits}/unsettle/$splitId");
  }

  Future<void> delete(int id) async {
    await _api.delete<void>("${ApiConfig.splits}/$id");
  }

  legacy.Split _toLegacy(Map<String, dynamic> json) {
    final s = legacy.Split();
    s.id = _asInt(json['id']);
    s.transactionId =
        _asInt(json['transactionId'] ?? json['transaction_id']) ?? 0;
    // backend may use String UUID userId; legacy expects int -> default 0
    s.userId = _asInt(json['userId'] ?? json['user_id']) ?? 0;
    s.contactId = _asInt(json['contactId'] ?? json['contact_id']);
    s.amount = ((json['amount'] as num?) ?? 0).toDouble();
    s.isSettled = (json['isSettled'] ?? json['is_settled'] ?? 0) as int;
    return s;
  }

  Map<String, dynamic> _fromLegacy(legacy.Split s) => {
        'transactionId': s.transactionId,
        'userId': s.userId?.toString(),
        'contactId': s.contactId,
        'amount': s.amount,
        'isSettled': s.isSettled,
      };
}
