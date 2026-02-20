import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/ConstantUtil.dart';
import 'package:tracko/Utils/SettingUtil.dart';
import 'package:tracko/Utils/enums.dart';
import 'package:tracko/Utils/AppLog.dart';
import 'package:tracko/controllers/SplitController.dart';
import 'package:tracko/controllers/UserController.dart';
import 'package:tracko/dtos/TrackoContact.dart';
import 'package:tracko/models/contact.dart' as backend;
import 'package:tracko/models/account.dart';
import 'package:tracko/models/category.dart';
import 'package:tracko/models/split.dart';
import 'package:tracko/models/transaction.dart';
import 'package:tracko/models/user.dart';
import 'package:tracko/services/SessionService.dart';
import 'package:tracko/repositories/transaction_repository.dart';
import 'package:tracko/repositories/split_repository.dart';
import 'package:tracko/repositories/contact_repository.dart';
import 'package:tracko/models/transaction_period_summary.dart';

import 'CategoryController.dart';

class TransactionController {
  static String? _pickUserId(User user) {
    final gid = user.globalId.trim();
    if (gid.isNotEmpty) return gid;

    final id = user.id;
    if (id != null) return id.toString();

    return null;
  }

  static Future<String?> _resolveUserId() async {
    try {
      final user = SessionService.currentUser();
      final id = _pickUserId(user);
      if (id != null && id.isNotEmpty) return id;
    } catch (_) {
      // Fall through to backend user lookup
    }

    try {
      final user = await SessionService.getCurrentUser();
      final id = _pickUserId(user);
      if (id != null && id.isNotEmpty) return id;
    } catch (_) {
      // ignore and return null
    }

    AppLog.d(
        '[TransactionController] _resolveUserId failed to resolve user id');
    return null;
  }

  static Future<bool> saveTransaction(Transaction transaction) async {
    final txRepo = TransactionRepository();
    final splitRepo = SplitRepository();
    bool isUserCountable = true;

    if (transaction.transactionType == TransactionType.TRANSFER) {
      final fromId = transaction.fromAccountId;
      final toId = transaction.toAccountId;
      if (fromId == null || toId == null) {
        throw Exception(
            "From Account and To Account must be specified for transfer");
      }
      if (fromId == toId) {
        throw Exception("From Account and To Account cannot be same");
      }

      if (transaction.id == null || transaction.id == 0) {
        await txRepo.create(transaction);
      } else {
        await txRepo.update(transaction.id!, transaction);
      }
      return true;
    }

    // Create or update transaction
    Transaction saved;
    if (transaction.id == null || transaction.id == 0) {
      saved = await txRepo.create(transaction);
    } else {
      saved = await txRepo.update(transaction.id!, transaction);
    }
    transaction.id = saved.id;
    transaction.amount = saved.amount;
    transaction.exchangeRate = saved.exchangeRate;
    transaction.originalAmount = saved.originalAmount;
    transaction.originalCurrency = saved.originalCurrency;

    // Handle splits: Always clean up existing splits to ensure sync with UI
    if (transaction.id != null && transaction.id! > 0) {
      final existingSplits =
          await splitRepo.getByTransactionId(transaction.id!);
      for (var split in existingSplits) {
        if (split.id != null) {
          await splitRepo.delete(split.id!);
        }
      }
    }

    // Create new splits if contacts exist
    if (transaction.contacts != null && transaction.contacts.length > 0) {
      isUserCountable =
          await TransactionController.saveSplitsInTransaction(transaction);
    }

    transaction.amount = saved.amount;
    // Update transaction with countable status
    transaction.isCountable = isUserCountable ? 1 : 0;
    saved = await txRepo.update(transaction.id!, transaction);
    transaction.id = saved.id;
    return true;
  }

  static Future<bool> saveSplitsInTransaction(Transaction transaction) async {
    final splitRepo = SplitRepository();
    User rootUser = SessionService.currentUser();

    final participants = transaction.contacts
        .where((c) => c.contactId != null)
        .toList(growable: false);

    if (participants.isEmpty) {
      return true;
    }

    final totalPeople = participants.length + 1; // + you
    final share = transaction.amount / totalPeople;

    for (int i = 0; i < participants.length; i++) {
      TrakoContact contact = participants.elementAt(i);
      Split split = new Split();
      split.amount = share;
      split.transactionId = transaction.id ?? 0;
      split.isSettled = 0;
      split.contactId = contact.contactId;

      // Create split via backend API
      Split created = await splitRepo.create(split);
      split.id = created.id;
    }
    return true;
  }

  static Future<Set<TrakoContact>> loadSplits(Transaction transaction) async {
    final splitRepo = SplitRepository();
    final contactRepo = ContactRepository();
    final txId = transaction.id ?? 0;

    if (txId == 0) {
      transaction.splits = [];
      transaction.contacts = {SessionService.currentUserContact()};
      return transaction.contacts;
    }

    final splits = await splitRepo.getByTransactionId(txId);
    transaction.splits = splits;

    final contactsById = <int, backend.Contact>{};
    try {
      final all = await contactRepo.listMine();
      for (final c in all) {
        if (c.id != null) contactsById[c.id!] = c;
      }
    } catch (e) {
      // ignore
    }

    transaction.contacts = Set<TrakoContact>();
    // Always include current user
    transaction.contacts.add(SessionService.currentUserContact());

    for (final s in splits) {
      final cid = s.contactId;
      if (cid == null) continue;
      final c = contactsById[cid];
      if (c == null) continue;
      final tc = TrakoContact();
      tc.contactId = cid;
      tc.name = c.name;
      tc.phoneNo = c.phoneNo;
      tc.email = c.email;
      transaction.contacts.add(tc);
    }

    return transaction.contacts;
  }

  static Future<double> getTotalBetween(DateTime begin, DateTime end,
      [int? accountId]) async {
    final txRepo = TransactionRepository();
    final user = SessionService.currentUser();
    final userId = (user.id ?? '').toString();

    final summary = accountId == null
        ? await txRepo.getSummary(
            userId,
            begin,
            end,
          )
        : await txRepo.getAccountSummary(
            accountId,
            begin,
            end,
            includeRollover: false,
          );
    return (summary['netTotal'] as num?)?.toDouble() ?? 0.0;
  }

  static Future<double> getPreviousMonthTotal([int? accountId]) async {
    // SELECT SUM(amount) AS amount FROM transactions WHERE transaction_type = 'DEBIT'
    // SELECT SUM(amount) AS amount FROM transactions WHERE transaction_type = 'CREDIT'
    DateTime month = SettingUtil.previousMonth;
    DateTime nextMonth = SettingUtil.currentMonth;
    return await getTotalBetween(month, nextMonth);
  }

  static Future<double> getCurrentMonthTotal([int? accountId]) async {
    // SELECT SUM(amount) AS amount FROM transactions WHERE transaction_type = 'DEBIT'
    // SELECT SUM(amount) AS amount FROM transactions WHERE transaction_type = 'CREDIT'
    DateTime month = SettingUtil.currentMonth;
    DateTime nextMonth = SettingUtil.nextMonth;
    return await getTotalBetween(month, nextMonth);
  }

  static Future<double> getCurrentMonthIncome({List<int>? accountIds}) async {
    final summary = await getSummaryBetween(
      SettingUtil.currentMonth,
      SettingUtil.nextMonth,
      accountIds: accountIds,
    );
    return (summary['totalIncome'] as num?)?.toDouble() ?? 0.0;
  }

  static Future<double> getCurrentMonthExpense({List<int>? accountIds}) async {
    final summary = await getSummaryBetween(
      SettingUtil.currentMonth,
      SettingUtil.nextMonth,
      accountIds: accountIds,
    );
    return (summary['totalExpense'] as num?)?.toDouble() ?? 0.0;
  }

  static Future<Map<String, dynamic>> getSummaryBetween(
    DateTime begin,
    DateTime end, {
    List<int>? accountIds,
  }) async {
    final txRepo = TransactionRepository();
    final userId = await _resolveUserId();
    if (userId == null) {
      return <String, dynamic>{};
    }

    if (accountIds != null && accountIds.length == 1) {
      return await txRepo.getAccountSummary(
        accountIds.first,
        begin,
        end,
        includeRollover: true,
      );
    }

    return await txRepo.getSummary(userId, begin, end, accountIds: accountIds);
  }

  static Future<int> totalTransactionCount({List<int>? accountIds}) async {
    final txRepo = TransactionRepository();
    final userId = await _resolveUserId();
    if (userId == null) {
      return 0;
    }
    DateTime month = SettingUtil.currentMonth;
    DateTime nextMonth = SettingUtil.nextMonth;

    final summary = await txRepo.getSummary(
      userId,
      month,
      nextMonth,
      accountIds: accountIds,
    );
    return (summary['transactionCount'] as num?)?.toInt() ?? 0;
  }

  static Future<List<Transaction>> getTransactionsForSelectedMonth(
      {List<int>? accountIds, DateTime? month}) async {
    final txRepo = TransactionRepository();
    final userId = await _resolveUserId();
    AppLog.d(
        '[TransactionController] getTransactionsForSelectedMonth userId=$userId accountIds=$accountIds month=$month');
    if (userId == null) {
      AppLog.d(
          '[TransactionController] getTransactionsForSelectedMonth early return: userId is null');
      return <Transaction>[];
    }

    final DateTime start = month ?? SettingUtil.currentMonth;
    final DateTime end = DateTime.utc(start.year, start.month + 1);

    List<Transaction> transactions = await txRepo.getByUserIdAndDateRange(
      userId,
      startDate: start,
      endDate: end,
      accountIds: accountIds,
    );
    AppLog.d(
        '[TransactionController] date-range returned count=${transactions.length} start=$start end=$end');

    if (accountIds != null && accountIds.isNotEmpty) {
      final allowed = accountIds.toSet();
      transactions =
          transactions.where((t) => allowed.contains(t.accountId)).toList();
    }

    transactions.sort((a, b) => b.date.compareTo(a.date));

    return transactions;
  }

  static Future<List<Transaction>> getTransactionsForSelectedMonthPaginated(
      {List<int>? accountIds,
      DateTime? month,
      int page = 0,
      int size = 20}) async {
    final txRepo = TransactionRepository();
    final userId = await _resolveUserId();
    if (userId == null) {
      return <Transaction>[];
    }

    final DateTime start = month ?? SettingUtil.currentMonth;

    List<Transaction> transactions = await txRepo.getAll(
      month: start.month,
      year: start.year,
      accountIds: accountIds,
      page: page,
      size: size,
      expand: true,
    );

    return transactions;
  }

  static Future<double> getMonthIncome(DateTime month,
      {List<int>? accountIds}) async {
    final nextMonth = DateTime.utc(month.year, month.month + 1);
    final summary = await getSummaryBetween(
      month,
      nextMonth,
      accountIds: accountIds,
    );
    return (summary['totalIncome'] as num?)?.toDouble() ?? 0.0;
  }

  static Future<double> getMonthExpense(DateTime month,
      {List<int>? accountIds}) async {
    final nextMonth = DateTime.utc(month.year, month.month + 1);
    final summary = await getSummaryBetween(
      month,
      nextMonth,
      accountIds: accountIds,
    );
    return (summary['totalExpense'] as num?)?.toDouble() ?? 0.0;
  }

  static Future<List<TransactionPeriodSummary>> getMonthlySummaries(int year,
      {List<int>? accountIds}) async {
    final txRepo = TransactionRepository();
    return await txRepo.getMonthlySummaries(year, accountIds: accountIds);
  }

  static Future<List<TransactionPeriodSummary>> getYearlySummaries(
      {List<int>? accountIds}) async {
    final txRepo = TransactionRepository();
    return await txRepo.getYearlySummaries(accountIds: accountIds);
  }

  static _preloadTransactions(Transaction transaction) async {
    // Load category via CategoryController (already migrated to backend)
    transaction.category =
        await CategoryController.findById(transaction.categoryId);
    await loadSplits(transaction);
  }

  static Future<List<Transaction>> getRecentTransaction() async {
    final txRepo = TransactionRepository();
    DateTime month = SettingUtil.currentMonth;
    final userId = await _resolveUserId();
    if (userId == null) {
      return <Transaction>[];
    }

    List<Transaction> transactions = await txRepo.getAll(
      month: month.month,
      year: month.year,
      page: 0,
      size: 5,
      expand: true,
    );

    return transactions;
  }

  static Future<Transaction> fromJson(dynamic jsonResponse) async {
    bool valid = jsonResponse['valid'];
    if (!valid) return Transaction();
    Transaction transaction = new Transaction();
    transaction.amount = jsonResponse['amounts'][0];
    transaction.comments = jsonResponse['request']['text'];
    transaction.date = DateTime.parse(jsonResponse['request']['date']);
    if (jsonResponse['dates'] != null) {
      transaction.date = DateTime.parse(jsonResponse['dates'][0].toString());
    }
    transaction.transactionType = TransactionType.inttify(jsonResponse['type']);

    transaction.name = "Item";
    transaction.accountId = Account.defaultAccountId();
    if (jsonResponse['entity'] != null && jsonResponse['entity'].length > 0) {
      dynamic entity = jsonResponse['entity'][0];
      transaction.name = entity['name'];
      transaction.category =
          await CategoryController.findOrCreateByName(entity['category']);
      transaction.categoryId = transaction.category?.id ?? 0;
//      transaction.logo = entity['logo'];
    } else {
      transaction.categoryId = 0;
      transaction.category = null;
    }
    transaction.contacts = Set();
    TransactionController._preloadTransactions(transaction);
//    if (transaction.logo == null && transaction.category != null) {
//      transaction.logo =
//          CommonUtil.categoryName2ImageUrl(transaction.category.name);
//    } else {
//      Category category = await CategoryController.getDefaultCategory();
//      transaction.logo = CommonUtil.categoryName2ImageUrl(category.name);
//    }

    return transaction;
  }

  static Future<Transaction> findById(int id) async {
    final txRepo = TransactionRepository();
    return await txRepo.getById(id);
  }

  static Future<int> deleteById(int transactionId) async {
    // Use backend repositories
    final splitRepo = SplitRepository();
    final txRepo = TransactionRepository();
    // Remove splits first to mirror previous logic
    final splits = await splitRepo.getByTransactionId(transactionId);
    for (final s in splits) {
      if (s.id != null) {
        await splitRepo.delete(s.id!);
      }
    }
    await txRepo.deleteById(transactionId);
    return transactionId;
  }

  static void clear() async {
    // Backend route doesn't expose bulk delete; perform best-effort by fetching and deleting
    final txRepo = TransactionRepository();
    final userId = await _resolveUserId();
    if (userId == null) {
      return;
    }

    final all = await txRepo.getByUserId(userId);
    for (final t in all) {
      if (t.id != null) {
        await txRepo.deleteById(t.id!);
      }
    }
  }
}
