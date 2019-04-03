// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'transaction.dart';

// **************************************************************************
// BeanGenerator
// **************************************************************************

abstract class _TransactionBean implements Bean<Transaction> {
  final id = IntField('id');
  final transactionType = IntField('transaction_type');
  final name = StrField('name');
  final logo = StrField('logo');
  final comments = StrField('comments');
  final date = DateTimeField('date');
  final amount = DoubleField('amount');
  final accountId = IntField('account_id');
  final categoryId = IntField('category_id');
  Map<String, Field> _fields;
  Map<String, Field> get fields => _fields ??= {
        id.name: id,
        transactionType.name: transactionType,
        name.name: name,
        logo.name: logo,
        comments.name: comments,
        date.name: date,
        amount.name: amount,
        accountId.name: accountId,
        categoryId.name: categoryId,
      };
  Transaction fromMap(Map map) {
    Transaction model = Transaction();
    model.id = adapter.parseValue(map['id']);
    model.transactionType = adapter.parseValue(map['transaction_type']);
    model.name = adapter.parseValue(map['name']);
    model.logo = adapter.parseValue(map['logo']);
    model.comments = adapter.parseValue(map['comments']);
    model.date = adapter.parseValue(map['date']);
    model.amount = adapter.parseValue(map['amount']);
    model.accountId = adapter.parseValue(map['account_id']);
    model.categoryId = adapter.parseValue(map['category_id']);

    return model;
  }

  List<SetColumn> toSetColumns(Transaction model,
      {bool update = false, Set<String> only}) {
    List<SetColumn> ret = [];

    if (only == null) {
      if (model.id != null) {
        ret.add(id.set(model.id));
      }
      ret.add(transactionType.set(model.transactionType));
      ret.add(name.set(model.name));
      ret.add(logo.set(model.logo));
      ret.add(comments.set(model.comments));
      ret.add(date.set(model.date));
      ret.add(amount.set(model.amount));
      ret.add(accountId.set(model.accountId));
      ret.add(categoryId.set(model.categoryId));
    } else {
      if (model.id != null) {
        if (only.contains(id.name)) ret.add(id.set(model.id));
      }
      if (only.contains(transactionType.name))
        ret.add(transactionType.set(model.transactionType));
      if (only.contains(name.name)) ret.add(name.set(model.name));
      if (only.contains(logo.name)) ret.add(logo.set(model.logo));
      if (only.contains(comments.name)) ret.add(comments.set(model.comments));
      if (only.contains(date.name)) ret.add(date.set(model.date));
      if (only.contains(amount.name)) ret.add(amount.set(model.amount));
      if (only.contains(accountId.name))
        ret.add(accountId.set(model.accountId));
      if (only.contains(categoryId.name))
        ret.add(categoryId.set(model.categoryId));
    }

    return ret;
  }

  Future<void> createTable({bool ifNotExists: false}) async {
    final st = Sql.create(tableName, ifNotExists: ifNotExists);
    st.addInt(id.name, primary: true, autoIncrement: true, isNullable: false);
    st.addInt(transactionType.name, isNullable: false);
    st.addStr(name.name, length: 128, isNullable: false);
    st.addStr(logo.name, length: 512, isNullable: true);
    st.addStr(comments.name, length: 512, isNullable: true);
    st.addDateTime(date.name, isNullable: false);
    st.addDouble(amount.name, isNullable: false);
    st.addInt(accountId.name,
        foreignTable: accountBean.tableName,
        foreignCol: 'id',
        isNullable: false);
    st.addInt(categoryId.name,
        foreignTable: categoryBean.tableName,
        foreignCol: 'id',
        isNullable: false);
    return adapter.createTable(st);
  }

  Future<dynamic> insert(Transaction model, {bool cascade: false}) async {
    final Insert insert = inserter.setMany(toSetColumns(model)).id(id.name);
    var retId = await adapter.insert(insert);
    if (cascade) {
      Transaction newModel;
    }
    return retId;
  }

  Future<void> insertMany(List<Transaction> models) async {
    final List<List<SetColumn>> data =
        models.map((model) => toSetColumns(model)).toList();
    final InsertMany insert = inserters.addAll(data);
    await adapter.insertMany(insert);
    return;
  }

  Future<dynamic> upsert(Transaction model, {bool cascade: false}) async {
    final Upsert upsert = upserter.setMany(toSetColumns(model)).id(id.name);
    var retId = await adapter.upsert(upsert);
    if (cascade) {
      Transaction newModel;
    }
    return retId;
  }

  Future<void> upsertMany(List<Transaction> models) async {
    final List<List<SetColumn>> data = [];
    for (var i = 0; i < models.length; ++i) {
      var model = models[i];
      data.add(toSetColumns(model).toList());
    }
    final UpsertMany upsert = upserters.addAll(data);
    await adapter.upsertMany(upsert);
    return;
  }

  Future<int> update(Transaction model, {Set<String> only}) async {
    final Update update = updater
        .where(this.id.eq(model.id))
        .setMany(toSetColumns(model, only: only));
    return adapter.update(update);
  }

  Future<void> updateMany(List<Transaction> models) async {
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

  Future<Transaction> find(int id,
      {bool preload: false, bool cascade: false}) async {
    final Find find = finder.where(this.id.eq(id));
    return await findOne(find);
  }

  Future<int> remove(int id) async {
    final Remove remove = remover.where(this.id.eq(id));
    return adapter.remove(remove);
  }

  Future<int> removeMany(List<Transaction> models) async {
    final Remove remove = remover;
    for (final model in models) {
      remove.or(this.id.eq(model.id));
    }
    return adapter.remove(remove);
  }

  Future<List<Transaction>> findByAccount(int accountId,
      {bool preload: false, bool cascade: false}) async {
    final Find find = finder.where(this.accountId.eq(accountId));
    return findMany(find);
  }

  Future<List<Transaction>> findByAccountList(List<Account> models,
      {bool preload: false, bool cascade: false}) async {
    final Find find = finder;
    for (Account model in models) {
      find.or(this.accountId.eq(model.id));
    }
    return findMany(find);
  }

  Future<int> removeByAccount(int accountId) async {
    final Remove rm = remover.where(this.accountId.eq(accountId));
    return await adapter.remove(rm);
  }

  void associateAccount(Transaction child, Account parent) {
    child.accountId = parent.id;
  }

  Future<List<Transaction>> findByCategory(int categoryId,
      {bool preload: false, bool cascade: false}) async {
    final Find find = finder.where(this.categoryId.eq(categoryId));
    return findMany(find);
  }

  Future<List<Transaction>> findByCategoryList(List<Category> models,
      {bool preload: false, bool cascade: false}) async {
    final Find find = finder;
    for (Category model in models) {
      find.or(this.categoryId.eq(model.id));
    }
    return findMany(find);
  }

  Future<int> removeByCategory(int categoryId) async {
    final Remove rm = remover.where(this.categoryId.eq(categoryId));
    return await adapter.remove(rm);
  }

  void associateCategory(Transaction child, Category parent) {
    child.categoryId = parent.id;
  }

  AccountBean get accountBean;
  CategoryBean get categoryBean;
}
