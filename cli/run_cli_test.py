#!/usr/bin/env python3
"""Run CLI tests with backend auto-start/stop."""
import json
import subprocess
import time
import requests
import sys
from pathlib import Path
import shutil

BASE_URL = "http://localhost:8080"
BACKEND_LOG = Path(__file__).parent / "tests" / "backend.log"
CONFIG_FILE = Path.home() / ".tracko-cli.json"
# Categories the CLI tests depend on. The backend seeds these via an
# ApplicationRunner that finishes *after* the web server starts accepting
# requests, so /api/health can return 200 before they exist. Gate on them
# to avoid a race where tests start mid-seed and hit "Category not found".
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


def start_backend():
    """Start Spring Boot backend."""
    project_root = Path(__file__).parent.parent
    backend_dir = project_root / "backend"
    log_file = project_root / "cli" / "tests" / "backend.log"

    # Fail if port 8080 is already in use
    try:
        resp = requests.get("http://localhost:8080/api/health", timeout=2)
        if resp.status_code:
            print("Error: port 8080 is already in use. Stop the existing process first.")
            return None
    except:
        pass  # Port is free, continue
    
    mvn_cmd = shutil.which("mvn")
    if not mvn_cmd:
        print("Maven not found")
        return None
    
    print("Starting Spring Boot backend...")
    proc = subprocess.Popen(
        [mvn_cmd, "spring-boot:run", "-Dspring-boot.run.profiles=test"],
        cwd=backend_dir,
        stdout=open(log_file, "w", encoding="utf-8"),
        stderr=subprocess.STDOUT,
        text=True,
        encoding="utf-8"
    )
    
    start = time.time()
    while time.time() - start < 120:
        try:
            resp = requests.get("http://localhost:8080/api/health", timeout=2)
            if resp.status_code == 200:
                print("Backend ready")
                return proc
        except:
            pass
        time.sleep(1)
    
    print("Backend failed to start")
    proc.terminate()
    return None

def configure_profile() -> bool:
    """Pin the 'test' profile to the freshly started local backend.

    The CLI resolves base_url from the active profile and otherwise falls back
    to a *remote* DEFAULT_BASE_URL. Without pinning, `auth login` authenticates
    against that remote server, mints a token signed with the remote JWT secret,
    and the whole suite (plus the seed gate) then talks to the wrong backend —
    the local one rejects the token with a SignatureException, surfacing in the
    CLI as "Category 'INCOME' not found".
    """
    print(f"Pinning 'test' profile to {BASE_URL}...")
    project_root = Path(__file__).parent.parent
    env = dict(subprocess.os.environ)
    env["PYTHONIOENCODING"] = "utf-8"
    env["TRACKO_PROFILE"] = "test"

    result = subprocess.run(
        [sys.executable, "-m", "cli", "config", "set", "--base-url", BASE_URL, "--profile", "test"],
        cwd=project_root,
        capture_output=True,
        text=True,
        encoding="utf-8",
        env=env,
    )
    if result.returncode != 0:
        print(f"Failed to pin base_url: {result.stderr or result.stdout}")
        return False
    return True


def login(max_attempts: int = 10) -> bool:
    """Login and save token to 'test' profile.

    Retries because the seeded user is created by the same startup runner
    that /api/health does not wait for, so the first attempt can race it.
    """
    print("Logging in...")
    project_root = Path(__file__).parent.parent

    env = dict(subprocess.os.environ)
    env["PYTHONIOENCODING"] = "utf-8"
    env["TRACKO_PROFILE"] = "test"

    for attempt in range(max_attempts):
        result = subprocess.run(
            [sys.executable, "-m", "cli", "auth", "login", "--username", "user@example.com", "--password", "password"],
            cwd=project_root,
            capture_output=True,
            text=True,
            encoding="utf-8",
            env=env
        )
        if result.returncode == 0:
            print("Login successful")
            return True
        print(f"Login attempt {attempt + 1}/{max_attempts} failed: {result.stderr or result.stdout}")
        time.sleep(1)
    return False


def wait_for_seed(max_attempts: int = 30) -> bool:
    """Wait until default categories are seeded before running tests.

    The seeder runs as an ApplicationRunner, which Spring executes only after
    the embedded server is already accepting requests. Without this gate the
    suite can start before INCOME/TRANSFER exist and fail with
    "Category 'INCOME' not found".
    """
    try:
        cfg = json.loads(CONFIG_FILE.read_text(encoding="utf-8"))
        token = cfg.get("profiles", {}).get("test", {}).get("token", "")
    except (OSError, ValueError) as e:
        print(f"wait_for_seed: could not read token from {CONFIG_FILE}: {e}")
        return False
    if not token:
        print("wait_for_seed: no token found; cannot verify seed state")
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
            print(f"Seed check attempt {attempt + 1} failed: {e}")
        time.sleep(1)

    print(f"ERROR: categories {sorted(REQUIRED_CATEGORIES)} not seeded after {max_attempts}s")
    return False

def run_tests():
    """Run pytest."""
    print("Running tests...")
    # Run from project root
    project_root = Path(__file__).parent.parent
    
    env = dict(subprocess.os.environ)
    env["PYTHONIOENCODING"] = "utf-8"
    env["TRACKO_PROFILE"] = "test"  # Set profile for tests
    
    result = subprocess.run(
        [sys.executable, "-m", "pytest", "cli/tests", "-v"],
        cwd=project_root,
        encoding="utf-8",
        env=env
    )
    return result.returncode

def stop_backend(proc):
    """Kill backend and all child processes."""
    print("Stopping backend...")
    try:
        import psutil
        # Kill by port 8080 (catches detached JVM processes too)
        for conn in psutil.net_connections():
            if conn.laddr.port == 8080 and conn.pid:
                try:
                    psutil.Process(conn.pid).kill()
                except Exception:
                    pass
        # Also kill maven process tree
        try:
            parent = psutil.Process(proc.pid)
            for child in parent.children(recursive=True):
                child.kill()
            parent.kill()
        except Exception:
            pass
    except ImportError:
        proc.terminate()
    proc.wait()

def main():
    """Main entry point."""
    proc = start_backend()
    if not proc:
        return 1

    try:
        if not configure_profile():
            print("Could not pin 'test' profile to local backend — aborting.")
            dump_backend_log_tail()
            return 1
        if not login():
            print("Login failed after retries — aborting.")
            dump_backend_log_tail()
            return 1
        if not wait_for_seed():
            print("Seed readiness gate failed — aborting.")
            dump_backend_log_tail()
            return 1
        rc = run_tests()
        if rc != 0:
            dump_backend_log_tail()
        return rc
    finally:
        stop_backend(proc)

if __name__ == "__main__":
    sys.exit(main())
