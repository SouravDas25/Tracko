# Trako CLI installer / updater for Windows
# Usage: irm https://raw.githubusercontent.com/SouravDas25/Tracko/main/scripts/install.ps1 | iex
$ErrorActionPreference = "Stop"

$Repo = "SouravDas25/Tracko"
$Asset = "trako-windows-x86_64.exe"
$InstallDir = "$env:USERPROFILE\.trako\bin"
$BinaryName = "trako.exe"

# ── Create install directory ───────────────────────────────────────────
if (-not (Test-Path $InstallDir)) {
    New-Item -ItemType Directory -Path $InstallDir -Force | Out-Null
}

# ── Download ───────────────────────────────────────────────────────────
$DownloadUrl = "https://github.com/$Repo/releases/latest/download/$Asset"
$DestPath = Join-Path $InstallDir $BinaryName

$IsUpdate = Test-Path $DestPath

Write-Host "Downloading from $DownloadUrl ..."
Invoke-WebRequest -Uri $DownloadUrl -OutFile $DestPath -UseBasicParsing

# ── Add to PATH if not already present ─────────────────────────────────
$UserPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($UserPath -notlike "*$InstallDir*") {
    [Environment]::SetEnvironmentVariable("Path", "$UserPath;$InstallDir", "User")
    $env:Path = "$env:Path;$InstallDir"
    Write-Host "Added $InstallDir to your PATH."
}

# ── Verify ─────────────────────────────────────────────────────────────
Write-Host ""
if ($IsUpdate) {
    Write-Host "Trako CLI updated successfully!"
} else {
    Write-Host "Trako CLI installed successfully!"
}
Write-Host "  Location: $DestPath"
Write-Host ""
Write-Host "Restart your terminal, then run:"
Write-Host "  trako --help"
Write-Host "  trako auth login"
