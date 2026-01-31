# Tracko Startup Scripts

This folder contains scripts to easily start both the backend and Flutter UI together.

## Available Scripts

### 1. Batch Script (Windows) - `start-tracko.bat`
**Recommended for most users**

Double-click the file or run from command prompt:
```cmd
start-tracko.bat
```

**What it does:**
- Opens a new window for the Spring Boot backend
- Waits 30 seconds for backend to initialize
- Opens a new window for the Flutter UI
- Both services run in separate windows

### 2. PowerShell Script - `start-tracko.ps1`
**Advanced option with better error handling**

Right-click and select "Run with PowerShell" or run from PowerShell:
```powershell
.\start-tracko.ps1
```

**What it does:**
- Validates paths exist
- Starts backend in new PowerShell window
- Checks if backend is responding
- Starts Flutter UI in new PowerShell window
- Color-coded output for better visibility

**Note:** You may need to enable script execution:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

## Manual Startup (Alternative)

If you prefer to start services manually:

### Terminal 1 - Backend
```cmd
cd C:\Users\soura\Documents\Personal\Tracko\Tracko-Java-Backend
mvn spring-boot:run
```

### Terminal 2 - Flutter UI
```cmd
cd C:\Users\soura\Documents\Personal\Tracko\Tracko-Flutter-UI
flutter run
```

## Troubleshooting

### Backend takes longer to start
- Edit the script and increase the wait time from 30 to 60 seconds
- In batch file: change `timeout /t 30` to `timeout /t 60`
- In PowerShell: change `Start-Sleep -Seconds 30` to `Start-Sleep -Seconds 60`

### Flutter doesn't find backend
- Make sure backend is fully started (check backend window for "Started Application")
- Backend should be accessible at `http://localhost:8080`
- For Android emulator, the app uses `http://10.0.2.2:8080`

### Ports already in use
- Backend uses port 8080
- Check if another service is using this port
- Stop the conflicting service or change backend port in `application.properties`

## Service URLs

- **Backend API:** http://localhost:8080
- **Backend Swagger (if enabled):** http://localhost:8080/swagger-ui.html
- **Flutter App:** Runs on connected device/emulator

## Stopping Services

To stop the services:
1. Close the backend window (or press Ctrl+C)
2. Close the Flutter window (or press 'q' in the Flutter console)

## First Time Setup

Before running these scripts for the first time:

1. **Backend:** Ensure Maven is installed and configured
2. **Flutter:** Ensure Flutter SDK is installed and in PATH
3. **Database:** Backend will auto-create H2 database on first run
4. **Dependencies:** Run `mvn clean install` in backend folder once

## Need Help?

Check the migration documentation:
- `BACKEND_MIGRATION_COMPLETE.md` - Backend API details
- `UI_MIGRATION_COMPLETE.md` - Flutter UI changes
