// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'split.dart';

// **************************************************************************
// BeanGenerator
// **************************************************************************

abstract class _SplitBean implements Bean<Split> {
  final id = IntField('id');
  final transactionId = IntField('transaction_id');
  final userId = IntField('user_id');
  final amount = DoubleField('amount');
  final isSettled = IntField('is_settled');
  final settledAt = DateTimeField('settled_at');
  Map<String, Field> _fields;
  Map<String, Field> get fields => _fields ??= {
        id.name: id,
        transactionId.name: transactionId,
        userId.name: userId,
        amount.name: amount,
        isSettled.name: isSettled,
        settledAt.name: settledAt,
      };
  Split fromMap(Map map) {
    Split model = Split();
    model.id = adapter.parseValue(map['id']);
    model.transactionId = adapter.parseValue(map['transaction_id']);
    model.userId = adapter.parseValue(map['user_id']);
    model.amount = adapter.parseValue(map['amount']);
    model.isSettled = adapter.parseValue(map['is_settled']);
    model.settledAt = adapter.parseValue(map['settled_at']);

    return model;
  }

  List<SetColumn> toSetColumns(Split model,
      {bool update = false, Set<String> only}) {
    List<SetColumn> ret = [];

    if (only == null) {
      if (model.id != null) {
        ret.add(id.set(model.id));
      }
      ret.add(transactionId.set(model.transactionId));
      ret.add(userId.set(model.userId));
      ret.add(amount.set(model.amount));
      ret.add(isSettled.set(model.isSettled));
      ret.add(settledAt.set(model.settledAt));
    } else {
      if (model.id != null) {
        if (only.contains(id.name)) ret.add(id.set(model.id));
      }
      if (only.contains(transactionId.name))
        ret.add(transactionId.set(model.transactionId));
      if (only.contains(userId.name)) ret.add(userId.set(model.userId));
      if (only.contains(amount.name)) ret.add(amount.set(model.amount));
      if (only.contains(isSettled.name))
        ret.add(isSettled.set(model.isSettled));
      if (only.contains(settledAt.name))
        ret.add(settledAt.set(model.settledAt));
    }

    return ret;
  }

  Future<void> createTable({bool ifNotExists: false}) async {
    final st = Sql.create(tableName, ifNotExists: ifNotExists);
    st.addInt(id.name, primary: true, autoIncrement: true, isNullable: false);
    st.addInt(transactionId.name,
        foreignTable: transactionBean.tableName,
        foreignCol: 'id',
        isNullable: false);
    st.addInt(userId.name,
        foreignTable: userBean.tableName, foreignCol: 'id', isNullable: false);
    st.addDouble(amount.name, isNullable: false);
    st.addInt(isSettled.name, isNullable: true);
    st.addDateTime(settledAt.name, isNullable: false);
    return adapter.createTable(st);
  }

  Future<dynamic> insert(Split model, {bool cascade: false}) async {
    final Insert insert = inserter.setMany(toSetColumns(model)).id(id.name);
    var retId = await adapter.insert(insert);
    if (cascade) {
      Split newModel;
    }
    return retId;
  }

  Future<void> insertMany(List<Split> models) async {
    final List<List<SetColumn>> data =
        models.map((model) => toSetColumns(model)).toList();
    final InsertMany insert = inserters.addAll(data);
    await adapter.insertMany(insert);
    return;
  }

  Future<dynamic> upsert(Split model, {bool cascade: false}) async {
    final Upsert upsert = upserter.setMany(toSetColumns(model)).id(id.name);
    var retId = await adapter.upsert(upsert);
    if (cascade) {
      Split newModel;
    }
    return retId;
  }

  Future<void> upsertMany(List<Split> models) async {
    final List<List<SetColumn>> data = [];
    for (var i = 0; i < models.length; ++i) {
      var model = models[i];
      data.add(toSetColumns(model).toList());
    }
    final UpsertMany upsert = upserters.addAll(data);
    await adapter.upsertMany(upsert);
    return;
  }

  Future<int> update(Split model, {Set<String> only}) async {
    final Update update = updater
        .where(this.id.eq(model.id))
        .setMany(toSetColumns(model, only: only));
    return adapter.update(update);
  }

  Future<void> updateMany(List<Split> models) async {
    final List<List<SetColumn>> data = [];
    final List<Expression> where = [];
    for (var i = 0; i < models.length; ++i) {
      var model = models[i];
      data.add(toSetColumns(model).toList());
      where.add(this.id.eq(model.id));
    }
    final UpdateMany update = updaters.addAll(data, where);
    await adapter.updateMany(update);
    return;
  }

  Future<Split> find(int id, {bool preload: false, bool cascade: false}) async {
    final Find find = finder.where(this.id.eq(id));
    return await findOne(find);
  }

  Future<int> remove(int id) async {
    final Remove remove = remover.where(this.id.eq(id));
    return adapter.remove(remove);
  }

  Future<int> removeMany(List<Split> models) async {
    final Remove remove = remover;
    for (final model in models) {
      remove.or(this.id.eq(model.id));
    }
    return adapter.remove(remove);
  }

  Future<List<Split>> findByTransaction(int transactionId,
      {bool preload: false, bool cascade: false}) async {
    final Find find = finder.where(this.transactionId.eq(transactionId));
    return findMany(find);
  }

  Future<List<Split>> findByTransactionList(List<Transaction> models,
      {bool preload: false, bool cascade: false}) async {
    final Find find = finder;
    for (Transaction model in models) {
      find.or(this.transactionId.eq(model.id));
    }
    return findMany(find);
  }

  Future<int> removeByTransaction(int transactionId) async {
    final Remove rm = remover.where(this.transactionId.eq(transactionId));
    return await adapter.remove(rm);
  }

  void associateTransaction(Split child, Transaction parent) {
    child.transactionId = parent.id;
  }

  Future<List<Split>> findByUser(int userId,
      {bool preload: false, bool cascade: false}) async {
    final Find find = finder.where(this.userId.eq(userId));
    return findMany(find);
  }

  Future<List<Split>> findByUserList(List<User> models,
      {bool preload: false, bool cascade: false}) async {
    final Find find = finder;
    for (User model in models) {
      find.or(this.userId.eq(model.id));
    }
    return findMany(find);
  }

  Future<int> removeByUser(int userId) async {
    final Remove rm = remover.where(this.userId.eq(userId));
    return await adapter.remove(rm);
  }

  void associateUser(Split child, User parent) {
    child.userId = parent.id;
  }

  TransactionBean get transactionBean;
  UserBean get userBean;
}
