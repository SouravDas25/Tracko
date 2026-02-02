// Lightweight ORM stubs to replace jaguar_orm for build-time compatibility
// Provides minimal functionality used by controllers.

// Annotation stubs
class GenBean {
  const GenBean();
}

class PrimaryKey {
  final bool auto;
  const PrimaryKey({this.auto = false});
}

class Column {
  final bool isNullable;
  final int? length;
  const Column({this.isNullable = true, this.length});
}

class BelongsTo {
  final Type bean;
  const BelongsTo(this.bean);
}

class HasMany {
  final Type bean;
  const HasMany(this.bean);
}

class IgnoreColumn {
  const IgnoreColumn();
}

// Adapter wrapper around sqflite Database
class Adapter {
  final dynamic db;
  Adapter(this.db);

  static Future<Adapter> ensure(dynamic adapter) async {
    if (adapter is Adapter) return adapter;
    if (adapter != null) return Adapter(adapter);
    throw ArgumentError('Unsupported adapter type: ${adapter.runtimeType}');
  }
}

// Base Bean stub
abstract class Bean<T> {
  final Adapter adapter;
  Bean(this.adapter);
}

// Empty mixins to satisfy "with _XBean" clauses - exported for use in model files
mixin _AccountBean {}
mixin _CategoryBean {}
mixin _TransactionBean {}
mixin _SplitBean {}
mixin _ChatBean {}
mixin _UserBean {}
