# Script to start backend, generate SDK, and stop backend
# Platforms: Windows
# Usage: powershell -ExecutionPolicy Bypass -File scripts/generate-sdk.ps1

$ErrorActionPreference = "Stop"

# Configuration
$BackendDir       = "backend"
$OpenApiUrl       = "http://localhost:8080/v3/api-docs"
$HealthUrl        = "http://localhost:8080/api/health"
$OpenApiSpec      = "openapi.json"
$SdkOutDir        = "sdk"
$GeneratorJar     = "tools/openapi-generator-cli.jar"
$GeneratorVersion = "7.4.0"
$GeneratorUrl     = "https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/$GeneratorVersion/openapi-generator-cli-$GeneratorVersion.jar"
$MaxWaitSeconds   = 60
$BackendProcess   = $null

function Log-Info  { param($Msg) Write-Host "[INFO] $Msg" -ForegroundColor Green }
function Log-Warn  { param($Msg) Write-Host "[WARN] $Msg" -ForegroundColor Yellow }
function Log-Error { param($Msg) Write-Host "[ERROR] $Msg" -ForegroundColor Red }

function Test-BackendRunning {
    try {
        $response = Invoke-WebRequest -Uri $HealthUrl -UseBasicParsing -TimeoutSec 2 -ErrorAction SilentlyContinue
        return $response.StatusCode -eq 200
    } catch {
        return $false
    }
}

function Stop-Backend {
    # Stop the Maven process tree if we started it
    if ($null -ne $script:BackendProcess -and -not $script:BackendProcess.HasExited) {
        Log-Info "Stopping backend (PID: $($script:BackendProcess.Id))..."
        Stop-Process -Id $script:BackendProcess.Id -Force -ErrorAction SilentlyContinue
        Log-Info "Backend process stopped."
    }

    # Also kill anything still listening on port 8080
    $pids = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue |
            Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($procId in $pids) {
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
    }
}

function Start-Backend {
    Log-Info "Starting backend..."

    if (Test-BackendRunning) {
        Log-Warn "Backend is already running on port 8080"
        return
    }

    $script:BackendProcess = Start-Process -FilePath "mvn" `
        -ArgumentList "-DskipTests", "spring-boot:run", "-P", "dev" `
        -WorkingDirectory $BackendDir `
        -PassThru -NoNewWindow
    Log-Info "Backend started with PID: $($script:BackendProcess.Id)"
}

function Wait-ForBackend {
    Log-Info "Waiting for backend to be ready (max ${MaxWaitSeconds}s)..."

    $elapsed = 0
    while ($elapsed -lt $MaxWaitSeconds) {
        if (Test-BackendRunning) {
            Log-Info "Backend is ready!"
            return
        }
        Start-Sleep -Seconds 2
        $elapsed += 2
        Write-Host "." -NoNewline
    }

    Write-Host ""
    Log-Error "Backend did not become ready within $MaxWaitSeconds seconds"
    throw "Backend startup timed out"
}

function Ensure-Generator {
    if (Test-Path $GeneratorJar) {
        Log-Info "OpenAPI generator JAR already present."
        return
    }

    Log-Info "Downloading openapi-generator-cli v$GeneratorVersion..."
    New-Item -ItemType Directory -Force -Path "tools" | Out-Null
    Invoke-WebRequest -Uri $GeneratorUrl -OutFile $GeneratorJar -UseBasicParsing
    Log-Info "Generator downloaded."
}

function Fetch-OpenApiSpec {
    Log-Info "Fetching OpenAPI spec from $OpenApiUrl..."
    Invoke-WebRequest -Uri $OpenApiUrl -OutFile $OpenApiSpec -UseBasicParsing
    Log-Info "Saved OpenAPI spec to $OpenApiSpec"
}

function Generate-Sdk {
    Log-Info "Generating Python SDK in $SdkOutDir/..."
    & java -jar $GeneratorJar generate `
        -i $OpenApiSpec `
        -g python `
        -o $SdkOutDir `
        --package-name tracko_sdk
    if ($LASTEXITCODE -ne 0) { throw "SDK generation failed" }
    Log-Info "SDK generated successfully!"
}

# Main
try {
    Log-Info "=== SDK Generation Script ==="

    Start-Backend
    Wait-ForBackend
    Ensure-Generator
    Fetch-OpenApiSpec
    Generate-Sdk

    Log-Info "=== SDK Generation Complete ==="
} finally {
    # Always stop backend, even on failure
    Stop-Backend
}
