import 'package:tracko/Utils/CommonUtil.dart';
import 'package:tracko/Utils/ConstantUtil.dart';
import 'package:tracko/Utils/SettingUtil.dart';
import 'package:tracko/Utils/enums.dart';
import 'package:tracko/controllers/ChatController.dart';
import 'package:tracko/controllers/SplitController.dart';
import 'package:tracko/controllers/UserController.dart';
import 'package:tracko/dtos/TrackoContact.dart';
import 'package:tracko/models/account.dart';
import 'package:tracko/models/category.dart';
import 'package:tracko/models/split.dart';
import 'package:tracko/models/transaction.dart';
import 'package:tracko/models/user.dart';
import 'package:tracko/services/SessionService.dart';
import 'package:tracko/repositories/transaction_repository.dart';
import 'package:tracko/repositories/split_repository.dart';
// import 'package:jaguar_orm/jaguar_orm.dart'; // Removed - migrating to plain sqflite

import 'CategoryController.dart';

class TransactionController {
  static Future<bool> saveTransaction(Transaction transaction) async {
    final txRepo = TransactionRepository();
    final splitRepo = SplitRepository();
    bool isUserCountable = true;
    
    // Create or update transaction
    Transaction saved;
    if (transaction.id == null || transaction.id == 0) {
      saved = await txRepo.create(transaction);
    } else {
      saved = await txRepo.update(transaction.id!, transaction);
    }
    transaction.id = saved.id;

    // Handle splits if contacts exist
    if (transaction.contacts != null && transaction.contacts.length > 0) {
      // Delete existing splits for this transaction
      final existingSplits = await splitRepo.getByTransactionId(transaction.id!);
      for (var split in existingSplits) {
        if (split.id != null) {
          await splitRepo.delete(split.id!);
        }
      }
      
      // Create new splits
      isUserCountable = await TransactionController.saveSplitsInTransaction(transaction);
    }
    
    // Update transaction with countable status
    transaction.isCountable = isUserCountable ? 1 : 0;
    saved = await txRepo.update(transaction.id!, transaction);
    transaction.id = saved.id;
    return true;
  }

  static Future<bool> saveSplitsInTransaction(Transaction transaction) async {
    final splitRepo = SplitRepository();
    bool isUserCountable = false;
    double a = (transaction.amount / transaction.contacts.length);
    User rootUser = SessionService.currentUser();

    for (int i = 0; i < transaction.contacts.length; i++) {
      TrakoContact contact = transaction.contacts.elementAt(i);
      print(contact.phoneNo + " " + rootUser.phoneNo);
      if (contact.phoneNo == null) {
        throw Exception("No Phone Number Found in Contact Info.");
      }
      Split split = new Split();
      split.amount = a;
      split.transactionId = transaction.id ?? 0;
      split.isSettled = 0;

      User user;

      if (rootUser.phoneNo.contains(contact.phoneNo)) {
        isUserCountable = true;
        user = rootUser;
      } else {
        String phoneNumber = contact.phoneNo;
        phoneNumber = CommonUtil.extractPhoneNumber(phoneNumber);
        user = await UserController.findByPhoneNumber(phoneNumber);
        if (user == null) {
          user = new User();
        }
        user.phoneNo = phoneNumber;
        user.name = contact.name;
        user.email = contact.email != null ? contact.email : "";
        user.profilePic = "";
        user.id = await UserController.saveUser(user, isShadow: true);
        await ChatController.createChatGroup(user);
      }
      split.userId = user.id ?? 0;
      
      // Create split via backend API
      Split created = await splitRepo.create(split);
      split.id = created.id;
    }
    return isUserCountable;
  }

  static Future<Set<TrakoContact>> loadSplits(Transaction transaction) async {
    final splitRepo = SplitRepository();
    final txId = transaction.id ?? 0;
    final splits = await splitRepo.getByTransactionId(txId);
    transaction.splits = splits;
    transaction.contacts = transaction.contacts ?? Set<TrakoContact>();
    return transaction.contacts;
  }

  static Future<double> getTotalBetween(DateTime begin, DateTime end,
      [int? accountId]) async {
    final txRepo = TransactionRepository();
    final user = SessionService.currentUser();
    final userId = (user.id ?? '').toString();
    
    final summary = await txRepo.getSummary(userId, begin, end);
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
    final txRepo = TransactionRepository();
    final user = SessionService.currentUser();
    final userId = (user.id ?? '').toString();
    DateTime month = SettingUtil.currentMonth;
    DateTime nextMonth = SettingUtil.nextMonth;
    
    return await txRepo.getTotalIncome(userId, month, nextMonth);
  }

  static Future<double> getCurrentMonthExpense({List<int>? accountIds}) async {
    final txRepo = TransactionRepository();
    final user = SessionService.currentUser();
    final userId = (user.id ?? '').toString();
    DateTime month = SettingUtil.currentMonth;
    DateTime nextMonth = SettingUtil.nextMonth;
    
    return await txRepo.getTotalExpense(userId, month, nextMonth);
  }

  static Future<int> totalTransactionCount({List<int>? accountIds}) async {
    final txRepo = TransactionRepository();
    final user = SessionService.currentUser();
    final userId = (user.id ?? '').toString();
    DateTime month = SettingUtil.currentMonth;
    DateTime nextMonth = SettingUtil.nextMonth;
    
    final summary = await txRepo.getSummary(userId, month, nextMonth);
    return (summary['transactionCount'] as num?)?.toInt() ?? 0;
  }

  static Future<List<Transaction>> getTransaction(int pageNumber,
      {List<int>? accountIds}) async {
    final txRepo = TransactionRepository();
    final user = SessionService.currentUser();
    final userId = (user.id ?? '').toString();
    DateTime month = SettingUtil.currentMonth;
    DateTime nextMonth = SettingUtil.nextMonth;

    pageNumber = pageNumber - 1;
    if (pageNumber < 0) pageNumber = 0;

    // Get transactions for current month
    List<Transaction> transactions = await txRepo.getByUserIdAndDateRange(
      userId,
      startDate: month,
      endDate: nextMonth,
    );

    // Sort by date descending
    transactions.sort((a, b) => b.date.compareTo(a.date));

    // Apply pagination
    int startIndex = pageNumber * ConstantUtil.NO_OF_RECORDS_PER_PAGE;
    int endIndex = startIndex + ConstantUtil.NO_OF_RECORDS_PER_PAGE;
    if (startIndex >= transactions.length) return [];
    if (endIndex > transactions.length) endIndex = transactions.length;
    transactions = transactions.sublist(startIndex, endIndex);

    // Preload category and splits
    for (Transaction transaction in transactions) {
      await _preloadTransactions(transaction);
    }
    return transactions;
  }

  static _preloadTransactions(Transaction transaction) async {
    // Load category via CategoryController (already migrated to backend)
    transaction.category = await CategoryController.findById(transaction.categoryId);
    await loadSplits(transaction);
  }

  static Future<List<Transaction>> getRecentTransaction() async {
    final txRepo = TransactionRepository();
    DateTime month = SettingUtil.currentMonth;
    DateTime nextMonth = SettingUtil.nextMonth;
    final user = SessionService.currentUser();
    final userId = (user.id ?? '').toString();

    List<Transaction> transactions = await txRepo.getByUserIdAndDateRange(
      userId,
      startDate: month,
      endDate: nextMonth,
    );

    // Sort by date desc and take top 5
    transactions.sort((a, b) => b.date.compareTo(a.date));
    if (transactions.length > 5) {
      transactions = transactions.sublist(0, 5);
    }

    // Preload splits using backend (skip contacts for now)
    for (final t in transactions) {
      await loadSplits(t);
    }
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
      transaction.categoryId = transaction.category?.id ?? 1;
//      transaction.logo = entity['logo'];
    } else {
      transaction.categoryId = Category.defaultCategoryId();
      transaction.category = await CategoryController.getDefaultCategory();
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
    final all = await txRepo.getAll();
    for (final t in all) {
      if (t.id != null) {
        await txRepo.deleteById(t.id!);
      }
    }
  }
}
