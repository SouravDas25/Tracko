#!/bin/bash
# Script to start backend, generate SDK, and stop backend
# Platforms: macOS, Linux
# Usage: bash scripts/generate-sdk.sh

set -e

# Configuration
BACKEND_DIR="backend"
OPENAPI_URL="http://localhost:8080/v3/api-docs"
HEALTH_URL="http://localhost:8080/api/health"
OPENAPI_SPEC="openapi.json"
SDK_OUT_DIR="sdk"
GENERATOR_JAR="tools/openapi-generator-cli.jar"
GENERATOR_VERSION="7.4.0"
GENERATOR_URL="https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/${GENERATOR_VERSION}/openapi-generator-cli-${GENERATOR_VERSION}.jar"
MAX_WAIT_SECONDS=60
BACKEND_PID=""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Cleanup: stop backend on exit (success or failure)
cleanup() {
    if [ -n "$BACKEND_PID" ]; then
        log_info "Stopping backend (PID: $BACKEND_PID)..."
        kill "$BACKEND_PID" 2>/dev/null || true
        wait "$BACKEND_PID" 2>/dev/null || true
        log_info "Backend stopped."
    fi
}

trap cleanup EXIT

check_backend_running() {
    curl -sf "$HEALTH_URL" > /dev/null 2>&1
}

start_backend() {
    log_info "Starting backend..."

    if check_backend_running; then
        log_warn "Backend is already running on port 8080"
        return 0
    fi

    (cd "$BACKEND_DIR" && mvn -DskipTests spring-boot:run -P dev) &
    BACKEND_PID=$!
    log_info "Backend started with PID: $BACKEND_PID"
}

wait_for_backend() {
    log_info "Waiting for backend to be ready (max ${MAX_WAIT_SECONDS}s)..."

    local elapsed=0
    while [ $elapsed -lt $MAX_WAIT_SECONDS ]; do
        if check_backend_running; then
            log_info "Backend is ready!"
            return 0
        fi
        sleep 2
        elapsed=$((elapsed + 2))
        echo -n "."
    done

    echo ""
    log_error "Backend did not become ready within ${MAX_WAIT_SECONDS} seconds"
    return 1
}

ensure_generator() {
    if [ -f "$GENERATOR_JAR" ]; then
        log_info "OpenAPI generator JAR already present."
        return 0
    fi

    log_info "Downloading openapi-generator-cli v${GENERATOR_VERSION}..."
    mkdir -p tools
    curl -sL "$GENERATOR_URL" -o "$GENERATOR_JAR"
    log_info "Generator downloaded."
}

fetch_openapi_spec() {
    log_info "Fetching OpenAPI spec from $OPENAPI_URL..."
    curl -sf "$OPENAPI_URL" -o "$OPENAPI_SPEC"
    log_info "Saved OpenAPI spec to $OPENAPI_SPEC"
}

generate_sdk() {
    log_info "Generating Python SDK in $SDK_OUT_DIR/..."
    java -jar "$GENERATOR_JAR" generate \
        -i "$OPENAPI_SPEC" \
        -g python \
        -o "$SDK_OUT_DIR" \
        --package-name tracko_sdk
    log_info "SDK generated successfully!"
}

main() {
    log_info "=== SDK Generation Script ==="

    start_backend
    wait_for_backend
    ensure_generator
    fetch_openapi_spec
    generate_sdk

    log_info "=== SDK Generation Complete ==="
}

main "$@"
