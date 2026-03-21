# Tracko - Expense Manager

A full-stack expense management app: Flutter mobile UI, Spring Boot API, and ML-powered categorization.

## What It Does

- Track expenses and income across multiple accounts and currencies
- Zero-based budgeting with real-time usage tracking
- ML-powered automatic expense categorization
- Transfer money between accounts with split tracking
- JWT authentication (username/password & phone)
- CLI tools for power users and automation

## Architecture

```
┌─────────────┐     ┌──────────────────┐        ┌─────────────────┐
│   Flutter    │────▶│  Spring Boot API  │────▶│   PostgreSQL    │
│  Mobile App  │    │  (Java 17, JWT)  │        │   (H2 in dev)   │
└─────────────┘     └───────────────────┘       └─────────────────┘
```

| Component | Path | Tech |
|-----------|------|------|
| Mobile App | `frontend/` | Flutter/Dart |
| REST API | `backend/` | Spring Boot, Liquibase, JWT |
| ML Service | `ml-backend/` | Django, scikit-learn, NLP |
| CLI | `cli/` | Python, Typer, Rich |

## Quick Start

**Prerequisites:** Flutter 3.0+ • Java 17+ • Maven 3.8+ • Python 3.8+

```bash
# Install Task runner (recommended)
go install github.com/go-task/task/v3/cmd/task@latest

task start    # Starts backend (localhost:8080) + Flutter app
task stop     # Stops all services
task test     # Runs test suite
```

> For manual startup, Docker, or Windows scripts → **[Startup Guide](README-STARTUP.md)**

## Development

```bash
task start       # Start everything
task backend     # Backend only
task flutter     # Flutter only
task clean       # Clean build artifacts
task install     # Install dependencies
```

Backend API runs at `http://localhost:8080`. Uses H2 in-memory database in development, PostgreSQL in production.

## CLI Usage

```bash
cd cli && pip install -r requirements.txt
```

```bash
python -m cli auth login                # Login (interactive password prompt)
python -m cli account list              # List accounts
python -m cli transaction add \
  --amount 50 --type expense \
  --name "Lunch"                        # Add expense
python -m cli budget view               # View budget with usage %
python -m cli db seed                   # Seed sample data
```

Common command groups: `account` • `transaction` • `budget` • `category` • `contact` • `currency` • `split` • `stats`

All commands support `--raw` for JSON output and `--help` for usage details.

> Full command reference → **[CLI Guide](cli/README.md)**

## Documentation

| Guide | Description |
|-------|-------------|
| **[Startup Guide](README-STARTUP.md)** | Configuration, env variables, troubleshooting |
| **[API Integration](backend/docs/FLUTTER_INTEGRATION_GUIDE.md)** | Backend API reference for frontend |
| **[CLI Guide](cli/README.md)** | Full CLI command reference |
| **[CSV Import](money-manager-converter/README.md)** | Migrate data from Money Manager etc. |
| **[Migration Status](backend/docs/MIGRATION_COMPLETE.md)** | Backend migration changelog |
