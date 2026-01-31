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
    echo -e "${RED}ERROR: JAVA_HOME is not set.${NC}"
    echo "Please set the JAVA_HOME environment variable to point to your JDK 17+ installation."
    exit 1
fi

JAVA_CMD="$JAVA_HOME/bin/java"
if [ ! -x "$JAVA_CMD" ]; then
    echo -e "${RED}ERROR: Java executable not found or not executable at '$JAVA_CMD'.${NC}"
    echo "Please ensure your JAVA_HOME is set correctly."
    exit 1
fi

JAVA_VERSION=$("$JAVA_CMD" -version 2>&1 | awk -F '"' '/version/ {print $2}')
MAJOR_VERSION=$(echo "$JAVA_VERSION" | cut -d. -f1)
echo "Detected JAVA_HOME version: $JAVA_VERSION"

if (( MAJOR_VERSION < 17 )); then
    echo -e "${RED}ERROR: Java 17 or newer is required by Maven, but JAVA_HOME points to version $MAJOR_VERSION.${NC}"
    echo "Please set your JAVA_HOME environment variable to a JDK 17+ installation."
    exit 1
fi

echo "Current PATH: $PATH"

if ! command -v mvn &> /dev/null; then
    echo -e "${RED}ERROR: 'mvn' command not found. Please ensure Maven is installed and its 'bin' directory is in your PATH.${NC}"
    exit 1
fi

if ! command -v flutter &> /dev/null; then
    echo -e "${RED}ERROR: 'flutter' command not found. Please ensure the Flutter SDK is installed and its 'bin' directory is in your PATH.${NC}"
    exit 1
fi
echo "All dependencies found."
# -------------------------

# Array to hold background process PIDs
declare -a PIDS

# Function to convert a Bash path (/c/Users/...) to a Windows path (C:\Users\...)
# for use with native commands like mvn.cmd
to_win_path() {
    if [[ -z "$1" ]]; then return; fi
    local path="$1"
    # Check if we are likely in a Git Bash/MSYS environment on Windows
    if [[ "$path" == /?/* && "$OSTYPE" == "msys"* ]]; then
        # /c/path -> c:/path
        path="$(echo "$path" | sed -e 's|^/\([a-zA-Z]\)/|\1:/|')"
        # c:/path -> c:\path
        path="$(echo "$path" | sed 's|/|\\|g')"
    fi
    echo "$path"
}

# Function to clean up background processes
cleanup() {
    echo -e "\n${RED}Caught exit signal... Shutting down background processes.${NC}"
    for pid in "${PIDS[@]}"; do
        echo "Killing process $pid"
        # Use taskkill for Windows compatibility, check if command exists
        if command -v taskkill &> /dev/null; then
            taskkill //PID $pid //F
        else
            kill "$pid"
        fi
    done
    echo "Cleanup complete."
    exit 0
}

# Trap EXIT, SIGINT, and SIGTERM signals to run the cleanup function
trap cleanup EXIT SIGINT TERM

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

# --- Log and Path Config ---
# Get the absolute path of the script's directory
SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
LOGS_DIR="$SCRIPT_DIR/logs"
TS=$(date +"%Y-%m-%d_%H-%M-%S")
BACKEND_LOG="$LOGS_DIR/backend_$TS.log"
FLUTTER_LOG="$LOGS_DIR/flutter_$TS.log"

mkdir -p "$LOGS_DIR"

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

# Start Backend Server
echo -e "${YELLOW}[1/2] Starting Spring Boot Backend...${NC}"
echo "Redirecting backend logs to: $BACKEND_LOG"
# Convert log path for Windows
WIN_BACKEND_LOG=$(to_win_path "$BACKEND_LOG")
(cd "$backend_path" && echo -e "${YELLOW}Force cleaning target directory...${NC}" && rm -rf target && echo -e "${GREEN}Starting Maven with default 'dev' profile...${NC}" && mvn spring-boot:run > "$WIN_BACKEND_LOG" 2>&1) &
PIDS+=($!)

# Wait for backend to initialize
echo -e "${YELLOW}Waiting 30 seconds for backend to start...${NC}"
sleep 30

# Check if backend is running
if curl -s --head --request GET http://localhost:8080 | grep "200 OK" > /dev/null; then
    echo -e "${GREEN}Backend is running!${NC}"
else
    echo -e "${YELLOW}WARNING: Backend may not be fully started yet. Continuing anyway...${NC}"
fi

# Start Flutter UI
echo -e "${YELLOW}[2/2] Starting Flutter UI...${NC}"
echo "Redirecting Flutter logs to: $FLUTTER_LOG"
# Convert log path for Windows
WIN_FLUTTER_LOG=$(to_win_path "$FLUTTER_LOG")
(cd "$flutter_path" && echo -e "${GREEN}Starting Flutter on Chrome...${NC}" && flutter run -d chrome > "$WIN_FLUTTER_LOG" 2>&1) &
PIDS+=($!)

echo ""
echo -e "${CYAN}========================================${NC}"
echo -e "${GREEN}Both services are starting!${NC}"
echo -e "${CYAN}========================================${NC}"
echo -e "${WHITE}Backend: http://localhost:8080${NC}"
echo -e "${WHITE}Flutter: Check the Flutter UI window${NC}"
echo ""

echo "Press Ctrl+C to exit and kill all processes..."
# Wait indefinitely to keep the script running. The trap will handle the exit.
wait
