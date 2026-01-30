import 'migration_control.dart';

class Migrations {
  static final List<Migration> migrations = [
    Migration.changeSet(1, """
      PRAGMA foreign_keys=on;
      DROP TABLE IF EXISTS splits;
      DROP TABLE IF EXISTS transactions;
      DROP TABLE IF EXISTS chats;
      DROP TABLE IF EXISTS categories;
      DROP TABLE IF EXISTS accounts;
      DROP TABLE IF EXISTS users;
      
      
      CREATE TABLE users (
          id INTEGER             NOT NULL PRIMARY KEY,
          profile_pic  CHAR(512) NOT NULL,
          name         CHAR(250) NOT NULL,
          phone_no     CHAR(10)  NOT NULL,
          email        CHAR(250),
          fire_base_id CHAR(250),
          global_id    INT
      );
      
      
      CREATE TABLE accounts (
          id      INTEGER   NOT NULL PRIMARY KEY,
          name    CHAR(250) NOT NULL,
          user_id INT       NOT NULL REFERENCES users(id)
      );
      
      
      CREATE TABLE categories (
          id      INTEGER   NOT NULL PRIMARY KEY,
          name    CHAR(250) NOT NULL,
          user_id INT       NOT NULL REFERENCES users(id)
      );
      
      
      CREATE TABLE chats (
          id            INTEGER NOT NULL PRIMARY KEY,
          user_id       INT     NOT NULL REFERENCES users(id),
          chat_group_id INT     NOT NULL
      );
      
      
      CREATE TABLE transactions (
          id               INTEGER   NOT NULL PRIMARY KEY,
          transaction_type INT       NOT NULL,
          name             CHAR(128) NOT NULL,
          logo             CHAR(512),
          comments         CHAR(512),
          date             TEXT      NOT NULL,
          amount           REAL      NOT NULL,
          account_id       INT       NOT NULL REFERENCES accounts(id),
          category_id      INT       NOT NULL REFERENCES categories(id)
      );
      
      
      CREATE TABLE splits (
          id             INTEGER NOT NULL PRIMARY KEY,
          transaction_id INT     NOT NULL REFERENCES transactions(id),
          user_id        INT     NOT NULL REFERENCES users(id),
          amount         REAL    NOT NULL,
          is_settled     INT
      );
    """),
    Migration.changeSet(2, """
      PRAGMA foreign_keys=off;

      BEGIN TRANSACTION;
      DROP TABLE IF EXISTS _transactions_old;
      ALTER TABLE transactions RENAME TO _transactions_old;
      
      CREATE TABLE transactions (
          id               INTEGER   NOT NULL PRIMARY KEY,
          transaction_type INT       NOT NULL,
          name             CHAR(128) NOT NULL,
          comments         CHAR(512),
          date             TEXT      NOT NULL,
          amount           REAL      NOT NULL,
          account_id       INT       NOT NULL REFERENCES accounts(id),
          category_id      INT       NOT NULL REFERENCES categories(id)
      );
      
      INSERT INTO transactions (id, transaction_type, name, comments, date, account_id, category_id)
        SELECT id, transaction_type, name, comments,date,account_id,category_id
        FROM _transactions_old;
        
      DROP TABLE _transactions_old;
      
      COMMIT;
      
      PRAGMA foreign_keys=on;
    """),
    Migration.changeSet(3, """
    PRAGMA foreign_keys=off;
    BEGIN TRANSACTION;
    DROP TABLE IF EXISTS _users_old;
    ALTER TABLE users RENAME TO _users_old;
    
    CREATE TABLE users (
          id INTEGER             NOT NULL PRIMARY KEY,
          profile_pic  CHAR(512) NOT NULL,
          name         CHAR(250) NOT NULL,
          phone_no     CHAR(10)  NOT NULL,
          email        CHAR(250),
          fire_base_id CHAR(250),
          global_id    CHAR(64) UNIQUE
      );
     
    INSERT INTO users (id, profile_pic, name, phone_no, email, fire_base_id) 
      SELECT id, profile_pic, name, phone_no, email, fire_base_id FROM _users_old;
      
    DROP TABLE _users_old;
    
    COMMIT;
    PRAGMA foreign_keys=on;
    """),
    Migration.changeSet(3, """
    PRAGMA foreign_keys=off;
    BEGIN TRANSACTION;
    DROP TABLE IF EXISTS _chats_old;
    ALTER TABLE chats RENAME TO _chats_old;
    
    CREATE TABLE chats (
          id            INTEGER NOT NULL PRIMARY KEY,
          user_id       INT     NOT NULL REFERENCES users(id),
          chat_group_id CHAR(64) UNIQUE NOT NULL
      );
     
    INSERT INTO chats (id, user_id, chat_group_id) 
      SELECT id, user_id, chat_group_id FROM _chats_old;
      
    DROP TABLE _chats_old;
    
    COMMIT;
    PRAGMA foreign_keys=on;
    """),
    Migration.changeSet(4, """
    ALTER TABLE transactions ADD COLUMN is_countable INT default 1
    """),
    Migration.changeSet(5, """
    PRAGMA foreign_keys=off;
    BEGIN TRANSACTION;
    DROP TABLE IF EXISTS _splits_old;
    ALTER TABLE splits RENAME TO _splits_old;
    
    CREATE TABLE splits (
          id             INTEGER NOT NULL PRIMARY KEY,
          transaction_id INT     NOT NULL REFERENCES transactions(id),
          user_id        INT     NOT NULL REFERENCES users(id),
          amount         REAL    NOT NULL,
          settled_at     TEXT,
          is_settled     INT default 0
      );
     
    INSERT INTO splits (id, transaction_id, user_id, amount, is_settled) 
      SELECT id, transaction_id, user_id, amount, is_settled FROM _splits_old;
      
    DROP TABLE _splits_old;
    
    COMMIT;
    PRAGMA foreign_keys=on;
    """),
    Migration.changeSet(6, """
    CREATE TABLE json_store (
          name          CHAR(64) NOT NULL PRIMARY KEY,
          value         TEXT
      );
    """),
  ];
}
