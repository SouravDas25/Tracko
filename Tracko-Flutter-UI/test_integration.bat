@echo off
echo ========================================
echo Tracko Integration Test Runner
echo ========================================
echo.

echo Checking if backend is running...
curl -s http://localhost:8080/api/signUp > nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Backend is not running on http://localhost:8080
    echo Please start the backend server first:
    echo   cd Tracko-Java-Backend
    echo   mvn spring-boot:run
    echo.
    pause
    exit /b 1
)

echo [OK] Backend is running
echo.

echo Installing dependencies...
call flutter pub get
if %errorlevel% neq 0 (
    echo [ERROR] Failed to install dependencies
    pause
    exit /b 1
)

echo.
echo Running integration tests on Chrome...
echo.
call flutter test integration_test/app_test.dart -d chrome

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo [SUCCESS] All tests passed!
    echo ========================================
) else (
    echo.
    echo ========================================
    echo [FAILED] Some tests failed
    echo ========================================
)

echo.
pause
