# Tracko Justfile
# Run commands with: just <recipe>


# Use PowerShell on Windows so Windows env vars (e.g., JAVA_HOME) are visible
set windows-shell := ["powershell", "-NoLogo", "-NoProfile", "-Command"]

# Default recipe - show available commands
help:
    @echo "Tracko - Expense Manager"
    @echo ""
    @echo "Available commands:"
    @echo "  start        Start Flutter UI and Java Backend"
    @echo "  flutter      Start Flutter UI only"
    @echo "  backend      Start Java Backend only"
    @echo "  stop         Stop all services"
    @echo "  clean        Clean build artifacts"
    @echo "  test         Run tests"
    @echo ""

# Start both Flutter UI and Java Backend
start:
    @echo "🚀 Starting Tracko..."
    concurrently --names "BACKEND,FLUTTER" --prefix-colors "blue,green" "cd Tracko-Java-Backend; mvn spring-boot:run -P dev" "cd frontend; flutter run"

# Start Flutter UI only
flutter:
    @echo "🎨 Starting Flutter UI..."
    cd frontend; flutter run -d chrome

# Start Java Backend only
backend:
    @echo "📱 Starting Java Backend..."
    cd Tracko-Java-Backend; mvn spring-boot:run -P dev

# Stop all services (cross-platform)
stop:
    @echo "🛑 Stopping services..."
    {{ if os() == "windows" { "Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique | Stop-Process -Force -ErrorAction SilentlyContinue; Stop-Process -Name 'flutter' -Force -ErrorAction SilentlyContinue; exit 0" } else { "pkill -f 'flutter run' || true; pkill -f 'mvn spring-boot:run' || true" } }}
    @echo "✅ Services stopped"


# Clean build artifacts
clean:
    @echo "🧹 Cleaning..."
    cd frontend; flutter clean; cd ..
    cd Tracko-Java-Backend; mvn clean; cd ..
    @echo "✅ Clean complete"

# Run tests
test:
    @echo "🧪 Running tests..."
    cd frontend; flutter test; cd ..
    cd Tracko-Java-Backend; mvn test; cd ..
    @echo "✅ Tests complete"

# Install dependencies
install:
    @echo "📦 Installing dependencies..."
    cd frontend; flutter pub get; cd ..
    cd Tracko-Java-Backend; mvn install -P dev; cd ..
    npm install -g concurrently
    @echo "✅ Dependencies installed"
