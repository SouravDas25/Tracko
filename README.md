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
