import 'package:tracko/Utils/SettingUtil.dart';
import 'package:tracko/models/split.dart';
import 'package:tracko/models/contact.dart';
import 'package:tracko/repositories/split_repository.dart';
import 'package:tracko/repositories/contact_repository.dart';
import 'package:tracko/repositories/transaction_repository.dart';
import 'package:tracko/controllers/CategoryController.dart';
import 'package:tracko/di/di.dart';

class SplitController {
  static String? _normalizeUserId(String userId) {
    final id = userId.trim();
    if (id.isEmpty || id == '0') return null;
    return id;
  }

  static Future<double> getDueAmount(int userId) async {
    return getDueAmountByUserId(userId.toString());
  }

  static Future<double> getDueAmountByUserId(String userId) async {
    final splitRepo = sl<SplitRepository>();
    final uid = _normalizeUserId(userId);
    if (uid == null) return 0.0;
    List<Split> splits = await splitRepo.getByUserId(uid);

    // Filter unsettled splits
    splits = splits.where((s) => s.isSettled == 0).toList();

    double amount =
        splits.fold(0.0, (value, element) => value + element.amount);
    return amount;
  }

  static Future<List<Split>> findByUserId(int userId,
      {bool preload = true}) async {
    return findByUserIdKey(userId.toString(), preload: preload);
  }

  static Future<List<Split>> findByUserIdKey(String userId,
      {bool preload = true}) async {
    final splitRepo = sl<SplitRepository>();
    final contactRepo = sl<ContactRepository>();
    final txRepo = sl<TransactionRepository>();
    final userIdStr = _normalizeUserId(userId);
    if (userIdStr == null) return <Split>[];

    DateTime month = SettingUtil.currentMonth;
    DateTime nextMonth = SettingUtil.nextMonth;

    // Get all splits for user
    List<Split> splits = await splitRepo.getByUserId(userIdStr);

    // Preload contacts once (used to show who a split is with)
    final Map<int, Contact> contactsById = {};
    try {
      final contacts = await contactRepo.listMine();
      for (final c in contacts) {
        if (c.id != null) contactsById[c.id!] = c;
      }
    } catch (e) {
      // Ignore contact load failures; splits will still show without contact info.
    }

    List<Split> returningSplit = [];
    for (Split split in splits) {
      if (split.contactId != null) {
        split.contact = contactsById[split.contactId!];
      }
      // Get transaction for this split
      if (split.transactionId == 0) continue;

      try {
        split.transaction = await txRepo.getById(split.transactionId);
      } catch (e) {
        continue; // Skip if transaction not found
      }

      if (split.transaction == null) continue;

      // Filter: include if transaction in current month OR split is unsettled OR settled this month
      bool inCurrentMonth = split.transaction!.date.isAfter(month) &&
          split.transaction!.date.isBefore(nextMonth);
      bool isUnsettled = split.isSettled == 0;
      bool settledThisMonth =
          split.settledAt.isAfter(month) && split.settledAt.isBefore(nextMonth);

      if (inCurrentMonth || isUnsettled || settledThisMonth) {
        // Preload category if requested
        if (preload && split.transaction != null) {
          split.transaction!.category =
              await CategoryController.findById(split.transaction!.categoryId);
        }
        returningSplit.add(split);
      }
    }

    // Sort by transaction date
    returningSplit.sort((a, b) => (a.transaction?.date ?? DateTime.now())
        .compareTo(b.transaction?.date ?? DateTime.now()));

    return returningSplit;
  }

  static Future<int> settleAll(int userId) async {
    return settleAllByUserId(userId.toString());
  }

  static Future<int> settleAllByUserId(String userId) async {
    final splitRepo = sl<SplitRepository>();
    final uid = _normalizeUserId(userId);
    if (uid == null) return 0;
    List<Split> splitList = await splitRepo.getByUserId(uid);

    for (Split split in splitList) {
      await settleSplit(split);
    }
    return splitList.length;
  }

  static Future<int> settleSplit(Split split, {int? settleTo}) async {
    final splitRepo = sl<SplitRepository>();
    if (split.id == null) return 0;

    // Use settle endpoint if available, otherwise update manually
    try {
      await splitRepo.settle(split.id!);
      return 1;
    } catch (e) {
      // Fallback: manual update
      if (settleTo != null) {
        split.isSettled = settleTo;
      } else {
        split.isSettled = split.isSettled == 1 ? 0 : 1;
      }
      if (split.isSettled == 1) split.settledAt = DateTime.now();
      await splitRepo.update(split.id!, split);
      return split.isSettled;
    }
  }

  static removeByTransactionId(int transactionId) async {
    final splitRepo = sl<SplitRepository>();
    List<Split> splits = await splitRepo.getByTransactionId(transactionId);

    for (Split split in splits) {
      if (split.id != null) {
        await splitRepo.delete(split.id!);
      }
    }
  }
}
