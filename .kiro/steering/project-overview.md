# Tracko — Project Overview

## What is Tracko?
Tracko is a comprehensive expense tracking and budget management application with multi-currency support, transaction splitting, and budget allocation features.

## Monorepo Structure

| Folder     | Stack                  | Language | Purpose                        |
|------------|------------------------|----------|--------------------------------|
| `backend/` | Spring Boot 3.2.2      | Java 17  | REST API server                |
| `frontend/`| Flutter (Dart SDK 3.0+)| Dart     | Mobile app (Android/iOS)       |
| `cli/`     | Typer + Rich           | Python 3.10+ | Command-line interface     |
| `sdk/`     | OpenAPI Generator      | Python 3.7+ | Auto-generated API client   |

## Build, Run & Test Commands

### Backend (Java/Spring Boot)
```bash
# From backend/ directory
mvn clean package          # Build
mvn spring-boot:run        # Run (dev profile, H2 database)
mvn test                   # Run tests
mvn test -Dtest=ClassName  # Run a specific test class
```
- Dev profile uses H2 in-file database (`tracko-dev-db.mv.db`)
- Test profile uses H2 in-memory with PostgreSQL compatibility mode
- Prod profile uses PostgreSQL
- Liquibase manages all schema migrations (`src/main/resources/db/changelog/`)
- JaCoCo enforces 70% line / 50% branch coverage
- Swagger UI available at `/swagger-ui.html` when running

### Frontend (Flutter/Dart)
```bash
# From frontend/ directory
flutter pub get            # Install dependencies
flutter run                # Run on connected device/emulator
flutter build apk          # Build Android APK
flutter test               # Run unit tests
flutter test integration_test/  # Run integration tests
flutter analyze            # Run Dart analyzer
```

### CLI (Python)
```bash
# From cli/ directory
pip install -e ../sdk      # Install SDK dependency first
pip install -e .           # Install CLI in editable mode
trako --help               # Run CLI
pytest                     # Run tests with coverage
```

### SDK (Python)
```bash
# From sdk/ directory
pip install -e .           # Install in editable mode
pytest                     # Run tests
```

## Architecture Notes
- Frontend and CLI both consume the Backend REST API
- CLI depends on the SDK package (`tracko-sdk`) for API client code
- SDK is auto-generated from the Backend's OpenAPI spec — do not hand-edit `sdk/tracko_sdk/`
- Authentication is JWT-based (JJWT library in backend, secure storage in frontend)
- Backend uses Spring Security with custom JWT filter
- Frontend uses GetIt for dependency injection and Dio for HTTP
- CLI uses Typer for command structure and Rich for terminal output

## Database
- **Dev:** H2 file-based (`backend/tracko-dev-db.mv.db`)
- **Test:** H2 in-memory with `MODE=PostgreSQL`
- **Prod:** PostgreSQL
- **Migrations:** Liquibase changelogs in `backend/src/main/resources/db/changelog/`

## Key Conventions
- Backend package: `com.trako` (controllers, services, repositories, entities, dtos, enums)
- Frontend follows a page-based structure under `lib/pages/` with shared components in `lib/component/`
- CLI commands are organized as Typer sub-apps in `cli/commands/`
