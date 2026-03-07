import os
import sys

# Add the generated SDK to the path so it can be imported without installation.
_SDK_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "sdk"))
if _SDK_PATH not in sys.path:
    sys.path.insert(0, _SDK_PATH)

import tracko_sdk
from tracko_sdk.rest import ApiException

from .config import load_config, save_config, config_path


def make_api_client(base_url: str, token: str | None = None) -> tracko_sdk.ApiClient:
    """Return a configured SDK ApiClient for the given base URL and bearer token."""
    configuration = tracko_sdk.Configuration(host=base_url.rstrip("/"))
    client = tracko_sdk.ApiClient(configuration)
    if token:
        client.default_headers["Authorization"] = f"Bearer {token}"
    return client


def sdk_call(fn):
    """Execute an SDK API call with centralised error handling.

    Returns the API result on success.
    Exits on 401 (clears saved token) or connection failure.
    Returns None on other API errors.
    """
    try:
        return fn()
    except ApiException as e:
        if e.status == 401:
            print("Error: Unauthorized (401). Your token has expired or is invalid.", file=sys.stderr)
            cfg = load_config()
            if "token" in cfg:
                cfg.pop("token", None)
                save_config(cfg)
                print(f"Removed expired token from {config_path()}", file=sys.stderr)
            print("Please run `python -m cli login` again.", file=sys.stderr)
            sys.exit(1)
        print(f"API error {e.status}: {e.reason}", file=sys.stderr)
        if e.body:
            print(e.body, file=sys.stderr)
        return None
    except Exception as e:
        msg = str(e)
        if any(k in msg for k in ("Connection refused", "Failed to establish", "URLError", "MaxRetryError")):
            print("Error: Could not connect to API.", file=sys.stderr)
            sys.exit(1)
        print(f"Error: {e}", file=sys.stderr)
        return None
