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
- Windows users: Git Bash (recommended for running the start script)

### Quick start (recommended)

Run both backends and the Flutter UI with hot reload from the repo root:

```bash
./start-tracko.sh
```

Hot keys in the terminal:

- r — Flutter hot reload
- b — Backend restart (Spring Boot DevTools)
- q — Quit (cleans up all processes)

Logs are written to the `logs/` folder.

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

3. **Python Backend Setup**
   ```bash
   cd Tracko-Python-Backend
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

- 📱 Cross-platform mobile app (Flutter)
- 💰 Expense tracking and categorization
- 📊 Visual analytics and charts
- 🤖 ML-powered smart categorization
- 🔐 Secure authentication with JWT
- ☁️ Cloud deployment ready (Heroku, SAP Cloud Platform)

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

- Port 8080 already in use
  - The start script attempts to free port 8080 automatically on start/exit. If you still see this, on Windows:
    - Find listeners: `powershell -NoProfile -Command "Get-NetTCPConnection -LocalPort 8080 -State Listen | Select OwningProcess"`
    - Kill process: `taskkill /T /PID <PID> /F`
- Flutter hot reload commands not working
  - Make sure you are running the start script in a terminal and the terminal window has focus when pressing keys.
  - If you started Flutter separately, ensure you’re in an interactive run (`flutter run`), not `flutter run --machine`.
