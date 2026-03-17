# Tracko - Expense Manager

A comprehensive expense management application with Flutter mobile UI and dual backend (Java + Python with ML).

## Project Structure

- **frontend**: Flutter mobile application
- **backend**: Spring Boot REST API backend
- **ml-backend**: Django backend with ML-powered expense categorization

## Quick Start

### Prerequisites
- Flutter SDK 3.0+
- Java 17+ 
- Maven 3.8+
- Python 3.8+
- Docker & Docker Compose (optional)
- Task (recommended): `go install github.com/go-task/task/v3/cmd/task@latest`

### Option 1: Taskfile (Recommended)
```bash
task start    # Start backend + Flutter
task stop     # Stop services
task test     # Run tests
```

### Option 2: Manual
```bash
# Terminal 1 - Backend
cd backend && mvn spring-boot:run -P dev
# Terminal 2 - Flutter 
cd frontend && flutter run
```

### Option 3: Docker
```bash
docker-compose up -d
```

## Configuration

Database credentials and other sensitive configuration should be stored in environment variables or secure configuration files (not committed to version control).

### Environment Variables
- `DB_HOST`: Database host
- `DB_PORT`: Database port
- `DB_NAME`: Database name
- `DB_USER`: Database username
- `DB_PASSWORD`: Database password

## Features

📱 Cross-platform Flutter app • 💰 Expense tracking & categorization • 📊 Analytics with sticky navigation • 🤖 ML-powered categorization • 🔐 JWT auth (username/password & phone) • 💳 Zero-Based Budgeting • 💱 Multi-currency with live rates • 🔄 Unified Transaction/Transfer API • 📋 CLI tools • 🎨 Modern UI • 🐳 Docker support

## Architecture

**Backend (Java Spring Boot):** REST API with JWT, ZBB engine, multi-currency, unified transaction/transfer API, Liquibase migrations, comprehensive tests

**Frontend (Flutter):** Modern UI with sticky navigation, multi-currency entry, budget management, transfer functionality, offline-first

**ML Backend (Python Django):** Smart categorization using ML models, NLP for descriptions

## Development Tools

**Taskfile Commands:** `task start` • `task flutter` • `task backend` • `task stop` • `task clean` • `task test` • `task install`

**Docker:** `docker-compose up -d` • `docker build -t tracko-app .`

**Integration Tests:**
```bash
cd backend
mvn test -Dtest=*IntegrationTest
mvn test -Dtest=AccountIntegrationTest
mvn test -Dtest=TransactionIntegrationTest
mvn test -Dtest=BudgetIntegrationTest
```

**Test Coverage:** Account Management (6) • Transaction Management (8) • Budget System • Multi-Currency • Authentication • Transfers & Splits


### CSV Import Tool

For migrating data from other sources (like Money Manager), a CSV import tool is available.

```bash
cd money-manager-converter
python import_csv_to_tracko.py "data.csv" --user "Name"
```

See `money-manager-converter/README.md` for full documentation.

## Documentation

**[Startup Guide](README-STARTUP.md)** • **[Backend API](backend/docs/FLUTTER_INTEGRATION_GUIDE.md)** • **[Migration Status](backend/docs/MIGRATION_COMPLETE.md)** • **[CLI Guide](cli/README_TYPER.md)**

## CLI Tools (New Typer-based CLI)

**Quick Start:**
```bash
cd cli && pip install -r requirements.txt
python -m cli auth login
python -m cli account list
python -m cli db seed
```

**Features:** Rich tables • Interactive prompts • Progress bars • Tab completion

**Key Commands:**
- **Auth:** `python -m cli auth login/logout`
- **Database:** `python -m cli db seed/reset`
- **Resources:** `python -m cli account|transaction|category|budget|contact|currency list/add/update/delete`
- **API:** `python -m cli request --method GET --path /api/health`

**Documentation:** [`cli/README_TYPER.md`](cli/README_TYPER.md) • [`cli/MIGRATION_COMPLETE.md`](cli/MIGRATION_COMPLETE.md)

## License

See individual component repositories for license information.

## Troubleshooting

**Port 8080 in use:** `task stop` or find/kill process on Windows

**Flutter hot reload:** Use `flutter run` (interactive) or `task flutter`

**Backend fails:** Check Java 17+, Maven setup, or run `task clean && task install`

**Database:** Uses H2 in-memory for development, PostgreSQL for production

**Docker:** Ensure Docker Desktop running, try `docker-compose build --no-cache`
