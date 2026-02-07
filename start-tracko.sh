#!/bin/bash

# Source shell configuration to load PATH
if [ -f "$HOME/.bash_profile" ]; then
    . "$HOME/.bash_profile"
fi

if [ -f "$HOME/.bashrc" ]; then
    . "$HOME/.bashrc"
fi

# Tracko Application Startup Script
# Bash version with process cleanup

# --- Dependency Checks ---
echo "Checking for dependencies..."

# Check JAVA_HOME and Java Version for Maven
if [ -z "$JAVA_HOME" ]; then
    echo "ERROR: JAVA_HOME is not set."
    echo "Please set the JAVA_HOME environment variable to point to your JDK 17+ installation."
    exit 1
fi

JAVA_CMD="$JAVA_HOME/bin/java"
if [ ! -x "$JAVA_CMD" ]; then
    echo "ERROR: Java executable not found or not executable at '$JAVA_CMD'."
    echo "Please ensure your JAVA_HOME is set correctly."
    exit 1
fi

JAVA_VERSION=$("$JAVA_CMD" -version 2>&1 | awk -F '"' '/version/ {print $2}')
MAJOR_VERSION=$(echo "$JAVA_VERSION" | cut -d. -f1)
echo "Detected JAVA_HOME version: $JAVA_VERSION"

if (( MAJOR_VERSION < 17 )); then
    echo "ERROR: Java 17 or newer is required by Maven, but JAVA_HOME points to version $MAJOR_VERSION."
    echo "Please set your JAVA_HOME environment variable to a JDK 17+ installation."
    exit 1
fi

echo "Current PATH: $PATH"

if ! command -v mvn &> /dev/null; then
    echo "ERROR: 'mvn' command not found. Please ensure Maven is installed and its 'bin' directory is in your PATH."
    exit 1
fi

if ! command -v flutter &> /dev/null; then
    echo "ERROR: 'flutter' command not found. Please ensure the Flutter SDK is installed and its 'bin' directory is in your PATH."
    exit 1
fi
echo "All dependencies found."
# -------------------------

# Array to hold background process PIDs (no longer used with concurrently)
:

kill_port() {
    local port="$1"
    if command -v powershell.exe &> /dev/null; then
        local pids=$(powershell.exe -NoProfile -Command "Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue | Select-Object -Expand OwningProcess" | tr -d '\r')
        for pid in $pids; do
            if [[ "$pid" =~ ^[0-9]+$ ]]; then
                MSYS2_ARG_CONV_EXCL='*' taskkill /T /PID "$pid" /F 2>/dev/null || true
            fi
        done
    else
        local pids=$(netstat -ano 2>/dev/null | grep ":$port " | awk '{print $5}' | sort -u)
        for pid in $pids; do
            if [[ "$pid" =~ ^[0-9]+$ ]]; then
                MSYS2_ARG_CONV_EXCL='*' taskkill /T /PID "$pid" /F 2>/dev/null || kill "$pid" 2>/dev/null || true
            fi
        done
    fi
}

# No trap needed; concurrently will handle signal propagation

# Colors
CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
WHITE='\033[1;37m'
GRAY='\033[0;90m'
NC='\033[0m' # No Color

echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}Starting Tracko Application${NC}"
echo -e "${CYAN}========================================${NC}"
echo ""

# --- Path Config ---
SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)

backend_path="./Tracko-Java-Backend"
flutter_path="./Tracko-Flutter-UI"

# Check if paths exist
if [ ! -d "$backend_path" ]; then
    echo -e "${RED}ERROR: Backend path not found: $backend_path${NC}"
    exit 1
fi

if [ ! -d "$flutter_path" ]; then
    echo -e "${RED}ERROR: Flutter path not found: $flutter_path${NC}"
    exit 1
fi

# Start both services using npx concurrently
echo -e "${YELLOW}Ensuring port 8080 is free...${NC}"
kill_port 8080 2>/dev/null || true

# Check for npx (Node.js) to run concurrently
if ! command -v npx &> /dev/null; then
    echo -e "${RED}ERROR: 'npx' not found. Please install Node.js (which provides npx) or add it to PATH.${NC}"
    exit 1
fi

echo -e "${YELLOW}Starting Backend and Flutter UI with concurrently...${NC}"
echo -e "${GRAY}Logs will be shown inline with labels. Press Ctrl+C to stop all.${NC}"

BACK_CMD="cd \"$backend_path\" && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
FRONT_CMD="cd \"$flutter_path\" && flutter run -d chrome"

# Use exec so signals pass through to child processes managed by concurrently
exec npx --yes concurrently \
  -n backend,flutter \
  -c "bgBlue.bold,bgMagenta.bold" \
  -k \
  --handle-input \
  --raw \
  "$BACK_CMD" \
  "$FRONT_CMD"
