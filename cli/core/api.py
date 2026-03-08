import json
import os
import sys

# Add the generated SDK to the path so it can be imported without installation.
_SDK_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "sdk"))
if _SDK_PATH not in sys.path:
    sys.path.insert(0, _SDK_PATH)

import tracko_sdk
from tracko_sdk.rest import ApiException

from .config import load_config, save_config, config_path


class _RawApiClient(tracko_sdk.ApiClient):
    """ApiClient subclass that skips typed deserialization.

    The backend wraps every response in {"result": T, "message": "..."} which
    breaks the SDK's Pydantic-based deserialization.  This subclass returns the
    raw parsed JSON (dict/list) so callers can unwrap "result" themselves.
    """

    def deserialize(self, response_text, response_type):
        try:
            return json.loads(response_text)
        except (json.JSONDecodeError, TypeError):
            return response_text


def make_api_client(base_url: str, token: str | None = None) -> tracko_sdk.ApiClient:
    """Return a configured SDK ApiClient for the given base URL and bearer token."""
    configuration = tracko_sdk.Configuration(host=base_url.rstrip("/"))
    if token:
        configuration.access_token = token
    return _RawApiClient(configuration)


def sdk_call_unwrapped(fn, auth_call=False):
    """Like sdk_call but unwraps the backend's {\"result\": T} envelope before returning."""
    result = sdk_call(fn, auth_call=auth_call)
    if isinstance(result, dict) and "result" in result:
        return result["result"]
    return result


def sdk_call(fn, auth_call=False):
    """Execute an SDK API call with centralised error handling.

    Returns the API result on success (as a raw dict/list thanks to _RawApiClient).
    Exits on 401 (clears saved token) or connection failure, unless auth_call=True
    in which case a 401 is treated as a normal API error (wrong credentials).
    Returns None on other API errors.
    """
    try:
        return fn()
    except ApiException as e:
        if e.status == 401 and not auth_call:
            print("Error: Unauthorized (401). Your token has expired or is invalid.", file=sys.stderr)
            cfg = load_config()
            cfg.get("profiles", {}).get(
                cfg.get("active_profile", "default"), {}
            ).pop("token", None)
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
