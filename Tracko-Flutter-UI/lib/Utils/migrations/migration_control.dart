import 'package:sqflite/sqflite.dart';

class Migration {
  late int version;
  late String migrationSQL;

  static Migration changeSet(int version, String migrationSQL) {
    if (version < 1) {
      throw Exception("Invaid Version, version >= 1");
    }
    var migration = new Migration();
    migration.version = version;
    migration.migrationSQL = migrationSQL;
    return migration;
  }
}

class MigrationControl {
  static List<Migration> migrations = [];

  static createVersionControlDB(String path, List<Migration> migrations) async {
    int currentVersion = getLatestVersionFromMigrations(migrations);
    MigrationControl.migrations = migrations;
    Database database = await (openDatabase(path));
    int oldVersion = await database.getVersion();
    print("Db version : $oldVersion => $currentVersion");
    if (oldVersion == 0) {
      await onCreate(database, currentVersion);
    } else {
      await onUpdate(database, oldVersion, currentVersion);
    }
    return database;
  }

  static int getLatestVersionFromMigrations(List<Migration> migrations) {
    int max = 0;
    for (Migration migration in migrations) {
      if (migration.version > max) max = migration.version;
    }
    return max;
  }

  static onCreate(Database database, int version) async {
    await migrateAll(database, 0, version);
  }

  static onUpdate(Database database, int oldVersion, int newVersion) async {
    await migrateAll(database, oldVersion, newVersion);
  }

  static migrateAll(Database database, int oldVersion, int newVersion) async {
    print("Starting DB migration");
    for (int i = oldVersion + 1; i <= newVersion; i++) {
      Batch batch = database.batch();
      for (Migration migration in MigrationControl.migrations) {
        if (migration.version == i) {
          String sql = migration.migrationSQL;
          List<String> statements = sql.split(";");
          for (String statement in statements) {
            if (statement
                .trim()
                .length > 0) {
              print("Executing Statement : $statement");
              batch.execute(statement);
            }
          }
          print("Migration Executed Succussfully.");
        }
      }
      await batch.commit();
      await database.setVersion(i);
      print("Migration to version $i complete.");
    }
    print("Finishing DB migration");
  }
}
