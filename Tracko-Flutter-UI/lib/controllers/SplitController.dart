import 'package:Tracko/Utils/DatabaseUtil.dart';
import 'package:Tracko/Utils/SettingUtil.dart';
import 'package:Tracko/models/category.dart';
import 'package:Tracko/models/split.dart';
import 'package:Tracko/models/transaction.dart';
// import 'package:jaguar_orm/jaguar_orm.dart'; // Removed - migrating to plain sqflite
import 'package:sqflite/sqlite_api.dart' as sqlite;

class SplitController {
  static Future<double> getDueAmount(int userId) async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    SplitBean splitBean = new SplitBean(adapter);
    var find = splitBean.finder
        .eq(splitBean.userId.name, userId)
        .eq(splitBean.isSettled.name, 0);
    List<Split> splits = await splitBean.findMany(find);

    double amount =
        splits.fold(0.0, (value, element) => value + element.amount);

    return amount;
  }

  static Future<List<Split>> findByUserId(int userId,
      {bool preload = true}) async {
    sqlite.Database db = await DatabaseUtil.getRawDatabase();
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    SplitBean splitBean = new SplitBean(adapter);
    TransactionBean transactionBean = new TransactionBean(adapter);
    CategoryBean categoryBean = new CategoryBean(adapter);

    DateTime month = SettingUtil.currentMonth;
    DateTime nextMonth = SettingUtil.nextMonth;

    String sql = """
      SELECT s.${splitBean.id.name}, 
             s.${splitBean.isSettled.name},
             s.${splitBean.transactionId.name}, 
             s.${splitBean.amount.name}, 
             s.${splitBean.userId.name}
      FROM ${splitBean.tableName} s
      JOIN ${transactionBean.tableName} t 
      ON s.${splitBean.transactionId.name} = t.${transactionBean.id.name}
      WHERE   (
          (
              t.${transactionBean.date.name} >= Datetime('$month') 
              AND t.${transactionBean.date.name} < Datetime('$nextMonth') 
          ) 
          OR s.${splitBean.isSettled.name} = 0 
          OR s.${splitBean.settledAt.name} >= Datetime('$month')
      ) 
      AND s.${splitBean.userId.name} = $userId 
      GROUP BY s.${splitBean.id.name} 
      ORDER BY t.${transactionBean.date.name}
    """;

    print(sql);
    List<dynamic> tmp = (await db.rawQuery(sql)).toList();

    List<Split> returningSplit = [];
    for (var splitVar in tmp) {
      Split split = splitBean.fromMap(splitVar);
      split.transaction = await transactionBean.find(split.transactionId);
      if (split.transaction == null) {
        continue;
      }
      if (preload && split.transaction != null) {
        split.transaction!.category =
        await categoryBean.find(split.transactionId);
      }
      returningSplit.add(split);
    }
    return returningSplit;
  }

  static Future<int> settleAll(int userId) async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    SplitBean splitBean = new SplitBean(adapter);
    List<Split> splitList = await splitBean.findByUser(userId);
    for (Split split in splitList) {
      await settleSplit(split);
    }
    return splitList.length;
  }

  static Future<int> settleSplit(Split split, {int? settleTo}) async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    SplitBean splitBean = new SplitBean(adapter);
    Split? s = await splitBean.find(split.id);
    if (s == null) return 0;
    if (settleTo != null) {
      s.isSettled = settleTo;
    } else {
      s.isSettled = s.isSettled == 1 ? 0 : 1;
    }
    if (s.isSettled == 1) s.settledAt = DateTime.now();
    await splitBean.update(s);
    return s.isSettled;
  }

  static removeByTransactionId(int transactionId) async {
    var adapter = await DatabaseUtil.getAdapter();
    await adapter.connect();
    SplitBean splitBean = new SplitBean(adapter);
    await splitBean.removeByTransaction(transactionId);
//    print(await splitBean.findByTransaction(transactionId));
  }
}
