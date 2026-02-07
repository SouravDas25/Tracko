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
    # Restore terminal settings if modified
    if [ -n "$STTY_ORIG" ]; then
        stty "$STTY_ORIG" 2>/dev/null || true
    fi
    # Try to gracefully ask Flutter to quit
    if [[ -n "${FLUTTER_RUN[1]}" ]]; then
        printf 'q' >&"${FLUTTER_RUN[1]}" 2>/dev/null || true
        # Give Flutter a moment to exit cleanly to avoid pipe errors
        if [[ -n "$FLUTTER_PID" ]]; then
            for i in {1..25}; do
                if ! kill -0 "$FLUTTER_PID" 2>/dev/null; then
                    break
                fi
                sleep 0.2
            done
        fi
    fi
    for pid in "${PIDS[@]}"; do
        # Skip empty or non-numeric PIDs
        if ! [[ "$pid" =~ ^[0-9]+$ ]]; then continue; fi
        echo "Killing process $pid"
        # Use taskkill for Windows compatibility, check if command exists
        if command -v taskkill &> /dev/null; then
            MSYS2_ARG_CONV_EXCL='*' taskkill /T /PID "$pid" /F > /dev/null 2>&1 || true
        else
            kill "$pid"
        fi
    done
    kill_port 8080 2>/dev/null || true
    echo "Cleanup complete."
    exit 0
}

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
# Redirect logs using POSIX path directly; enable Spring Boot DevTools restart if present
(echo "Ensuring port 8080 is free..."; kill_port 8080 2>/dev/null || true)
(cd "$backend_path" && echo -e "${GREEN}Starting Maven (dev profile)...${NC}" && mvn spring-boot:run -Dspring-boot.run.profiles=dev > "$BACKEND_LOG" 2>&1) &
BACKEND_PID=$!
PIDS+=($BACKEND_PID)

# Wait for backend to initialize
echo -e "${YELLOW}Waiting 30 seconds for backend to start...${NC}"
sleep 30

# Check if backend is running
if curl -s --head --request GET http://localhost:8080 | grep "200 OK" > /dev/null; then
    echo -e "${GREEN}Backend is running!${NC}"
else
    echo -e "${YELLOW}WARNING: Backend may not be fully started yet. Continuing anyway...${NC}"
fi

# Start Flutter UI as a coprocess to allow sending hot-reload commands programmatically
echo -e "${YELLOW}[2/2] Starting Flutter UI (hot reload enabled)...${NC}"
echo -e "${GRAY}Tip: Press 'r' here to hot reload Flutter, 'b' to hot reload backend, 'q' to quit.${NC}"
coproc FLUTTER_RUN { (cd "$flutter_path" && echo -e "${GREEN}Starting Flutter on Chrome...${NC}" && flutter run -d chrome); }
FLUTTER_PID=$!
PIDS+=($FLUTTER_PID)

echo ""
echo -e "${CYAN}========================================${NC}"
echo -e "${GREEN}Both services are starting!${NC}"
echo -e "${CYAN}========================================${NC}"
echo -e "${WHITE}Backend: http://localhost:8080${NC}"
echo -e "${WHITE}Flutter: Check the Flutter UI window${NC}"
echo ""

echo "Interactive controls: [r]=Flutter hot reload, [b]=Backend restart, [q]=Quit"

# Save and adjust terminal for single-key input
STTY_ORIG=$(stty -g 2>/dev/null || true)
stty -echo -icanon min 1 time 0 2>/dev/null || true

# Key listener loop
while true; do
    if read -rsn1 key 2>/dev/null; then
        case "$key" in
            r)
                # Send hot reload to Flutter
                if [[ -n "${FLUTTER_RUN[1]}" ]]; then
                    printf 'r' >&"${FLUTTER_RUN[1]}" 2>/dev/null || true
                    echo -e "${GRAY}[Flutter] Hot reload triggered.${NC}"
                else
                    echo -e "${YELLOW}[Flutter] Input pipe unavailable.${NC}"
                fi
                ;;
            b)
                echo -e "${GRAY}[Backend] Restarting backend...${NC}"
                if [[ -n "$BACKEND_PID" ]] && kill -0 "$BACKEND_PID" 2>/dev/null; then
                    if command -v taskkill &> /dev/null; then
                        MSYS2_ARG_CONV_EXCL='*' taskkill /T /PID "$BACKEND_PID" /F 2>/dev/null || true
                    else
                        kill "$BACKEND_PID" 2>/dev/null || true
                    fi
                fi
                kill_port 8080 2>/dev/null || true
                (cd "$backend_path" && mvn spring-boot:run -Dspring-boot.run.profiles=dev >> "$BACKEND_LOG" 2>&1) &
                BACKEND_PID=$!
                PIDS[0]="$BACKEND_PID"
                echo -e "${GREEN}[Backend] Restarted (pid $BACKEND_PID).${NC}"
                ;;
            q)
                echo "Quitting..."
                break
                ;;
            *)
                # ignore other keys
                ;;
        esac
    else
        # If either process exits, we can exit the loop
        if ! kill -0 ${PIDS[0]} 2>/dev/null; then break; fi
        if ! kill -0 $FLUTTER_PID 2>/dev/null; then break; fi
        sleep 0.2
    fi
done

# Restore terminal before exiting main script; cleanup trap will kill processes
if [ -n "$STTY_ORIG" ]; then
    stty "$STTY_ORIG" 2>/dev/null || true
fi
exit 0
