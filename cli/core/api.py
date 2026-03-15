import json
import os
import sys
from contextlib import contextmanager

# Add the generated SDK to the path so it can be imported without installation.
_SDK_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "sdk"))
if _SDK_PATH not in sys.path:
    sys.path.insert(0, _SDK_PATH)

import tracko_sdk
from tracko_sdk.rest import ApiException

from .config import load_config, save_config, get_active_profile_config, get_active_profile_name
from .output import print_error


def get_config_for_api(require_token: bool = True) -> tuple[str, str | None]:
    """Read base_url and token from the active profile. Exits if token is missing and required."""
    config = get_active_profile_config()
    base_url = config.get("base_url", "http://localhost:8080")
    token = config.get("token")
    if require_token and not token:
        print_error("Not authenticated. Please run 'tracko login' first.")
        sys.exit(1)
    return base_url, token


@contextmanager
def get_api_client(base_url: str, token: str | None = None):
    """Context manager that yields a configured SDK ApiClient."""
    config = tracko_sdk.Configuration(host=base_url.rstrip("/"))
    config.access_token = token
    with tracko_sdk.ApiClient(config) as client:
        yield client


def unwrap_envelope(response):
    """Extract .result from SDK envelope wrappers, pass through non-envelope responses."""
    if hasattr(response, 'result'):
        return response.result
    return response


def handle_api_error(e: ApiException, auth_call: bool = False) -> None:
    """Print an appropriate error message for an ApiException and exit."""
    if e.status == 401 and not auth_call:
        print_error("Unauthorized: Your token has expired or is invalid.")
        profile_name = get_active_profile_name()
        cfg = load_config()
        if "profiles" in cfg and profile_name in cfg["profiles"]:
            cfg["profiles"][profile_name].pop("token", None)
            save_config(cfg)
        print_error("Token cleared. Please run 'tracko login' again.")
        sys.exit(1)

    if e.status == 401 and auth_call:
        print_error("Invalid credentials.")
        sys.exit(1)

    if e.status == 400:
        print_error("Bad request: Invalid input.")
        _print_body_message(e.body)
        sys.exit(1)

    if e.status == 404:
        print_error("Not found: The requested resource does not exist.")
        sys.exit(1)

    if e.status >= 500:
        print_error(f"Server error ({e.status}).")
        _print_body_message(e.body)
        sys.exit(1)

    print_error(f"API error ({e.status}): {e.reason}")
    sys.exit(1)


def _print_body_message(body: str | None) -> None:
    """Extract and print the 'message' field from a JSON response body."""
    if not body:
        return
    try:
        msg = json.loads(body).get("message")
        if msg:
            print_error(f"Details: {msg}")
    except (json.JSONDecodeError, TypeError):
        print_error(f"Details: {body}")



