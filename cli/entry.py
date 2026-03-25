"""Standalone entry point for PyInstaller builds."""
import sys
import os

# Ensure the bundled package is importable
if getattr(sys, "frozen", False):
    # Running as a PyInstaller bundle
    base = sys._MEIPASS
    if base not in sys.path:
        sys.path.insert(0, base)

from cli.main import main

if __name__ == "__main__":
    main()
