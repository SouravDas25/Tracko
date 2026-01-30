import 'package:Tracko/Utils/DatabaseUtil.dart';
import 'package:Tracko/exceptions/AccountIsUsedByTransactionExceptions.dart';
import 'package:Tracko/models/account.dart';
import 'package:Tracko/models/transaction.dart';

class AccountController {
  static Future<List<Account>> getAllAccounts() async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    // TODO: Reimplement with proper DAO pattern after jaguar_orm removal
    // AccountBean accountBean = AccountBean(adapter);
    // return await accountBean.getAll();
    List<Map<String, dynamic>> results = await adapter.rawQuery('SELECT * FROM accounts');
    return results.map((map) {
      Account account = Account();
      account.id = (map['id'] as int?) ?? 0;
      account.name = map['name'] as String? ?? '';
      account.userId = (map['userId'] as int?) ?? 0;
      return account;
    }).toList();
  }

  static deleteAccount(int id) async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    // TODO: Reimplement with proper DAO pattern after jaguar_orm removal
    // AccountBean accountBean = AccountBean(adapter);
    // TransactionBean transactionBean = TransactionBean(adapter);
    // List<Transaction> transactions = await transactionBean.findByCategory(id);
    List<Map<String, dynamic>> transactions = await adapter.rawQuery('SELECT * FROM transactions WHERE accountId = ?', [id]);
    if (transactions.length > 0) {
      throw AccountIsUsedByTransactionExceptions();
    }
    // await accountBean.remove(id);
    await adapter.rawDelete('DELETE FROM accounts WHERE id = ?', [id]);
  }
}
