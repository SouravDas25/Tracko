import 'package:tracko/exceptions/AccountIsUsedByTransactionExceptions.dart';
import 'package:tracko/models/account.dart';
import 'package:dio/dio.dart';
import 'package:tracko/repositories/account_repository.dart';

class AccountController {
  static Future<List<Account>> getAllAccounts() async {
    final repo = AccountRepository();
    return await repo.getAllAccounts();
  }

  static deleteAccount(int id) async {
    final repo = AccountRepository();
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
