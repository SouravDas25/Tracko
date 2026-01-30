import 'package:Tracko/Utils/CommonUtil.dart';
import 'package:Tracko/Utils/ConstantUtil.dart';
import 'package:Tracko/Utils/DatabaseUtil.dart';
import 'package:Tracko/Utils/SettingUtil.dart';
import 'package:Tracko/Utils/enums.dart';
import 'package:Tracko/controllers/ChatController.dart';
import 'package:Tracko/controllers/SplitController.dart';
import 'package:Tracko/controllers/UserController.dart';
import 'package:Tracko/dtos/TrackoContact.dart';
import 'package:Tracko/models/account.dart';
import 'package:Tracko/models/category.dart';
import 'package:Tracko/models/split.dart';
import 'package:Tracko/models/transaction.dart';
import 'package:Tracko/models/user.dart';
import 'package:Tracko/services/SessionService.dart';
// import 'package:jaguar_orm/jaguar_orm.dart'; // Removed - migrating to plain sqflite
import 'package:sqflite/sqlite_api.dart' as sqlite;

import 'CategoryController.dart';

class TransactionController {
  static Future<bool> saveTransaction(Transaction transaction) async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    bool isUserCountable = true;
    TransactionBean transactionBean = TransactionBean(adapter);
    transaction.id = await transactionBean.upsert(transaction);

    if (transaction.contacts != null && transaction.contacts.length > 0) {
      await SplitBean(adapter).removeByTransaction(transaction.id ?? 0);
      isUserCountable =
      await TransactionController.saveSplitsInTransaction(transaction);
    }
    transaction.isCountable = isUserCountable ? 1 : 0;
    transaction.id = await transactionBean.upsert(transaction);
    return true;
  }

  static Future<bool> saveSplitsInTransaction(Transaction transaction) async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    bool isUserCountable = false;
    double a = (transaction.amount / transaction.contacts.length);
    User rootUser = SessionService.currentUser();
//    transaction.contacts.removeLast();
//    print(transaction.contacts);
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
//      print(contact.phones);
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
//      user.id = await userBean.upsert(user);
        user.id = await UserController.saveUser(user, isShadow: true);
        await ChatController.createChatGroup(user);
      }
      split.userId = user.id ?? 0;
//      print(transaction);
//      print(user);
      split.id = await SplitBean(adapter).insert(split);
//      print(split);
    }
    return isUserCountable;
  }

  static Future<Set<TrakoContact>> loadSplits(Transaction transaction) async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    UserBean userBean = new UserBean(adapter);
    SplitBean splitBean = SplitBean(adapter);
    List<Split> splits = await splitBean.findByTransaction(transaction.id);
//    print(splits);
    transaction.splits = splits;
    transaction.contacts = Set<TrakoContact>();
    for (Split split in splits) {
      User? user = await userBean.find(split.userId);
      if (user == null) continue;
      TrakoContact contact = UserController.user2Contact(user);
      transaction.contacts.add(contact);
    }
//    TrakoContact userContact = SessionService.currentUserContact();
//    transaction.contacts.add(userContact);
    return transaction.contacts;
  }

  static Future<double> getTotalBetween(DateTime begin, DateTime end,
      [int? accountId]) async {
    sqlite.Database db = await DatabaseUtil.getRawDatabase();
    String query = "SELECT SUM( "
        "CASE transaction_type "
        "WHEN ${TransactionType.DEBIT} THEN amount*-1 "
        "WHEN ${TransactionType.CREDIT} THEN amount "
        "END "
        ") / CASE "
        "WHEN split_count > 0 THEN split_count "
        "ELSE 1 "
        "END "
        " AS amount , split_count FROM transactions t "
        "LEFT JOIN (SELECT COUNT(*) AS split_count , transaction_id  "
        "FROM splits GROUP BY transaction_id) s "
        "on t.id = s.transaction_id "
        "WHERE date >= Datetime('$begin') AND "
        "date < Datetime('$end') AND "
        "is_countable = 1 ";
    if (accountId != null) {
      query += " WHERE account_id = $accountId ";
    }
    var tmp = (await db.rawQuery(query)).toList();
    if (tmp.first == null) return 0;
    if (tmp.first['amount'] == null) return 0;
//    print(tmp);
    return (tmp.first['amount'] as num?)?.toDouble() ?? 0.0;
  }

  static Future<double> getPreviousMonthTotal([int? accountId]) async {
    // SELECT SUM(amount) AS amount FROM transactions WHERE transaction_type = 'DEBIT'
    // SELECT SUM(amount) AS amount FROM transactions WHERE transaction_type = 'CREDIT'
    DateTime month = SettingUtil.previousMonth;
    DateTime nextMonth = SettingUtil.currentMonth;
    return await getTotalBetween(month, nextMonth);
  }

  static Future<double> getCurrentMonthTotal([int? accountId]) async {
//    sqlite.Database db = await DatabaseUtil.getRawDatabase();
    // SELECT SUM(amount) AS amount FROM transactions WHERE transaction_type = 'DEBIT'
    // SELECT SUM(amount) AS amount FROM transactions WHERE transaction_type = 'CREDIT'
    DateTime month = SettingUtil.currentMonth;
    DateTime nextMonth = SettingUtil.nextMonth;
    return await getTotalBetween(month, nextMonth);
  }

  static Future<TransactionBean> _transactionBean() async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    TransactionBean transactionBean = new TransactionBean(adapter);
    return transactionBean;
  }

  static Future<dynamic> _basicTransactionFinder() async {
    TransactionBean transactionBean = await _transactionBean();

    var finder = transactionBean.finder;
    DateTime month = SettingUtil.currentMonth;
    DateTime nextMonth = SettingUtil.nextMonth;

    finder
//        .where(transactionBean.isCountable.eq(1))
        .where(transactionBean.date.gtEq(month))
        .where(transactionBean.date.lt(nextMonth));

    return finder;
  }

  static void _addAccountsFilter(List<int>? accountIds,
      TransactionBean transactionBean, dynamic finder) {
    // TODO: Implement account filtering when needed
    // if (accountIds != null && accountIds.length > 0) {
    //   var expression = transactionBean.accountId.eq(accountIds[0]);
    //   for (int i = 1; i < accountIds.length; i++) {
    //     expression = expression.or(transactionBean.accountId.eq(accountIds[i]));
    //   }
    //   finder = finder.and(expression);
    // }
  }

  static Future<double> getCurrentMonthIncome({List<int>? accountIds}) async {
    TransactionBean transactionBean = await _transactionBean();
    var finder = await _basicTransactionFinder();

    _addAccountsFilter(accountIds, transactionBean, finder);

    finder.and(transactionBean.transactionType.eq(TransactionType.CREDIT));

    List<Transaction> transactions = await transactionBean.findMany(finder);
    double sum = 0;
    transactions.forEach((Transaction transaction) {
      sum += transaction.amount;
    });

    return sum;
  }

  static Future<double> getCurrentMonthExpense({List<int>? accountIds}) async {
    TransactionBean transactionBean = await _transactionBean();
    var finder = await _basicTransactionFinder();

    _addAccountsFilter(accountIds, transactionBean, finder);

    finder.and(transactionBean.transactionType.eq(TransactionType.DEBIT));

    List<Transaction> transactions = await transactionBean.findMany(finder);
    double sum = 0;
    transactions.forEach((Transaction transaction) {
      sum += transaction.amount;
    });

    return sum;
  }

  static Future<int> totalTransactionCount({List<int>? accountIds}) async {
    TransactionBean transactionBean = await _transactionBean();
    var finder = await _basicTransactionFinder();

    _addAccountsFilter(accountIds, transactionBean, finder);

    List<Transaction> transactions = await transactionBean.findMany(finder);
    return transactions.length;
  }

  static Future<List<Transaction>> getTransaction(int pageNumber,
      {List<int>? accountIds}) async {
    TransactionBean transactionBean = await _transactionBean();
//    CategoryBean categoryBean = new CategoryBean(adapter);
    var finder = await _basicTransactionFinder();

    pageNumber = pageNumber - 1;
    if (pageNumber < 0) pageNumber = 0;

    _addAccountsFilter(accountIds, transactionBean, finder);

    finder
        .offset(pageNumber * ConstantUtil.NO_OF_RECORDS_PER_PAGE)
        .limit(ConstantUtil.NO_OF_RECORDS_PER_PAGE);

    finder.orderBy(transactionBean.date.name);

    List<Transaction> transactions = await transactionBean.findMany(finder);
    for (Transaction transaction in transactions) {
      await _preloadTransactions(transaction);
    }
//    transactions.sort((a, b) => b.date.compareTo(a.date));
//    print(transactions);
    return transactions;
  }

  static _preloadTransactions(Transaction transaction) async {
    var adapter = await DatabaseUtil.getAdapter();
    CategoryBean categoryBean = new CategoryBean(adapter);
    transaction.category = await categoryBean.find(transaction.categoryId);
    await loadSplits(transaction);
  }

  static Future<List<Transaction>> getRecentTransaction() async {
    var adapter = await DatabaseUtil.getAdapter();
    TransactionBean transactionBean = new TransactionBean(adapter);

    var query =
    transactionBean.finder.limit(5).orderBy(transactionBean.date.name);
    DateTime month = SettingUtil.currentMonth;
    DateTime nextMonth = SettingUtil.nextMonth;
    query = query
        .where(transactionBean.date.gtEq(month))
        .where(transactionBean.date.lt(nextMonth));

    List<Transaction> transactions = await transactionBean.findMany(query);
    for (Transaction transaction in transactions) {
      await _preloadTransactions(transaction);
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
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    TransactionBean transactionBean = new TransactionBean(adapter);
    await SplitController.removeByTransactionId(transactionId);
    transactionId = await transactionBean.remove(transactionId);
    return transactionId;
  }

  static void clear() async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    TransactionBean transactionBean = new TransactionBean(adapter);
    await transactionBean.removeAll();
  }
}
