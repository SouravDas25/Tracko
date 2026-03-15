#!/usr/bin/env python3
"""Run CLI tests with backend auto-start/stop."""
import subprocess
import time
import requests
import sys
from pathlib import Path
import shutil

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

def login():
    """Login and save token to 'test' profile."""
    print("Logging in...")
    project_root = Path(__file__).parent.parent
    cli_dir = Path(__file__).parent

    env = dict(subprocess.os.environ)
    env["PYTHONIOENCODING"] = "utf-8"
    env["TRACKO_PROFILE"] = "test"

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
    else:
        print(f"Login failed: {result.stderr}")
        if result.stdout:
            print(f"Stdout: {result.stdout}")

def run_tests():
    """Run pytest."""
    print("Running tests...")
    # Run from project root
    project_root = Path(__file__).parent.parent
    
    env = dict(subprocess.os.environ)
    env["PYTHONIOENCODING"] = "utf-8"
    env["TRACKO_PROFILE"] = "test"  # Set profile for tests
    
    result = subprocess.run(
        [sys.executable, "-m", "pytest", "cli/tests/test_transaction.py", "-v"],
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
        login()
        return run_tests()
    finally:
        stop_backend(proc)

if __name__ == "__main__":
    sys.exit(main())
