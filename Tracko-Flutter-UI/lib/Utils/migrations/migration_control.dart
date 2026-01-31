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
    MigrationControl.migrations = migrations;
    return null;
  }

  static int getLatestVersionFromMigrations(List<Migration> migrations) {
    int max = 0;
    for (Migration migration in migrations) {
      if (migration.version > max) max = migration.version;
    }
    return max;
  }

  static onCreate(dynamic database, int version) async {}

  static onUpdate(dynamic database, int oldVersion, int newVersion) async {}

  static migrateAll(dynamic database, int oldVersion, int newVersion) async {}
}
