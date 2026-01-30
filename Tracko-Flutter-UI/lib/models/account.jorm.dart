// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'account.dart';

// **************************************************************************
// BeanGenerator
// **************************************************************************

abstract class _AccountBean implements Bean<Account> {
  final id = IntField('id');
  final name = StrField('name');
  final userId = IntField('user_id');
  Map<String, Field> _fields;
  Map<String, Field> get fields => _fields ??= {
        id.name: id,
        name.name: name,
        userId.name: userId,
      };
  Account fromMap(Map map) {
    Account model = Account();
    model.id = adapter.parseValue(map['id']);
    model.name = adapter.parseValue(map['name']);
    model.userId = adapter.parseValue(map['user_id']);

    return model;
  }

  List<SetColumn> toSetColumns(Account model,
      {bool update = false, Set<String> only}) {
    List<SetColumn> ret = [];

    if (only == null) {
      if (model.id != null) {
        ret.add(id.set(model.id));
      }
      ret.add(name.set(model.name));
      ret.add(userId.set(model.userId));
    } else {
      if (model.id != null) {
        if (only.contains(id.name)) ret.add(id.set(model.id));
      }
      if (only.contains(name.name)) ret.add(name.set(model.name));
      if (only.contains(userId.name)) ret.add(userId.set(model.userId));
    }

    return ret;
  }

  Future<void> createTable({bool ifNotExists: false}) async {
    final st = Sql.create(tableName, ifNotExists: ifNotExists);
    st.addInt(id.name, primary: true, autoIncrement: true, isNullable: false);
    st.addStr(name.name, length: 250, isNullable: false);
    st.addInt(userId.name,
        foreignTable: userBean.tableName, foreignCol: 'id', isNullable: false);
    return adapter.createTable(st);
  }

  Future<dynamic> insert(Account model, {bool cascade: false}) async {
    final Insert insert = inserter.setMany(toSetColumns(model)).id(id.name);
    var retId = await adapter.insert(insert);
    if (cascade) {
      Account newModel;
      if (model.transactions != null) {
        newModel ??= await find(retId);
        model.transactions
            .forEach((x) => transactionBean.associateAccount(x, newModel));
        for (final child in model.transactions) {
          await transactionBean.insert(child);
        }
      }
    }
    return retId;
  }

  Future<void> insertMany(List<Account> models, {bool cascade: false}) async {
    if (cascade) {
      final List<Future> futures = [];
      for (var model in models) {
        futures.add(insert(model, cascade: cascade));
      }
      await Future.wait(futures);
      return;
    } else {
      final List<List<SetColumn>> data =
          models.map((model) => toSetColumns(model)).toList();
      final InsertMany insert = inserters.addAll(data);
      await adapter.insertMany(insert);
      return;
    }
  }

  Future<dynamic> upsert(Account model, {bool cascade: false}) async {
    final Upsert upsert = upserter.setMany(toSetColumns(model)).id(id.name);
    var retId = await adapter.upsert(upsert);
    if (cascade) {
      Account newModel;
      if (model.transactions != null) {
        newModel ??= await find(retId);
        model.transactions
            .forEach((x) => transactionBean.associateAccount(x, newModel));
        for (final child in model.transactions) {
          await transactionBean.upsert(child);
        }
      }
    }
    return retId;
  }

  Future<void> upsertMany(List<Account> models, {bool cascade: false}) async {
    if (cascade) {
      final List<Future> futures = [];
      for (var model in models) {
        futures.add(upsert(model, cascade: cascade));
      }
      await Future.wait(futures);
      return;
    } else {
      final List<List<SetColumn>> data = [];
      for (var i = 0; i < models.length; ++i) {
        var model = models[i];
        data.add(toSetColumns(model).toList());
      }
      final UpsertMany upsert = upserters.addAll(data);
      await adapter.upsertMany(upsert);
      return;
    }
  }

  Future<int> update(Account model,
      {bool cascade: false, bool associate: false, Set<String> only}) async {
    final Update update = updater
        .where(this.id.eq(model.id))
        .setMany(toSetColumns(model, only: only));
    final ret = adapter.update(update);
    if (cascade) {
      Account newModel;
      if (model.transactions != null) {
        if (associate) {
          newModel ??= await find(model.id);
          model.transactions
              .forEach((x) => transactionBean.associateAccount(x, newModel));
        }
        for (final child in model.transactions) {
          await transactionBean.update(child);
        }
      }
    }
    return ret;
  }

  Future<void> updateMany(List<Account> models, {bool cascade: false}) async {
    if (cascade) {
      final List<Future> futures = [];
      for (var model in models) {
        futures.add(update(model, cascade: cascade));
      }
      await Future.wait(futures);
      return;
    } else {
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
  }

  Future<Account> find(int id,
      {bool preload: false, bool cascade: false}) async {
    final Find find = finder.where(this.id.eq(id));
    final Account model = await findOne(find);
    if (preload && model != null) {
      await this.preload(model, cascade: cascade);
    }
    return model;
  }

  Future<int> remove(int id, [bool cascade = false]) async {
    if (cascade) {
      final Account newModel = await find(id);
      if (newModel != null) {
        await transactionBean.removeByAccount(newModel.id);
      }
    }
    final Remove remove = remover.where(this.id.eq(id));
    return adapter.remove(remove);
  }

  Future<int> removeMany(List<Account> models) async {
    final Remove remove = remover;
    for (final model in models) {
      remove.or(this.id.eq(model.id));
    }
    return adapter.remove(remove);
  }

  Future<List<Account>> findByUser(int userId,
      {bool preload: false, bool cascade: false}) async {
    final Find find = finder.where(this.userId.eq(userId));
    final List<Account> models = await findMany(find);
    if (preload) {
      await this.preloadAll(models, cascade: cascade);
    }
    return models;
  }

  Future<List<Account>> findByUserList(List<User> models,
      {bool preload: false, bool cascade: false}) async {
    final Find find = finder;
    for (User model in models) {
      find.or(this.userId.eq(model.id));
    }
    final List<Account> retModels = await findMany(find);
    if (preload) {
      await this.preloadAll(retModels, cascade: cascade);
    }
    return retModels;
  }

  Future<int> removeByUser(int userId) async {
    final Remove rm = remover.where(this.userId.eq(userId));
    return await adapter.remove(rm);
  }

  void associateUser(Account child, User parent) {
    child.userId = parent.id;
  }

  Future<Account> preload(Account model, {bool cascade: false}) async {
    model.transactions = await transactionBean.findByAccount(model.id,
        preload: cascade, cascade: cascade);
    return model;
  }

  Future<List<Account>> preloadAll(List<Account> models,
      {bool cascade: false}) async {
    models.forEach((Account model) => model.transactions ??= []);
    await OneToXHelper.preloadAll<Account, Transaction>(
        models,
        (Account model) => [model.id],
        transactionBean.findByAccountList,
        (Transaction model) => [model.accountId],
        (Account model, Transaction child) => model.transactions.add(child),
        cascade: cascade);
    return models;
  }

  TransactionBean get transactionBean;
  UserBean get userBean;
}
