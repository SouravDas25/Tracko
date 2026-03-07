# Backend-owned Database Plan

## Goals

- Move all data access from UI to backend APIs.
- Standardize databases:
    - Dev/Test: H2 in-memory via Liquibase.
    - Prod: PostgreSQL via Liquibase.
- Replace UI SQLite usage with REST calls secured by JWT.

## High-level Architecture

- Flutter UI → HTTP(S) → Spring Boot 3 backend → DB (H2 dev, Postgres prod).
- Liquibase manages schema for all environments.

## Backend Changes

- Dependencies (pom.xml)
    - Add liquibase-core (if not present).
    - Ensure H2 available for dev profile.
    - Postgres driver present for prod.
- Profiles & Config
    - application-dev.yml
        - spring.datasource.url: jdbc:h2:mem:expense_manager;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
        - spring.datasource.driver-class-name: org.h2.Driver
        - spring.jpa.hibernate.ddl-auto: none
        - spring.liquibase.change-log: classpath:db/changelog/db.changelog-master.yaml
        - spring.h2.console.enabled: true (optional)
    - application-prod.yml
        - spring.datasource.url: ${JDBC_URL}
        - spring.datasource.username: ${DB_USERNAME}
        - spring.datasource.password: ${DB_PASSWORD}
        - spring.jpa.hibernate.ddl-auto: none
        - spring.liquibase.change-log: classpath:db/changelog/db.changelog-master.yaml
- Security
    - Keep JWT flow; UI attaches Authorization: Bearer <token> to every request.

## Liquibase Structure

- db/changelog/db.changelog-master.yaml
    - Includes ordered changeSet files, e.g.:
        - db/changelog/001-initial-schema.yaml
        - db/changelog/002-indexes.yaml (optional)
- 001-initial-schema.yaml tables (aligned to JPA entities)
    - users (id PK, name, phone_no unique, email, profile_pic, firebase_uuid, is_shadow, created_at, updated_at)
    - chat_groups (id PK, name, created_at)
    - users_chat_groups (id PK, user_id FK→users.id, group_id FK→chat_groups.id)
    - chat_messages (id PK, group_id FK, user_id FK, message, is_read, created_at)
    - splits (id PK, source_user_id FK, due_user_id FK, split_amount, settled_amount, transaction_amount,
      transaction_name, created_at)
    - nlp_data (id PK, user_id FK, request, response, created_at)
- Use VARCHAR for ids (consistent with code), numeric for amounts, TIMESTAMP for dates.

## Endpoint Inventory (current/expected)

- Auth
    - POST /api/oauth/token (login)
    - POST /api/signUp (register)
- Users
    - GET /api/user
    - GET /api/user/{id}
    - GET /api/user/byPhoneNo?phone_no=...
    - POST /api/user/save
- Splits
    - GET /api/split
    - GET /api/split/{userId}
    - POST /api/split
    - PATCH /api/split/settle/{splitId}
- Chat
    - POST /api/chat/create
    - POST /api/chat/send
    - GET/POST /api/chat/messages
    - GET /api/chat/groups/{id}

## Flutter UI Migration

- Remove SQLite data layer and related packages.
- Add service layer using http/dio:
    - Handle login → store JWT securely.
    - Attach Authorization header to protected calls.
    - Map JSON responses to existing models.
- Replace direct queries with API calls to the endpoints above.

## Environment Variables

- Dev
    - SPRING_PROFILES_ACTIVE=dev,standalone
    - JWT_SECRET=<dev secret>
- Prod
    - SPRING_PROFILES_ACTIVE=prod
    - JDBC_URL, DB_USERNAME, DB_PASSWORD
    - JWT_SECRET

## Rollout Steps

1. Implement Liquibase changelogs and profile configs.
2. Verify dev: `mvn -P local spring-boot:run` (H2, Liquibase applies).
3. Update Flutter to use backend APIs (auth first, then features).
4. Deploy backend with Postgres; Liquibase applies schema automatically.
5. End-to-end smoke test (auth, users, splits, chat).

## Validation Checklist

- App starts with H2 in dev; H2 console shows tables.
- App starts with Postgres in prod; changeSets applied exactly once.
- All existing UI features work via REST (no direct DB).
- JWT-based auth required for protected endpoints.
- CI build passes and a simple integration test hits a health/endpoint.
