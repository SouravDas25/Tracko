#!/usr/bin/env bash
# Trako CLI installer / updater
# Usage: curl -fsSL https://raw.githubusercontent.com/SouravDas25/Tracko/main/scripts/install.sh | bash
set -euo pipefail

REPO="SouravDas25/Tracko"
INSTALL_DIR="/usr/local/bin"
BINARY_NAME="trako"

# ── Detect OS and architecture ─────────────────────────────────────────
OS="$(uname -s)"
ARCH="$(uname -m)"

case "$OS" in
  Linux)
    case "$ARCH" in
      x86_64) ASSET="trako-linux-x86_64" ;;
      *) echo "Error: Unsupported Linux architecture: $ARCH (only x86_64 is supported)"; exit 1 ;;
    esac
    ;;
  Darwin)
    case "$ARCH" in
      x86_64) ASSET="trako-macos-x86_64" ;;
      arm64)  ASSET="trako-macos-arm64" ;;
      *) echo "Error: Unsupported macOS architecture: $ARCH"; exit 1 ;;
    esac
    ;;
  *)
    echo "Error: Unsupported OS: $OS (use Linux or macOS)"
    echo "For Windows, see: https://github.com/$REPO#install-the-cli"
    exit 1
    ;;
esac

echo "Detected: $OS $ARCH → $ASSET"

# ── Resolve latest release URL ─────────────────────────────────────────
DOWNLOAD_URL="https://github.com/$REPO/releases/latest/download/$ASSET"

# ── Download ───────────────────────────────────────────────────────────
TMP_DIR="$(mktemp -d)"
TMP_FILE="$TMP_DIR/$BINARY_NAME"
trap 'rm -rf "$TMP_DIR"' EXIT

echo "Downloading from $DOWNLOAD_URL ..."
if command -v curl &>/dev/null; then
  curl -fSL --progress-bar "$DOWNLOAD_URL" -o "$TMP_FILE"
elif command -v wget &>/dev/null; then
  wget -q --show-progress "$DOWNLOAD_URL" -O "$TMP_FILE"
else
  echo "Error: curl or wget is required"
  exit 1
fi

chmod +x "$TMP_FILE"

# ── Detect fresh install vs update ─────────────────────────────────────
IS_UPDATE=false
if command -v "$BINARY_NAME" &>/dev/null; then
  IS_UPDATE=true
fi

# ── Install ────────────────────────────────────────────────────────────
if [ -w "$INSTALL_DIR" ]; then
  mv "$TMP_FILE" "$INSTALL_DIR/$BINARY_NAME"
else
  echo "Installing to $INSTALL_DIR (requires sudo) ..."
  sudo mv "$TMP_FILE" "$INSTALL_DIR/$BINARY_NAME"
fi

# ── Verify ─────────────────────────────────────────────────────────────
if command -v "$BINARY_NAME" &>/dev/null; then
  echo ""
  if [ "$IS_UPDATE" = true ]; then
    echo "Trako CLI updated successfully!"
  else
    echo "Trako CLI installed successfully!"
  fi
  echo "  Location: $(command -v $BINARY_NAME)"
  echo ""
  echo "Get started:"
  echo "  trako --help"
  echo "  trako auth login"
else
  echo ""
  echo "Trako CLI installed to $INSTALL_DIR/$BINARY_NAME"
  echo "Make sure $INSTALL_DIR is in your PATH."
fi
