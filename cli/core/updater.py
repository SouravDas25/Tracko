"""Self-update logic: GitHub API, platform detection, binary replacement."""
import json
import os
import platform
import subprocess
import sys
import tempfile
import urllib.request
from functools import lru_cache
from typing import Optional, Tuple

from rich.console import Console

GITHUB_API = "https://api.github.com/repos/SouravDas25/Tracko/releases/latest"
REQUEST_TIMEOUT = 5

_console = Console(log_time=False, force_terminal=True)


def get_current_version() -> Optional[str]:
    """Return the current build version, or None if dev build."""
    from ._build_version import BUILD_VERSION
    return BUILD_VERSION


def get_platform_asset_name() -> Optional[str]:
    system = sys.platform
    machine = platform.machine().lower()

    if system == 'win32':
        return 'trako-windows-x86_64.exe'
    elif system == 'darwin':
        if machine in ('arm64', 'aarch64'):
            return 'trako-macos-arm64'
        return 'trako-macos-x86_64'
    elif system == 'linux':
        if machine in ('x86_64', 'amd64'):
            return 'trako-linux-x86_64'
    return None


@lru_cache(maxsize=1)
def _fetch_latest_release() -> Optional[dict]:
    """Fetch the latest release metadata. Cached for the process lifetime so a
    single command (e.g. ``update apply``) doesn't hit the GitHub API twice."""
    request = urllib.request.Request(
        GITHUB_API,
        headers={"Accept": "application/vnd.github+json"},
    )
    try:
        with urllib.request.urlopen(request, timeout=REQUEST_TIMEOUT) as resp:
            return json.loads(resp.read().decode())
    except Exception:
        return None


def get_latest_version() -> Optional[str]:
    data = _fetch_latest_release()
    if data:
        return data.get("tag_name")
    return None


def check_for_update() -> Tuple[bool, Optional[str]]:
    current = get_current_version()
    if not current:
        return False, None

    latest = get_latest_version()
    if not latest:
        return False, None

    if latest != current:
        return True, latest
    return False, None


def get_download_url(asset_name: str) -> Optional[str]:
    data = _fetch_latest_release()
    if not data:
        return None
    for asset in data.get("assets", []):
        if asset.get("name") == asset_name:
            return asset.get("browser_download_url")
    return None


def download_binary(url: str, dest: str) -> bool:
    try:
        with urllib.request.urlopen(url, timeout=120) as resp:
            with open(dest, 'wb') as f:
                f.write(resp.read())
        return True
    except Exception:
        return False


def replace_and_exit(new_path: str) -> None:
    current_path = sys.executable if getattr(sys, 'frozen', False) else sys.argv[0]
    current_path = os.path.abspath(current_path)

    if sys.platform == 'win32':
        _replace_windows(new_path, current_path)
    else:
        os.replace(new_path, current_path)
        os.chmod(current_path, 0o755)

    sys.exit(0)


def _replace_windows(new_path: str, current_path: str) -> None:
    ps1_lines = [
        '$ErrorActionPreference = "Stop"',
        'Start-Sleep -Seconds 2',
        f'Copy-Item -Path "{new_path}" -Destination "{current_path}" -Force',
        f'Remove-Item -Path "{new_path}" -Force -ErrorAction SilentlyContinue',
        'Remove-Item -Path $MyInvocation.MyCommand.Path -Force',
    ]
    fd, ps1_path = tempfile.mkstemp(suffix='.ps1', prefix='trako-update-')
    with os.fdopen(fd, 'w') as f:
        f.write('\n'.join(ps1_lines))
    subprocess.Popen(
        ['powershell', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', ps1_path],
        creationflags=subprocess.CREATE_NO_WINDOW if hasattr(subprocess, 'CREATE_NO_WINDOW') else 0,
    )
