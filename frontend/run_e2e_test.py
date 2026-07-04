#!/usr/bin/env python3
"""Run the Flutter integration (E2E) tests against a live backend.

Mirrors ``cli/run_cli_test.py``: boot the Spring backend with the ``test``
profile (H2 in-memory, port 8080), wait until the ``GlobalStartupSeeder`` has
seeded the demo user + categories, start chromedriver, then drive the app on
Chrome/web via ``flutter drive`` and tear everything down.

Designed to run locally (with a display) or in CI under ``xvfb-run`` so Chrome
has a display to attach to.

Requires: Maven, Flutter, Chrome + chromedriver on PATH, and the Python
packages ``requests`` and ``psutil``.
"""
import shutil
import subprocess
import sys
import time
from pathlib import Path

import requests

PROJECT_ROOT = Path(__file__).resolve().parent.parent
BACKEND_DIR = PROJECT_ROOT / "backend"
FRONTEND_DIR = PROJECT_ROOT / "frontend"
BACKEND_LOG = FRONTEND_DIR / "integration_test" / "backend.log"

BASE_URL = "http://localhost:8080"
CHROMEDRIVER_PORT = 4444
SEED_EMAIL = "user@example.com"
SEED_PASSWORD = "password"
# Seeded by GlobalStartupSeeder. The seeder is an ApplicationRunner that
# finishes *after* /api/health starts returning 200, so gate on these before
# launching the UI to avoid starting a test mid-seed.
REQUIRED_CATEGORIES = {"INCOME", "TRANSFER", "FOOD"}


def dump_backend_log_tail(lines: int = 80):
    """Print the tail of the backend log so CI failures are diagnosable."""
    try:
        content = BACKEND_LOG.read_text(encoding="utf-8", errors="replace").splitlines()
    except OSError as e:
        print(f"Could not read backend log at {BACKEND_LOG}: {e}")
        return
    print(f"----- backend.log (last {lines} lines) -----")
    for line in content[-lines:]:
        print(line)
    print("----- end backend.log -----")


def _port_responds(url: str) -> bool:
    try:
        requests.get(url, timeout=2)
        return True
    except requests.RequestException:
        return False


def start_backend():
    """Start the Spring Boot backend with the test profile."""
    if _port_responds(f"{BASE_URL}/api/health"):
        print("Error: port 8080 is already in use. Stop the existing process first.")
        return None

    mvn_cmd = shutil.which("mvn")
    if not mvn_cmd:
        print("Maven not found on PATH")
        return None

    BACKEND_LOG.parent.mkdir(parents=True, exist_ok=True)
    print("Starting Spring Boot backend (test profile)...")
    proc = subprocess.Popen(
        [mvn_cmd, "spring-boot:run", "-Dspring-boot.run.profiles=test"],
        cwd=BACKEND_DIR,
        stdout=open(BACKEND_LOG, "w", encoding="utf-8"),
        stderr=subprocess.STDOUT,
        text=True,
    )

    start = time.time()
    while time.time() - start < 120:
        try:
            resp = requests.get(f"{BASE_URL}/api/health", timeout=2)
            if resp.status_code == 200:
                print("Backend ready")
                return proc
        except requests.RequestException:
            pass
        time.sleep(1)

    print("Backend failed to start within 120s")
    proc.terminate()
    return None


def _extract_token(body):
    """Pull a JWT out of a login response, tolerating envelope/field variants."""
    if not isinstance(body, dict):
        return None
    candidates = [body]
    if isinstance(body.get("result"), dict):
        candidates.append(body["result"])
    for candidate in candidates:
        for key in ("token", "jwtToken", "jwttoken", "jwt", "accessToken"):
            value = candidate.get(key)
            if isinstance(value, str) and value:
                return value
    return None


def wait_for_seed(max_attempts: int = 60) -> bool:
    """Log in as the seeded user and wait until default categories exist."""
    print("Waiting for seeded user + categories...")
    token = None
    for attempt in range(max_attempts):
        try:
            resp = requests.post(
                f"{BASE_URL}/api/login",
                json={"username": SEED_EMAIL, "password": SEED_PASSWORD},
                timeout=5,
            )
            if resp.status_code == 200:
                token = _extract_token(resp.json())
                if token:
                    break
        except requests.RequestException as e:
            print(f"Seed login attempt {attempt + 1}/{max_attempts} failed: {e}")
        time.sleep(1)

    if not token:
        print("Could not log in as the seeded user; seed not ready.")
        return False

    headers = {"Authorization": f"Bearer {token}", "Accept": "application/json"}
    for attempt in range(max_attempts):
        try:
            resp = requests.get(f"{BASE_URL}/api/categories", headers=headers, timeout=5)
            if resp.status_code == 200:
                names = {
                    str(c.get("name", "")).strip().upper()
                    for c in (resp.json().get("result") or [])
                }
                if REQUIRED_CATEGORIES.issubset(names):
                    print(f"Seed ready: required categories present {sorted(REQUIRED_CATEGORIES)}")
                    return True
                missing = sorted(REQUIRED_CATEGORIES - names)
                print(f"Waiting for seed... missing {missing} (have {len(names)} categories)")
        except requests.RequestException as e:
            print(f"Seed category check {attempt + 1}/{max_attempts} failed: {e}")
        time.sleep(1)

    print(f"ERROR: categories {sorted(REQUIRED_CATEGORIES)} not seeded after {max_attempts}s")
    return False


def start_chromedriver():
    """Start chromedriver for `flutter drive` to attach to."""
    cmd = shutil.which("chromedriver")
    if not cmd:
        print("chromedriver not found on PATH")
        return None

    print(f"Starting chromedriver on port {CHROMEDRIVER_PORT}...")
    proc = subprocess.Popen(
        [cmd, f"--port={CHROMEDRIVER_PORT}"],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.STDOUT,
    )

    start = time.time()
    while time.time() - start < 30:
        try:
            resp = requests.get(f"http://localhost:{CHROMEDRIVER_PORT}/status", timeout=2)
            if resp.status_code == 200 and resp.json().get("value", {}).get("ready"):
                print("chromedriver ready")
                return proc
        except requests.RequestException:
            pass
        time.sleep(1)

    print("chromedriver failed to start")
    proc.terminate()
    return None


def run_flutter_drive() -> int:
    """Run `flutter drive` against Chrome/web. Returns the process exit code."""
    flutter = shutil.which("flutter")
    if not flutter:
        print("flutter not found on PATH")
        return 1

    print("Fetching Flutter dependencies...")
    subprocess.run([flutter, "pub", "get"], cwd=FRONTEND_DIR)

    print("Running flutter drive (Chrome/web)...")
    cmd = [
        flutter,
        "drive",
        "--driver=integration_test/test_driver.dart",
        "--target=integration_test/app_test.dart",
        "-d",
        "web-server",
        "--browser-name=chrome",
    ]
    result = subprocess.run(cmd, cwd=FRONTEND_DIR)
    return result.returncode


def _kill_tree(proc):
    if not proc:
        return
    try:
        import psutil

        parent = psutil.Process(proc.pid)
        for child in parent.children(recursive=True):
            try:
                child.kill()
            except Exception:
                pass
        parent.kill()
    except Exception:
        try:
            proc.terminate()
        except Exception:
            pass


def stop_backend(proc):
    """Kill the backend and any child JVM/processes (incl. those on :8080)."""
    print("Stopping backend...")
    try:
        import psutil

        for conn in psutil.net_connections():
            if conn.laddr and conn.laddr.port == 8080 and conn.pid:
                try:
                    psutil.Process(conn.pid).kill()
                except Exception:
                    pass
    except Exception:
        # psutil missing, or net_connections denied (e.g. Windows) — the
        # process-tree kill below still cleans up the mvn/java children.
        pass
    _kill_tree(proc)
    try:
        proc.wait(timeout=30)
    except Exception:
        pass


def main() -> int:
    backend = start_backend()
    if not backend:
        return 1

    chromedriver = None
    try:
        if not wait_for_seed():
            dump_backend_log_tail()
            return 1

        chromedriver = start_chromedriver()
        if not chromedriver:
            dump_backend_log_tail()
            return 1

        rc = run_flutter_drive()
        if rc != 0:
            dump_backend_log_tail()
        return rc
    finally:
        _kill_tree(chromedriver)
        stop_backend(backend)


if __name__ == "__main__":
    sys.exit(main())
