# Tracko Integration Tests

This directory contains integration tests for the Tracko expense management app, specifically testing the backend-driven authentication and data flow.

## Prerequisites

1. **Backend Server Running**: Ensure the Spring Boot backend is running on `http://localhost:8080`
   ```bash
   cd Tracko-Java-Backend
   mvn spring-boot:run
   ```

2. **Flutter Dependencies**: Install all required packages
   ```bash
   flutter pub get
   ```

3. **ChromeDriver (for web tests)**: Download and run ChromeDriver
   - Download from: https://googlechromelabs.github.io/chrome-for-testing/
   - Match your Chrome browser version
   - Run: `chromedriver --port=4444`

## Running Integration Tests

### On Chrome (Web) - Recommended Method

**Important**: Use `flutter drive` command, not `flutter test`

```bash
# Start ChromeDriver first (in a separate terminal)
chromedriver --port=4444

# Then run the integration tests
flutter drive --driver=test_driver/integration_test.dart --target=integration_test/app_test.dart -d chrome
```

### On Mobile Device/Emulator
```bash
# Android
flutter drive --driver=test_driver/integration_test.dart --target=integration_test/app_test.dart -d <device-id>

# iOS
flutter drive --driver=test_driver/integration_test.dart --target=integration_test/app_test.dart -d <device-id>
```

### List Available Devices
```bash
flutter devices
```

## Test Coverage

The integration tests verify the following flows:

### 1. Authentication Bypass Flow
- ✅ Navigates to phone login page
- ✅ Enters all-zero phone number (0000000000)
- ✅ Verifies bypass authentication works
- ✅ Confirms navigation to setup page
- ✅ Validates bypass mode indicator is shown

### 2. User Setup with Backend Integration
- ✅ Completes authentication bypass
- ✅ Enters user name and email on setup page
- ✅ Submits form to create global account via backend API
- ✅ Verifies backend API call succeeds
- ✅ Confirms navigation to home page

### 3. Session Persistence
- ✅ Completes full authentication and setup flow
- ✅ Verifies user session is maintained in memory
- ✅ Confirms user remains logged in

### 4. Backend API Connectivity
- ✅ Tests communication with Spring Boot backend
- ✅ Verifies API endpoints are accessible
- ✅ Validates data persistence through backend

## Test Data

All tests use the bypass phone number: **0000000000**

This allows testing without Firebase authentication, making tests:
- Faster to execute
- Independent of external services
- Reliable and repeatable

## Expected Backend Endpoints

The tests expect the following backend endpoints to be available:

- `POST /api/signUp` - User registration
- `POST /api/oauth/token` - Authentication token
- `POST /api/account/create` - Create global account
- `POST /api/user/save` - Update user profile
- `GET /api/user/byPhoneNo` - Get user by phone number

## Troubleshooting

### Backend Connection Errors
If tests fail with connection errors:
1. Verify backend is running: `curl http://localhost:8080/api/signUp`
2. Check CORS configuration is enabled
3. Ensure PostgreSQL database is accessible

### Test Timeout
If tests timeout:
1. Increase wait durations in test file
2. Check backend response times
3. Verify network connectivity

### Widget Not Found
If tests fail to find widgets:
1. Run app manually to verify UI flow
2. Update widget finders in test file
3. Check for UI changes in app code

## CI/CD Integration

To run tests in CI/CD pipeline:

```yaml
# Example GitHub Actions
- name: Run Integration Tests
  run: |
    flutter pub get
    flutter test integration_test/app_test.dart -d chrome
```

## Notes

- Tests are designed to work on both **mobile** and **web** platforms
- All data persistence is handled by the backend (no local SQLite)
- Tests verify the complete user journey from authentication to home page
- Backend must be running before executing tests
