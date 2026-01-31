import '../config/api_config.dart';
import '../models/split.dart' as legacy;
import '../services/api_client.dart';

class SplitRepository {
  final _api = ApiClient();

  Future<List<legacy.Split>> getByTransactionId(int transactionId) async {
    final res = await _api.get<List<dynamic>>("${ApiConfig.splits}/transaction/$transactionId");
    return res.map((e) => _toLegacy(e as Map<String, dynamic>)).toList();
  }

  Future<List<legacy.Split>> getByUserId(String userId) async {
    final res = await _api.get<List<dynamic>>("${ApiConfig.splits}/user/$userId");
    return res.map((e) => _toLegacy(e as Map<String, dynamic>)).toList();
  }

  Future<List<legacy.Split>> getUnsettledByUserId(String userId) async {
    final res = await _api.get<List<dynamic>>("${ApiConfig.splits}/user/$userId/unsettled");
    return res.map((e) => _toLegacy(e as Map<String, dynamic>)).toList();
  }

  Future<legacy.Split> create(legacy.Split split) async {
    final res = await _api.post<Map<String, dynamic>>(ApiConfig.splits, data: _fromLegacy(split));
    return _toLegacy(res);
  }

  Future<legacy.Split> update(int id, legacy.Split split) async {
    final res = await _api.put<Map<String, dynamic>>("${ApiConfig.splits}/$id", data: _fromLegacy(split));
    return _toLegacy(res);
  }

  Future<void> settle(int splitId) async {
    await _api.post<void>("${ApiConfig.splits}/settle/$splitId");
  }

  Future<void> delete(int id) async {
    await _api.delete<void>("${ApiConfig.splits}/$id");
  }

  legacy.Split _toLegacy(Map<String, dynamic> json) {
    final s = legacy.Split();
    s.id = (json['id'] as num?)?.toInt();
    s.transactionId = (json['transactionId'] ?? json['transaction_id'] as num?)?.toInt() ?? 0;
    // backend uses String userId; legacy expects int -> default 0
    s.userId = (json['userId'] ?? json['user_id'] as num?)?.toInt() ?? 0;
    s.amount = ((json['amount'] as num?) ?? 0).toDouble();
    s.isSettled = (json['isSettled'] ?? json['is_settled'] ?? 0) as int;
    return s;
  }

  Map<String, dynamic> _fromLegacy(legacy.Split s) => {
        'transactionId': s.transactionId,
        'userId': s.userId?.toString(),
        'amount': s.amount,
        'isSettled': s.isSettled,
      };
}
