#!/usr/bin/env bash
set -euo pipefail

# Tracko Integration Test Automation (Bash)
# - Starts backend (Spring Boot)
# - Waits for health (/api/health)
# - Starts ChromeDriver
# - Runs Flutter integration tests (flutter drive)
# - Captures logs under ./logs
# - Cleans up on exit, errors, or Ctrl+C

# -------- Config --------
BACKEND_DIR="C:/Users/soura/Documents/Personal/Tracko/backend"
FLUTTER_DIR="C:/Users/soura/Documents/Personal/Tracko/frontend"
LOGS_DIR="C:/Users/soura/Documents/Personal/Tracko/logs"
BACKEND_PORT=${BACKEND_PORT:-8080}
CHROMEDRIVER_PORT=${CHROMEDRIVER_PORT:-4444}
FORCE_KILL_8080=${FORCE_KILL_8080:-1}  # 1=yes, 0=no
KEEP_BACKEND_RUNNING=${KEEP_BACKEND_RUNNING:-0}
SKIP_BACKEND_START=${SKIP_BACKEND_START:-0}

TS=$(date +"%Y-%m-%d_%H-%M-%S")
BACKEND_LOG="$LOGS_DIR/backend_$TS.log"
CHROMEDRIVER_LOG="$LOGS_DIR/chromedriver_$TS.log"
TEST_LOG="$LOGS_DIR/integration_test_$TS.log"

mkdir -p "$LOGS_DIR"

# -------- State --------
BACKEND_PID=""
CHROMEDRIVER_PID=""
BACKEND_STARTED_BY_SCRIPT=0

# -------- Helpers --------
log() { printf "%s\n" "$*"; }
logh() { printf "\033[36m%s\033[0m\n" "$*"; }
logok() { printf "\033[32m%s\033[0m\n" "$*"; }
logwarn() { printf "\033[33m%s\033[0m\n" "$*"; }
logerr() { printf "\033[31m%s\033[0m\n" "$*"; }

kill_pid() {
  local pid="$1"
  if [[ -z "$pid" ]]; then return; fi

  if command -v taskkill >/dev/null 2>&1; then
    # Windows: Use taskkill with /T to kill the process tree
    taskkill //F //T //PID "$pid" >/dev/null 2>&1 || true
  else
    # Unix-like: Find and kill all child processes recursively
    local children
    children=$(pgrep -P "$pid" || true)
    for child in $children; do
      kill_pid "$child"
    done
    # Finally, kill the parent
    if kill -0 "$pid" 2>/dev/null; then
      kill -9 "$pid" 2>/dev/null || true
    fi
  fi
}

# Kill all listeners on a port using best-effort cross-env commands
kill_port() {
  local port="$1"
  log "[CLEANUP] Attempting to free port $port ..."
  # Linux/macOS: fuser or lsof
  if command -v fuser >/dev/null 2>&1; then
    fuser -k "$port"/tcp 2>/dev/null || true
  fi
  if command -v lsof >/dev/null 2>&1; then
    lsof -ti:"$port" 2>/dev/null | xargs -r kill -9 2>/dev/null || true
  fi
  # Windows Git Bash fallback with netstat + taskkill
  if command -v netstat >/dev/null 2>&1; then
    local pids
    # Gather PIDs; ensure the pipeline never causes set -e to exit when no matches
    pids=$( { netstat -ano 2>/dev/null | tr -s ' ' | grep ":$port " | awk '{print $NF}' | sort -u; } || true )
    for p in $pids; do
      if [[ "$p" =~ ^[0-9]+$ ]] && [[ "$p" != "0" ]]; then
        if command -v taskkill >/dev/null 2>&1; then
          taskkill //F //PID "$p" >/dev/null 2>&1 || true
        else
          kill -9 "$p" >/dev/null 2>&1 || true
        fi
      fi
    done
  fi
}

port_free() {
  local port="$1"
  if command -v netstat >/dev/null 2>&1; then
    if netstat -ano 2>/dev/null | grep -q ":$port "; then
      # If any LISTENING remains, consider busy
      if netstat -ano 2>/dev/null | grep ":$port " | grep -q LISTENING; then
        return 1
      fi
    fi
  fi
  return 0
}

health_ok() {
  curl -fsS "http://localhost:$BACKEND_PORT/api/health" >/dev/null 2>&1
}

cleanup() {
  log ""
  logwarn "[CLEANUP] Stopping services ..."
  if [[ -n "$CHROMEDRIVER_PID" ]]; then
    kill_pid "$CHROMEDRIVER_PID"
  fi
  if [[ "$BACKEND_STARTED_BY_SCRIPT" -eq 1 && -n "$BACKEND_PID" ]]; then
    kill_pid "$BACKEND_PID"
  fi
  # Ensure port is freed
  kill_port "$BACKEND_PORT"
  logok "[CLEANUP] Done"
}
trap cleanup EXIT INT TERM

logh "========================================"
logh "Tracko Integration Test Automation (Bash)"
logh "========================================"
log ""

# -------- Step 1: Backend --------
logh "[STEP 1/4] Backend Server"
log "Checking /api/health on port $BACKEND_PORT ..."

if health_ok; then
  logok "[OK] Backend is already running"
else
  logwarn "[INFO] Backend not healthy or not running"
fi

if [[ "$FORCE_KILL_8080" == "1" && "$SKIP_BACKEND_START" == "0" ]]; then
  kill_port "$BACKEND_PORT"
  # wait up to 10s
  for i in {1..10}; do
    if port_free "$BACKEND_PORT"; then
      logok "[OK] Port $BACKEND_PORT is free"
      break
    fi
    sleep 1
  done
fi

if [[ "$SKIP_BACKEND_START" == "0" ]]; then
  log "Starting backend in $BACKEND_DIR ..."
  pushd "$BACKEND_DIR" >/dev/null || exit 1
  # Build mvn args (allow custom server.port)
  MVN_ARGS=(spring-boot:run)
  if [[ "$BACKEND_PORT" != "8080" ]]; then
    MVN_ARGS+=("-Dspring-boot.run.arguments=--server.port=$BACKEND_PORT")
  fi
  # Decide Maven binary depending on platform
  MVN_BIN="mvn"
  if command -v powershell.exe >/dev/null 2>&1 || command -v cmd.exe >/dev/null 2>&1; then
    # On Windows, prefer mvn.cmd
    MVN_BIN="mvn.cmd"
  fi
  # Start backend and redirect logs
  # Prefer PowerShell on Windows to launch detached process and capture real PID
    if command -v powershell.exe >/dev/null 2>&1; then
    # Use cmd.exe inside PowerShell to handle redirection, then get PID
    BACKEND_PID=$(powershell.exe -NoProfile -Command "\$p = Start-Process -FilePath cmd.exe -ArgumentList '/c', 'mvn.cmd spring-boot:run > \"$BACKEND_LOG\" 2>&1' -WindowStyle Hidden -PassThru; Write-Output \$p.Id" | tr -d '\r')
  elif command -v cmd.exe >/dev/null 2>&1; then
    # Fallback: run via cmd; PID may be of the launcher, so we will rely on health check and port killer
    if [[ "${#MVN_ARGS[@]}" -gt 1 ]]; then
      # Need to join args for cmd; pass quoted
      cmd.exe /c "start /B cmd /c $MVN_BIN ${MVN_ARGS[*]} ^> \"$BACKEND_LOG\" 2^>^&1"
    else
      cmd.exe /c "start /B cmd /c $MVN_BIN spring-boot:run ^> \"$BACKEND_LOG\" 2^>^&1"
    fi
    # No reliable PID; leave BACKEND_PID empty
    BACKEND_PID=""
  else
    # Unix-like
    nohup "$MVN_BIN" "${MVN_ARGS[@]}" >>"$BACKEND_LOG" 2>&1 &
    BACKEND_PID=$!
  fi
  popd >/dev/null || true
  BACKEND_STARTED_BY_SCRIPT=1
  log "Backend launcher PID: ${BACKEND_PID:-unknown}"
  if [[ -z "$BACKEND_PID" ]]; then
    logwarn "[WARN] Backend PID not captured (launcher is detached on Windows). Relying on health check and log file."
  fi

  # Wait for health up to 120s
  log "Waiting for backend health ... logs: $BACKEND_LOG"
  for i in {1..40}; do
    if health_ok; then
      logok "[OK] Backend is ready!"
      break
    fi
    # Print the last few backend log lines to aid diagnostics every 9 seconds
    if (( i % 3 == 0 )); then
      log "--- tail backend log (last 10 lines) ---"
      tail -n 10 "$BACKEND_LOG" || true
      log "----------------------------------------"
    fi
    log "Still waiting for /api/health ... ($((i*3))/120 sec)"
    sleep 3
  done
  if ! health_ok; then
    logerr "[ERROR] Backend failed to become healthy in time"
    log "Full backend log at: $BACKEND_LOG"
    exit 1
  fi
else
  logwarn "[SKIPPED] Backend startup skipped by user"
fi

# -------- Step 2: Flutter deps --------
log ""
logh "[STEP 2/4] Flutter Dependencies"
(
  cd "$FLUTTER_DIR" || exit 1
  flutter pub get
) | tee -a "$TEST_LOG"

# -------- Step 3: ChromeDriver --------
log ""
logh "[STEP 3/4] ChromeDriver"
if command -v chromedriver >/dev/null 2>&1; then
  log "Starting ChromeDriver on port $CHROMEDRIVER_PORT ... logs: $CHROMEDRIVER_LOG"
  chromedriver --port=$CHROMEDRIVER_PORT >>"$CHROMEDRIVER_LOG" 2>&1 &
  CHROMEDRIVER_PID=$!
  sleep 2
  logok "[OK] ChromeDriver PID: $CHROMEDRIVER_PID"
else
  logerr "[ERROR] chromedriver not found in PATH."
  log "Please install ChromeDriver and add it to your system's PATH."
  log "Download from: https://googlechromelabs.github.io/chrome-for-testing/"
  exit 1
fi

# -------- Step 4: Integration tests --------
log ""
logh "[STEP 4/4] Integration Tests"
log "Test logs: $TEST_LOG"
(
  cd "$FLUTTER_DIR" || exit 1
  flutter drive --driver=test_driver/integration_test.dart --target=integration_test/app_test.dart -d chrome
) 2>&1 | tee -a "$TEST_LOG"
TEST_RESULT=${PIPESTATUS[0]:-0}

# -------- Summary --------
if [[ "$KEEP_BACKEND_RUNNING" != "1" ]]; then
  cleanup
  # Remove EXIT trap cleanup already called
  trap - EXIT INT TERM
else
  # Stop ChromeDriver only
  if [[ -n "$CHROMEDRIVER_PID" ]]; then kill_pid "$CHROMEDRIVER_PID"; fi
fi

log ""
logh "========================================"
if [[ "$TEST_RESULT" -eq 0 ]]; then
  logok "[SUCCESS] All integration tests passed!"
else
  logerr "[FAILED] Some tests failed (Exit code: $TEST_RESULT)"
fi
log "Logs saved to: $LOGS_DIR"
logh "========================================"

exit "$TEST_RESULT"
