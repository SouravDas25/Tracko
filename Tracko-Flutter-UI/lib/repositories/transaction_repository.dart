import '../config/api_config.dart';
import '../models/transaction.dart' as legacy;
import '../services/api_client.dart';
import '../Utils/enums.dart';

class TransactionRepository {
  final _api = ApiClient();

  Future<List<legacy.Transaction>> getAll() async {
    final res = await _api.get<List<dynamic>>(ApiConfig.transactions);
    return res.map((e) => _toLegacy(e as Map<String, dynamic>)).toList();
  }

  Future<void> deleteById(int id) async {
    await _api.delete<void>("${ApiConfig.transactions}/$id");
  }

  Future<legacy.Transaction> getById(int id) async {
    final res =
        await _api.get<Map<String, dynamic>>("${ApiConfig.transactions}/$id");
    return _toLegacy(res);
  }

  Future<List<legacy.Transaction>> getByUserId(String userId) async {
    final res =
        await _api.get<List<dynamic>>("${ApiConfig.transactions}/user/$userId");
    return res.map((e) => _toLegacy(e as Map<String, dynamic>)).toList();
  }

  Future<List<legacy.Transaction>> getByAccountId(int accountId) async {
    final res = await _api
        .get<List<dynamic>>("${ApiConfig.transactions}/account/$accountId");
    return res.map((e) => _toLegacy(e as Map<String, dynamic>)).toList();
  }

  Future<List<legacy.Transaction>> getByCategoryId(int categoryId) async {
    final res = await _api
        .get<List<dynamic>>("${ApiConfig.transactions}/category/$categoryId");
    return res.map((e) => _toLegacy(e as Map<String, dynamic>)).toList();
  }

  Future<List<legacy.Transaction>> getByUserIdAndDateRange(
    String userId, {
    required DateTime startDate,
    required DateTime endDate,
    List<int>? accountIds,
  }) async {
    final res = await _api.get<List<dynamic>>(
      "${ApiConfig.transactions}/date-range",
      query: {
        'startDate': startDate.toIso8601String().split('T').first,
        'endDate': endDate.toIso8601String().split('T').first,
        if (accountIds != null && accountIds.isNotEmpty)
          'accountIds': accountIds.join(','),
      },
    );
    return res.map((e) => _toLegacy(e as Map<String, dynamic>)).toList();
  }

  Future<legacy.Transaction> create(legacy.Transaction t) async {
    final res = await _api.post<Map<String, dynamic>>(ApiConfig.transactions,
        data: _fromLegacy(t));
    return _toLegacy(res);
  }

  Future<legacy.Transaction> update(int id, legacy.Transaction t) async {
    final res = await _api.put<Map<String, dynamic>>(
        "${ApiConfig.transactions}/$id",
        data: _fromLegacy(t));
    return _toLegacy(res);
  }

  Future<void> createTransfer({
    required int fromAccountId,
    required int toAccountId,
    required double amount,
    String? name,
    String? comments,
  }) async {
    await _api.post<void>(
      ApiConfig.transfers,
      data: {
        'fromAccountId': fromAccountId,
        'toAccountId': toAccountId,
        'amount': amount,
        'name': name,
        'comments': comments,
      },
    );
  }

  legacy.Transaction _toLegacy(Map<String, dynamic> json) {
    final t = legacy.Transaction();
    t.id = (json['id'] as num?)?.toInt();
    t.name = (json['name'] as String?) ?? '';
    t.amount = ((json['amount'] as num?) ?? 0).toDouble();
    // date expected as ISO string
    final dateStr = json['date'] as String?;
    if (dateStr != null) {
      t.date = DateTime.tryParse(dateStr) ?? DateTime.now();
    }
    // Map enums if available
    final tt = json['transactionType'] ?? json['transaction_type'];
    if (tt is int) {
      t.transactionType = tt;
    } else if (tt is num) {
      t.transactionType = tt.toInt();
    } else if (tt is String) {
      t.transactionType = TransactionType.inttify(tt);
    }
    t.accountId =
        ((json['accountId'] ?? json['account_id']) as num?)?.toInt() ?? 0;
    t.categoryId =
        ((json['categoryId'] ?? json['category_id']) as num?)?.toInt() ?? 0;
    t.isCountable = (json['isCountable'] ?? json['is_countable'] ?? 1) as int;

    // Currency fields
    t.originalCurrency = json['originalCurrency'] as String?;
    t.originalAmount = (json['originalAmount'] as num?)?.toDouble();
    t.exchangeRate = (json['exchangeRate'] as num?)?.toDouble();

    return t;
  }

  Map<String, dynamic> _fromLegacy(legacy.Transaction t) {
    final payload = {
      'transactionType': t.transactionType,
      'name': t.name,
      'date': t.date.toIso8601String(),
      'accountId': t.accountId,
      'categoryId': t.categoryId,
      'isCountable': t.isCountable,
      'description': t.comments,
      'originalCurrency': t.originalCurrency,
      'originalAmount': t.originalAmount,
    };
    // Only include amount if explicitly set (base-currency transactions)
    if (t.amount != null) {
      payload['amount'] = t.amount;
    }
    // Only include exchangeRate if explicitly provided; otherwise backend will fetch
    if (t.exchangeRate != null) {
      payload['exchangeRate'] = t.exchangeRate;
    }
    return payload;
  }

  // Aggregation methods - backend calculates
  Future<Map<String, dynamic>> getSummary(
      String userId, DateTime startDate, DateTime endDate,
      {List<int>? accountIds}) async {
    final res = await _api.get<Map<String, dynamic>>(
      "${ApiConfig.transactions}/summary",
      query: {
        'startDate': startDate.toIso8601String().split('T').first,
        'endDate': endDate.toIso8601String().split('T').first,
        if (accountIds != null && accountIds.isNotEmpty)
          'accountIds': accountIds.join(','),
      },
    );
    return res;
  }

  Future<double> getTotalIncome(
      String userId, DateTime startDate, DateTime endDate) async {
    final res = await _api.get<double>(
      "${ApiConfig.transactions}/total-income",
      query: {
        'startDate': startDate.toIso8601String().split('T').first,
        'endDate': endDate.toIso8601String().split('T').first,
      },
    );
    return res;
  }

  Future<double> getTotalExpense(
      String userId, DateTime startDate, DateTime endDate) async {
    final res = await _api.get<double>(
      "${ApiConfig.transactions}/total-expense",
      query: {
        'startDate': startDate.toIso8601String().split('T').first,
        'endDate': endDate.toIso8601String().split('T').first,
      },
    );
    return res;
  }
}
