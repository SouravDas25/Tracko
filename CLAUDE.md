# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Tracko is an expense management application with:
- **`backend/`** — Spring Boot 3.2 REST API (Java 17), H2 dev / PostgreSQL prod
- **`frontend/`** — Flutter cross-platform mobile app
- **`cli/`** — Python CLI for API testing and database seeding
- **`money-manager-converter/`** — CSV import tool

## Commands

### Backend (Spring Boot)

```bash
cd backend

# Run in dev mode (H2 database, default profile)
mvn spring-boot:run -P dev

# Build
mvn clean install -P dev

# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=TransactionIntegrationTest

# Run all integration tests
mvn test -Dtest="*IntegrationTest"

# Run a single test method
mvn test -Dtest="TransactionIntegrationTest#testCreateTransaction"
```

Dev H2 console available at `http://localhost:8080/h2-console` (URL: `jdbc:h2:file:./tracko-dev-db`).
Swagger UI available at `http://localhost:8080/swagger-ui.html`.

### Frontend (Flutter)

```bash
cd frontend

flutter pub get
flutter run

# Run Flutter tests
flutter test

# Generate JSON serialization code
flutter pub run build_runner build --delete-conflicting-outputs
```

### Taskfile (Recommended for dev)

```bash
task start      # Start backend + Flutter
task backend    # Backend only
task flutter    # Flutter only (Chrome)
task stop       # Stop all services
task test       # Run all tests
task clean      # Clean build artifacts
```

### CLI

```bash
cd cli
python -m cli --help
python -m cli login --username user@example.com --password password
python seed_database.py   # Seed dev database with sample data
```

## Architecture

### Backend Layer Structure

```
controllers/        HTTP request handling, input validation, auth checks
services/           Business logic
  transactions/     Transaction subsystem (see below)
repositories/       Spring Data JPA repositories
entities/           JPA entities
dtos/               Response DTOs (read projection objects)
models/request/     Inbound request models
models/responses/   Outbound response models
configs/            Spring configuration (security, exception handling)
filters/            JWT auth filter, request logging
```

### Transaction Subsystem (Critical Architecture)

The transaction system has a deliberate read/write split:

- **`TransactionService`** — Read-only queries. All read paths go here.
- **`TransactionWriteService`** — Single write entry point. Acts as a facade that delegates to:
  - `CreditTransactionService` — income transactions
  - `DebitTransactionService` — expense transactions
  - `TransferService` — account-to-account transfers
  - `TransferConversionService` — currency conversion for transfers

**Transfer implementation**: Transfers are stored as two `Transaction` rows (DEBIT + CREDIT) both under a system-created `TRANSFER` category, linked via `linkedTransactionId`. The CREDIT leg has `isCountable=0` so it is excluded from budget/summary calculations. The frontend uses `transactionType=3` (TRANSFER) as a rendering hint — the backend stores only `DEBIT(1)` or `CREDIT(2)` in `TransactionEntryType`.

**Type distinction**:
- `TransactionEntryType` (DB enum): `DEBIT(1)`, `CREDIT(2)` — stored in the database
- `TransactionType` (API enum): `DEBIT(1)`, `CREDIT(2)`, `TRANSFER(3)` — used in request/response
- `TransactionEntryType.TRANSFER_RENDERING_VALUE = 3` is a virtual value for frontend rendering only
- `Transaction.getRenderedTransactionType()` returns 3 for the debit leg of a transfer

### Budget System (ZBB)

Zero-based budgeting is implemented across:
- `BudgetMonth` entity — per-user per-month budget container
- `BudgetCategoryAllocation` entity — per-category allocations within a month
- `BudgetCalculationService` — computes actuals vs. allocations, handles rollover
- Rollover = sum of net totals from all months strictly before the current month

### Multi-Currency

- Each transaction stores `originalCurrency`, `originalAmount`, and `exchangeRate`
- The DB-computed column `amount` = `original_amount * exchange_rate` (base currency)
- `UserCurrency` tracks which currencies a user has configured
- `ExchangeRateService` fetches live rates

### Database Migrations

Liquibase manages schema via `backend/src/main/resources/db/changelog/db.changelog-master.yaml`. Dev profile uses H2 with `MODE=PostgreSQL`.

### Frontend Architecture

- **DI**: `get_it` service locator, configured in `frontend/lib/di/di.dart`
- **HTTP**: `dio` client wrapped in `ApiClient` service
- **API config**: `frontend/lib/config/api_config.dart` — base URL configurable at runtime (stored in `SharedPreferences`); Android emulator uses `10.0.2.2:8080`
- **Repositories**: One repository class per domain entity in `frontend/lib/repositories/`
- **State**: Page-level stateful widgets; no global state manager

### Integration Tests

All integration tests extend `BaseIntegrationTest` (`backend/src/test/java/com/trako/integration/BaseIntegrationTest.java`), which:
- Uses `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")`
- Uses `@Transactional` so each test rolls back automatically
- Provides helpers: `createUniqueUser()`, `generateBearerToken()`, `generateUniquePhone()`
- Uses `TestJwtSecurityConfig` to bypass real JWT secret configuration in tests

### Key Config

- JWT secret: `application.properties` → `jwt.secret`
- Dev DB: file-based H2 at `./tracko-dev-db` (relative to `backend/`)
- Profiles: `dev` (default, H2), `prod` (PostgreSQL — requires env vars `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`)
