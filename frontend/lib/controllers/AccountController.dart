import 'package:tracko/exceptions/AccountIsUsedByTransactionExceptions.dart';
import 'package:tracko/models/account.dart';
import 'package:dio/dio.dart';
import 'package:tracko/repositories/account_repository.dart';
import 'package:tracko/di/di.dart';

class AccountController {
  static Future<List<Account>> getAllAccounts() async {
    final repo = sl<AccountRepository>();
    final accounts = await repo.getAllAccounts();
    accounts
        .sort((a, b) => a.name.toLowerCase().compareTo(b.name.toLowerCase()));
    return accounts;
  }

  static deleteAccount(int id) async {
    final repo = sl<AccountRepository>();
    try {
      await repo.deleteAccount(id);
    } on DioException catch (e) {
      // Map backend constraint/business error to legacy exception
      final status = e.response?.statusCode ?? 0;
      if (status == 400 || status == 409) {
        throw AccountIsUsedByTransactionExceptions();
      }
      rethrow;
    }
  }
}
