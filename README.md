# Tracko - Expense Manager

A comprehensive expense management application with Flutter mobile UI and dual backend (Java + Python with ML).

## Project Structure

- **Tracko-Flutter-UI**: Flutter mobile application
- **Tracko-Java-Backend**: Spring Boot REST API backend
- **Tracko-Python-Backend**: Django backend with ML-powered expense categorization

## Getting Started

### Prerequisites
- Flutter SDK 3.0+
- Java 8+
- Python 3.8+
- PostgreSQL (for local development)

### Setup Instructions

1. **Flutter UI Setup**
   ```bash
   cd Tracko-Flutter-UI
   flutter pub get
   flutter run
   ```

2. **Java Backend Setup**
   ```bash
   cd Tracko-Java-Backend
   mvn clean install -P local
   mvn spring-boot:run -P local
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
