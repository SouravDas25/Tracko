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
    concurrently --names "BACKEND,FLUTTER" --prefix-colors "blue,green" "cd Tracko-Java-Backend; mvn spring-boot:run -P dev" "cd Tracko-Flutter-UI; flutter run"

# Start Flutter UI only
flutter:
    @echo "🎨 Starting Flutter UI..."
    cd Tracko-Flutter-UI; flutter run -d chrome

# Start Java Backend only
backend:
    @echo "📱 Starting Java Backend..."
    cd Tracko-Java-Backend; mvn spring-boot:run -P dev

# Stop all services (cross-platform)
stop:
    @echo "🛑 Stopping services..."
    @if command -v pkill >/dev/null 2>&1; then pkill -f "flutter run" || true; pkill -f "mvn spring-boot:run" || true; else taskkill /F /IM java.exe 2>/dev/null || true; taskkill /F /IM flutter.exe 2>/dev/null || true; fi
    @echo "✅ Services stopped"


# Clean build artifacts
clean:
    @echo "🧹 Cleaning..."
    cd Tracko-Flutter-UI; flutter clean; cd ..
    cd Tracko-Java-Backend; mvn clean; cd ..
    @echo "✅ Clean complete"

# Run tests
test:
    @echo "🧪 Running tests..."
    cd Tracko-Flutter-UI; flutter test; cd ..
    cd Tracko-Java-Backend; mvn test; cd ..
    @echo "✅ Tests complete"

# Install dependencies
install:
    @echo "📦 Installing dependencies..."
    cd Tracko-Flutter-UI; flutter pub get; cd ..
    cd Tracko-Java-Backend; mvn install -P dev; cd ..
    npm install -g concurrently
    @echo "✅ Dependencies installed"
