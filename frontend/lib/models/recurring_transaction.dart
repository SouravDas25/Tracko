import 'package:tracko/Utils/enums.dart';

enum Frequency {
  DAILY,
  WEEKLY,
  MONTHLY,
  YEARLY;

  String toJson() => name;
  static Frequency fromJson(String json) => values.byName(json);
}

class RecurringTransaction {
  int? id;
  String userId = '';
  String name = '';
  double? amount;
  int accountId = 0;
  int categoryId = 0;
  int? toAccountId;
  int transactionType = TransactionType.DEBIT;
  Frequency frequency = Frequency.MONTHLY;
  DateTime startDate = DateTime.now();
  DateTime nextRunDate = DateTime.now();
  DateTime? endDate;
  bool isActive = true;
  DateTime? lastRunDate;
  DateTime? createdAt;
  DateTime? updatedAt;

  // Currency Support
  String? originalCurrency;
  double? originalAmount;
  double? exchangeRate;

  RecurringTransaction();

  factory RecurringTransaction.fromJson(Map<String, dynamic> json) {
    final rt = RecurringTransaction();
    rt.id = json['id'];
    rt.userId = json['userId'] ?? '';
    rt.name = json['name'] ?? '';
    rt.amount = (json['amount'] as num?)?.toDouble();
    rt.accountId = json['accountId'] ?? 0;
    rt.categoryId = json['categoryId'] ?? 0;
    rt.toAccountId = json['toAccountId'];
    rt.transactionType = json['transactionType'] ?? TransactionType.DEBIT;
    rt.frequency = Frequency.values.firstWhere(
        (e) => e.name == (json['frequency'] as String),
        orElse: () => Frequency.MONTHLY);
    rt.startDate = DateTime.parse(json['startDate']);
    rt.nextRunDate = DateTime.parse(json['nextRunDate']);
    rt.endDate =
        json['endDate'] != null ? DateTime.parse(json['endDate']) : null;
    rt.isActive = json['isActive'] ?? true;
    rt.lastRunDate = json['lastRunDate'] != null
        ? DateTime.parse(json['lastRunDate'])
        : null;
    rt.createdAt =
        json['createdAt'] != null ? DateTime.parse(json['createdAt']) : null;
    rt.updatedAt =
        json['updatedAt'] != null ? DateTime.parse(json['updatedAt']) : null;

    rt.originalCurrency = json['originalCurrency'];
    rt.originalAmount = (json['originalAmount'] as num?)?.toDouble();
    rt.exchangeRate = (json['exchangeRate'] as num?)?.toDouble();

    return rt;
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'userId': userId,
      'name': name,
      'amount': amount,
      'accountId': accountId,
      'categoryId': categoryId,
      'toAccountId': toAccountId,
      'transactionType': transactionType,
      'frequency': frequency.name,
      'startDate': startDate.toIso8601String(),
      'nextRunDate': nextRunDate.toIso8601String(),
      'endDate': endDate?.toIso8601String(),
      'isActive': isActive,
      'lastRunDate': lastRunDate?.toIso8601String(),
      'originalCurrency': originalCurrency,
      'originalAmount': originalAmount,
      'exchangeRate': exchangeRate,
    };
  }
}
