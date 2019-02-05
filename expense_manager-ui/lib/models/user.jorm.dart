// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'user.dart';

// **************************************************************************
// BeanGenerator
// **************************************************************************

abstract class _UserBean implements Bean<User> {
  final id = IntField('id');
  final name = StrField('name');
  final phoneNo = StrField('phone_no');
  final email = StrField('email');
  Map<String, Field> _fields;
  Map<String, Field> get fields => _fields ??= {
        id.name: id,
        name.name: name,
        phoneNo.name: phoneNo,
        email.name: email,
      };
  User fromMap(Map map) {
    User model = User();
    model.id = adapter.parseValue(map['id']);
    model.name = adapter.parseValue(map['name']);
    model.phoneNo = adapter.parseValue(map['phone_no']);
    model.email = adapter.parseValue(map['email']);

    return model;
  }

  List<SetColumn> toSetColumns(User model,
      {bool update = false, Set<String> only}) {
    List<SetColumn> ret = [];

    if (only == null) {
      if (model.id != null) {
        ret.add(id.set(model.id));
      }
      ret.add(name.set(model.name));
      ret.add(phoneNo.set(model.phoneNo));
      ret.add(email.set(model.email));
    } else {
      if (model.id != null) {
        if (only.contains(id.name)) ret.add(id.set(model.id));
      }
      if (only.contains(name.name)) ret.add(name.set(model.name));
      if (only.contains(phoneNo.name)) ret.add(phoneNo.set(model.phoneNo));
      if (only.contains(email.name)) ret.add(email.set(model.email));
    }

    return ret;
  }

  Future<void> createTable({bool ifNotExists: false}) async {
    final st = Sql.create(tableName, ifNotExists: ifNotExists);
    st.addInt(id.name, primary: true, autoIncrement: true, isNullable: false);
    st.addStr(name.name, length: 250, isNullable: false);
    st.addStr(phoneNo.name, length: 10, isNullable: false);
    st.addStr(email.name, length: 250, isNullable: true);
    return adapter.createTable(st);
  }

  Future<dynamic> insert(User model, {bool cascade: false}) async {
    final Insert insert = inserter.setMany(toSetColumns(model)).id(id.name);
    var retId = await adapter.insert(insert);
    if (cascade) {
      User newModel;
    }
    return retId;
  }

  Future<void> insertMany(List<User> models) async {
    final List<List<SetColumn>> data =
        models.map((model) => toSetColumns(model)).toList();
    final InsertMany insert = inserters.addAll(data);
    await adapter.insertMany(insert);
    return;
  }

  Future<dynamic> upsert(User model, {bool cascade: false}) async {
    final Upsert upsert = upserter.setMany(toSetColumns(model)).id(id.name);
    var retId = await adapter.upsert(upsert);
    if (cascade) {
      User newModel;
    }
    return retId;
  }

  Future<void> upsertMany(List<User> models) async {
    final List<List<SetColumn>> data = [];
    for (var i = 0; i < models.length; ++i) {
      var model = models[i];
      data.add(toSetColumns(model).toList());
    }
    final UpsertMany upsert = upserters.addAll(data);
    await adapter.upsertMany(upsert);
    return;
  }

  Future<int> update(User model, {Set<String> only}) async {
    final Update update = updater
        .where(this.id.eq(model.id))
        .setMany(toSetColumns(model, only: only));
    return adapter.update(update);
  }

  Future<void> updateMany(List<User> models) async {
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

  Future<User> find(int id, {bool preload: false, bool cascade: false}) async {
    final Find find = finder.where(this.id.eq(id));
    return await findOne(find);
  }

  Future<int> remove(int id) async {
    final Remove remove = remover.where(this.id.eq(id));
    return adapter.remove(remove);
  }

  Future<int> removeMany(List<User> models) async {
    final Remove remove = remover;
    for (final model in models) {
      remove.or(this.id.eq(model.id));
    }
    return adapter.remove(remove);
  }
}
