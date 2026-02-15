# Tracko - Expense Manager

A comprehensive expense management application with Flutter mobile UI and dual backend (Java + Python with ML).

## Project Structure

- **frontend**: Flutter mobile application
- **backend**: Spring Boot REST API backend
- **ml-backend**: Django backend with ML-powered expense categorization

## Getting Started

### Prerequisites
- Flutter SDK 3.0+
- Java 17+ (set JAVA_HOME to a JDK 17 installation)
- Maven 3.8+
- Python 3.8+
- PostgreSQL (for local development)
- Docker & Docker Compose (optional)
- Task (Taskfile) - `go install github.com/go-task/task/v3/cmd/task@latest` (recommended)

### Quick Start Options

#### Option 1: Taskfile (Recommended)
```bash
task start          # Start both backend and Flutter UI
task stop           # Stop all services
task clean          # Clean build artifacts
task test           # Run all tests
task install        # Install dependencies
```

#### Option 2: Manual Startup
```bash
# Terminal 1 - Backend
cd backend
mvn spring-boot:run -P dev

# Terminal 2 - Flutter UI  
cd frontend
flutter run
```

#### Option 3: Docker Compose
```bash
docker-compose up -d
```

### Setup Instructions

1. **Flutter UI Setup**
   ```bash
   cd frontend
   flutter pub get
   flutter run
   ```

2. **Java Backend Setup**
   ```bash
   cd backend
   mvn clean install -P dev
   mvn spring-boot:run -P dev
   ```

> Tip: For hot reload during development, add Spring Boot DevTools to your backend `pom.xml` (development only):

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-devtools</artifactId>
  <optional>true</optional>
</dependency>
```

3. **Python ML Backend Setup**
   ```bash
   cd ml-backend
   pip install -r requirements.txt
   python manage.py migrate
   python manage.py runserver
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

- 📱 Cross-platform mobile app (Flutter) with modern UI design
- 💰 Comprehensive expense tracking and categorization
- 📊 Advanced analytics and reporting with sticky navigation
- 🤖 ML-powered smart categorization
- 🔐 Secure authentication with JWT (username/password & phone login)
- ☁️ Cloud deployment ready (Docker, Heroku, SAP Cloud Platform)
- 💳 **Zero-Based Budgeting (ZBB)** system with monthly allocations
- 💱 **Multi-currency support** with live exchange rates
- 🔄 **Unified Transaction/Transfer API** for seamless money movement
- 📋 **CLI tools** for API testing and database seeding
- 🎨 **Modern UI** with consistent design language across all pages
- 🐳 **Docker support** for containerized deployment

## Architecture

### Backend (Java Spring Boot)
- **RESTful API** with JWT authentication
- **Zero-Based Budgeting (ZBB)** engine with monthly allocations and rollover logic
- **Multi-currency support** with automatic exchange rate fetching
- **Unified Transaction/Transfer API** handling both expense/income and account transfers
- **Liquibase database migrations** for schema versioning
- **Comprehensive test coverage** with integration tests

### Frontend (Flutter)
- **Modern UI design** with consistent styling across all pages
- **Sticky navigation headers** for better UX in list views
- **Multi-currency transaction entry** with automatic rate conversion
- **Budget management interface** with allocation and tracking
- **Transfer functionality** integrated with transaction flow
- **Offline-first architecture** with local caching

### ML Backend (Python Django)
- **Smart categorization** using machine learning models
- **Natural language processing** for transaction descriptions
- **API integration** with main Java backend

## Development Tools

### Taskfile Commands
```bash
task start          # Start Flutter UI and Java Backend
task flutter        # Start Flutter UI only (Chrome)
task backend        # Start Java Backend only (detached)
task stop           # Stop all services
task clean          # Clean build artifacts
task test           # Run tests (Flutter and Backend)
task install        # Install dependencies
task docker:tar     # Build and export Docker image
task docker:publish # Tag and push to registry
```

### Docker Support
```bash
# Build and run with Docker Compose
docker-compose up -d

# Build standalone Docker image
docker build -t tracko-app .
```

### Integration Testing

Tracko includes comprehensive integration tests for the backend API:

#### Running Integration Tests
```bash
# Run all integration tests
cd backend
mvn test -Dtest=*IntegrationTest

# Run specific test classes
mvn test -Dtest=AccountIntegrationTest
mvn test -Dtest=TransactionIntegrationTest
mvn test -Dtest=BudgetIntegrationTest
```

#### Test Coverage
The integration test suite covers:
- **Account Management** (6 tests) - CRUD operations
- **Transaction Management** (8 tests) - CRUD, date range, filtering
- **Budget System** (multiple tests) - ZBB allocations and calculations
- **Multi-Currency** (multiple tests) - Exchange rates and conversions
- **Authentication** (multiple tests) - JWT and phone login
- **Transfers & Splits** (multiple tests) - Money movement between accounts

#### Test Results
All integration tests are designed to pass with 100% success rate, providing full API coverage for the Flutter frontend.

## CLI and Database Seeding

Tracko includes a powerful command-line interface (CLI) for API testing and database management, along with a database seeding script.

### Tracko CLI

The CLI provides direct access to all Tracko backend APIs for testing, debugging, and data management.

#### Installation & Setup

The CLI is located in the `tracko-cli/` directory:

```bash
cd tracko-cli
python tracko_cli.py --help
```

#### Authentication

First, authenticate with the backend:

```bash
# Login with username/password
python tracko_cli.py login --username user@example.com --password password

# Or use OAuth token endpoint
python tracko_cli.py oauth-token --phone-no 9999999999 --firebase-uuid your-uuid
```

The CLI automatically saves the authentication token for subsequent commands.

#### Common Commands

```bash
# Health check
python tracko_cli.py health

# Accounts
python tracko_cli.py accounts list
python tracko_cli.py accounts add --name "HDFC Savings"

# Categories
python tracko_cli.py categories list
python tracko_cli.py categories add --name "Food & Dining"

# Transactions
python tracko_cli.py transactions list
python tracko_cli.py transactions add --account-id 1 --category-id 1 --amount 50.0 --type expense --name "Lunch"

# Budget
python tracko_cli.py budget view --month 2 --year 2026
python tracko_cli.py budget allocate --category-id 1 --amount 500.0 --month 2 --year 2026

# Contacts
python tracko_cli.py contacts list
python tracko_cli.py contacts add --name "Alice" --phone "9876543210" --email "alice@example.com"

# Currencies
python tracko_cli.py currencies list
python tracko_cli.py currencies add --code EUR --rate 0.85

# Generic API requests
python tracko_cli.py request --method GET --path /api/health
python tracko_cli.py request --method POST --path /api/contacts --json '{"name":"Bob"}'
```

#### CLI Options

- `--base-url`: Backend API URL (default: http://localhost:8080)
- `--token`: Override saved authentication token
- `--raw`: Print raw API response instead of formatted output

### Database Seeding Script

Tracko includes a comprehensive database seeding script that populates the database with realistic sample data for testing and development.

#### Running the Seeder

```bash
cd tracko-cli
python seed_database.py
```

#### What Gets Seeded

The seeder creates the following sample data:

- **5 Accounts**: HDFC Savings, ICICI Credit Card, Cash Wallet, Paytm Wallet, Investment Account
- **12 Categories**: Food & Dining, Transportation, Shopping, Entertainment, Bills & Utilities, Healthcare, Education, Travel, Investments, Salary, Freelance, Other Income
- **5 Contacts**: Sample contacts with phone numbers and emails
- **90+ Transactions**: Distributed across the previous 3 months from current month
  - Mix of income and expense transactions
  - Realistic amounts and timing
  - Various categories and accounts
- **5-7 Budget Allocations**: Monthly budget allocations for expense categories
- **6 Currency Configurations**: EUR, GBP, JPY, INR, CAD, AUD with exchange rates
- **Sample Split Transactions**: Split expense transactions with contacts

#### Seeder Features

- **Automatic API Health Check**: Waits for backend to be available
- **Authentication**: Uses existing user credentials (user@example.com / password)
- **Error Handling**: Robust error handling with detailed logging
- **Progress Tracking**: Real-time progress updates during data creation
- **Realistic Data**: Transactions distributed across 3 months with proper timing

#### Customization

You can modify `seed_database.py` to:
- Change sample data (names, amounts, categories)
- Adjust the number of entities created
- Modify the time range for transactions
- Add different types of test data

#### Using the CLI After Seeding

After running the seeder, you can use the CLI with the provided token:

```bash
# The seeder outputs the authentication token
python tracko_cli.py --base-url http://localhost:8080 --token <TOKEN> accounts list
python tracko_cli.py --base-url http://localhost:8080 --token <TOKEN> transactions list
python tracko_cli.py --base-url http://localhost:8080 --token <TOKEN> budget view
```

For detailed documentation, see `tracko-cli/README_SEEDING.md`.

## Documentation

- **[Startup Guide](README-STARTUP.md)** - Detailed startup instructions and troubleshooting
- **[Backend Integration Guide](backend/docs/FLUTTER_INTEGRATION_GUIDE.md)** - Complete API documentation
- **[Migration Complete](backend/docs/MIGRATION_COMPLETE.md)** - Backend migration status
- **[CLI Seeding Guide](tracko-cli/README_SEEDING.md)** - Database seeding documentation

## CLI and Database Seeding

Tracko includes a powerful command-line interface (CLI) for API testing and database management, along with a database seeding script.

### Tracko CLI

The CLI provides direct access to all Tracko backend APIs for testing, debugging, and data management.

#### Installation & Setup

The CLI is located in the `tracko-cli/` directory:

```bash
cd tracko-cli
python tracko_cli.py --help
```

#### Authentication

First, authenticate with the backend:

```bash
# Login with username/password
python tracko_cli.py login --username user@example.com --password password

# Or use OAuth token endpoint
python tracko_cli.py oauth-token --phone-no 9999999999 --firebase-uuid your-uuid
```

The CLI automatically saves the authentication token for subsequent commands.

#### Common Commands

```bash
# Health check
python tracko_cli.py health

# Accounts
python tracko_cli.py accounts list
python tracko_cli.py accounts add --name "HDFC Savings"

# Categories
python tracko_cli.py categories list
python tracko_cli.py categories add --name "Food & Dining"

# Transactions
python tracko_cli.py transactions list
python tracko_cli.py transactions add --account-id 1 --category-id 1 --amount 50.0 --type expense --name "Lunch"

# Budget
python tracko_cli.py budget view --month 2 --year 2026
python tracko_cli.py budget allocate --category-id 1 --amount 500.0 --month 2 --year 2026

# Contacts
python tracko_cli.py contacts list
python tracko_cli.py contacts add --name "Alice" --phone "9876543210" --email "alice@example.com"

# Currencies
python tracko_cli.py currencies list
python tracko_cli.py currencies add --code EUR --rate 0.85

# Generic API requests
python tracko_cli.py request --method GET --path /api/health
python tracko_cli.py request --method POST --path /api/contacts --json '{"name":"Bob"}'
```

#### CLI Options

- `--base-url`: Backend API URL (default: http://localhost:8080)
- `--token`: Override saved authentication token
- `--raw`: Print raw API response instead of formatted output

### Database Seeding Script

Tracko includes a comprehensive database seeding script that populates the database with realistic sample data for testing and development.

#### Running the Seeder

```bash
cd tracko-cli
python seed_database.py
```

#### What Gets Seeded

The seeder creates the following sample data:

- **5 Accounts**: HDFC Savings, ICICI Credit Card, Cash Wallet, Paytm Wallet, Investment Account
- **12 Categories**: Food & Dining, Transportation, Shopping, Entertainment, Bills & Utilities, Healthcare, Education, Travel, Investments, Salary, Freelance, Other Income
- **5 Contacts**: Sample contacts with phone numbers and emails
- **90+ Transactions**: Distributed across the previous 3 months from current month
  - Mix of income and expense transactions
  - Realistic amounts and timing
  - Various categories and accounts
- **5-7 Budget Allocations**: Monthly budget allocations for expense categories
- **6 Currency Configurations**: EUR, GBP, JPY, INR, CAD, AUD with exchange rates
- **Sample Split Transactions**: Split expense transactions with contacts

#### Seeder Features

- **Automatic API Health Check**: Waits for backend to be available
- **Authentication**: Uses existing user credentials (user@example.com / password)
- **Error Handling**: Robust error handling with detailed logging
- **Progress Tracking**: Real-time progress updates during data creation
- **Realistic Data**: Transactions distributed across 3 months with proper timing

#### Customization

You can modify `seed_database.py` to:
- Change sample data (names, amounts, categories)
- Adjust the number of entities created
- Modify the time range for transactions
- Add different types of test data

#### Using the CLI After Seeding

After running the seeder, you can use the CLI with the provided token:

```bash
# The seeder outputs the authentication token
python tracko_cli.py --base-url http://localhost:8080 --token <TOKEN> accounts list
python tracko_cli.py --base-url http://localhost:8080 --token <TOKEN> transactions list
python tracko_cli.py --base-url http://localhost:8080 --token <TOKEN> budget view
```

For detailed documentation, see `tracko-cli/README_SEEDING.md`.

## License

See individual component repositories for license information.

## Troubleshooting

### Port 8080 already in use
- Use `task stop` to cleanly stop all services
- If issues persist, on Windows:
  - Find listeners: `powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort 8080 -State Listen | Select OwningProcess"`
  - Kill process: `taskkill /T /PID <PID> /F`

### Flutter hot reload not working
- Make sure you're in an interactive run (`flutter run`), not `flutter run --machine`.
- Use `task flutter` to start Flutter UI with proper configuration.

### Backend fails to start
- Check Java version: `java -version` (should be 17+)
- Verify MAVEN_HOME is set: `mvn -version`
- Clean and rebuild: `task clean && task install`

### Database connection issues
- For development, the backend uses H2 in-memory database (no setup required)
- For production, configure PostgreSQL in `application.properties`
- Check environment variables are set correctly

### Docker issues
- Ensure Docker Desktop is running
- Check Docker Compose version: `docker-compose --version`
- Rebuild images: `docker-compose build --no-cache`
