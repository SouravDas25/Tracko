import '../config/api_config.dart';
import '../models/transaction.dart' as legacy;
import '../models/account.dart' as legacy_account;
import '../models/category.dart' as legacy_category;
import '../models/contact.dart' as legacy_contact;
import '../models/split.dart' as legacy_split;
import '../dtos/TrackoContact.dart';
import '../services/api_client.dart';
import '../Utils/enums.dart';
import '../Utils/AppLog.dart';

class TransactionRepository {
  final _api = ApiClient();

  Future<List<legacy.Transaction>> getAll({
    int? month,
    int? year,
    int page = 0,
    int size = 500,
  }) async {
    final now = DateTime.now();
    final res = await _api.get<Map<String, dynamic>>(
      ApiConfig.transactions,
      query: {
        'month': month ?? now.month,
        'year': year ?? now.year,
        'page': page,
        'size': size,
      },
    );

    final rows = (res['transactions'] as List<dynamic>?) ?? const <dynamic>[];
    return rows.map((e) => _toLegacy(e as Map<String, dynamic>)).toList();
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
    AppLog.d(
        '[TransactionRepository] getByUserIdAndDateRange userId=$userId start=${startDate.toIso8601String()} end=${endDate.toIso8601String()} accountIds=$accountIds');
    final res = await _api.get<List<dynamic>>(
      "${ApiConfig.transactions}/date-range",
      query: {
        'startDate': startDate.toIso8601String().split('T').first,
        'endDate': endDate.toIso8601String().split('T').first,
        'expand': true,
        if (accountIds != null && accountIds.isNotEmpty)
          'accountIds': accountIds.join(','),
      },
    );
    AppLog.d(
        '[TransactionRepository] date-range raw response count=${res.length}');
    return res.map((e) {
      final row = e as Map<String, dynamic>;
      if (row['transaction'] is Map<String, dynamic> ||
          row.containsKey('category') ||
          row.containsKey('account') ||
          row.containsKey('splits')) {
        return _toLegacyFromExpanded(row);
      }
      return _toLegacy(row);
    }).toList();
  }

  legacy.Transaction _toLegacyFromExpanded(Map<String, dynamic> json) {
    final txJson = (json['transaction'] as Map<String, dynamic>?) ?? json;
    final t = _toLegacy(txJson);

    final catJson = json['category'] as Map<String, dynamic>?;
    if (catJson != null) {
      t.category = _toLegacyCategory(catJson);
    }

    final accJson = json['account'] as Map<String, dynamic>?;
    if (accJson != null) {
      t.account = _toLegacyAccount(accJson);
    }

    final splitRows = (json['splits'] as List<dynamic>?) ?? const <dynamic>[];
    t.splits = splitRows.map((row) {
      final m = row as Map<String, dynamic>;
      final splitJson = (m['split'] as Map<String, dynamic>?) ?? m;
      final s = _toLegacySplit(splitJson);

      final contactJson = m['contact'] as Map<String, dynamic>?;
      if (contactJson != null) {
        final c = legacy_contact.Contact.fromJson(contactJson);
        s.contact = c;

        final tc = TrakoContact();
        tc.contactId = c.id;
        tc.name = c.name;
        tc.phoneNo = c.phoneNo;
        tc.email = c.email;
        t.contacts.add(tc);
      }
      return s;
    }).toList();

    return t;
  }

  legacy_category.Category _toLegacyCategory(Map<String, dynamic> json) {
    final c = legacy_category.Category();
    c.id = (json['id'] as num?)?.toInt();
    c.name = (json['name'] as String?) ?? '';
    c.categoryType = (json['categoryType'] as String?) ?? 'EXPENSE';
    return c;
  }

  legacy_account.Account _toLegacyAccount(Map<String, dynamic> json) {
    final a = legacy_account.Account();
    a.id = (json['id'] as num?)?.toInt();
    a.name = (json['name'] as String?) ?? '';
    a.currency = (json['currency'] as String?) ?? 'INR';
    return a;
  }

  legacy_split.Split _toLegacySplit(Map<String, dynamic> json) {
    final s = legacy_split.Split();
    s.id = (json['id'] as num?)?.toInt();
    s.transactionId = (json['transactionId'] as num?)?.toInt() ??
        (json['transaction_id'] as num?)?.toInt() ??
        0;
    s.userId =
        int.tryParse((json['userId'] ?? json['user_id'] ?? '0').toString()) ??
            0;
    s.contactId = (json['contactId'] as num?)?.toInt() ??
        (json['contact_id'] as num?)?.toInt();
    s.amount = ((json['amount'] as num?) ?? 0).toDouble();
    s.isSettled = (json['isSettled'] ?? json['is_settled'] ?? 0) as int;
    return s;
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

    // Transfer fields
    t.toAccountId = (json['toAccountId'] as num?)?.toInt();
    t.fromAccountId = (json['fromAccountId'] as num?)?.toInt();
    t.linkedTransactionId = (json['linkedTransactionId'] as num?)?.toInt();

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
      'comments': t.comments, // mapped from description/comments
      'originalCurrency': t.originalCurrency,
      'originalAmount': t.originalAmount,
    };

    // Transfer fields
    if (t.toAccountId != null) payload['toAccountId'] = t.toAccountId;
    if (t.fromAccountId != null) payload['fromAccountId'] = t.fromAccountId;
    if (t.linkedTransactionId != null)
      payload['linkedTransactionId'] = t.linkedTransactionId;

    final hasOriginalCurrency =
        (t.originalCurrency != null && t.originalCurrency!.isNotEmpty);
    final hasOriginalAmount = (t.originalAmount != null);

    // Only include amount for base-currency transactions.
    // IMPORTANT: Transaction.amount is non-nullable in Flutter (defaults to 0.0),
    // so checking for null is not enough. If original currency info is present,
    // omit amount to let backend compute base amount and persist it.
    if (!(hasOriginalCurrency && hasOriginalAmount)) {
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
