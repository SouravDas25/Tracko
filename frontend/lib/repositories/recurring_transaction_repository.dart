import 'package:tracko/config/api_config.dart';
import 'package:tracko/models/recurring_transaction.dart';
import 'package:tracko/services/api_client.dart';

class RecurringTransactionRepository {
  final ApiClient _api;

  RecurringTransactionRepository({ApiClient? api}) : _api = api ?? ApiClient();

  Future<List<RecurringTransaction>> getAll() async {
    final res = await _api.get<List<dynamic>>(ApiConfig.recurringTransactions);
    return res
        .map((e) => RecurringTransaction.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<RecurringTransaction> getById(int id) async {
    final res = await _api
        .get<Map<String, dynamic>>("${ApiConfig.recurringTransactions}/$id");
    return RecurringTransaction.fromJson(res);
  }

  Future<RecurringTransaction> create(RecurringTransaction rt) async {
    final res = await _api.post<Map<String, dynamic>>(
        ApiConfig.recurringTransactions,
        data: rt.toJson());
    return RecurringTransaction.fromJson(res);
  }

  Future<RecurringTransaction> update(int id, RecurringTransaction rt) async {
    final res = await _api.put<Map<String, dynamic>>(
        "${ApiConfig.recurringTransactions}/$id",
        data: rt.toJson());
    return RecurringTransaction.fromJson(res);
  }

  Future<void> delete(int id) async {
    await _api.delete<void>("${ApiConfig.recurringTransactions}/$id");
  }
}
