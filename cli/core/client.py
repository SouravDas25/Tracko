import sys
import urllib.parse
from typing import Any, Dict, Optional

from .http import http_request, join_url
from .config import load_config, save_config, config_path


class APIError(Exception):
    pass


class TrackoClient:
    """A centralized client for making HTTP requests to the Tracko backend.
    
    This encapsulates URL joining, token injection, and JSON serialization.
    It also implements centralized error handling to fail fast on common issues.
    """

    def __init__(self, base_url: str, token: str | None = None):
        self.base_url = base_url
        self.token = token

    def _handle_response(self, result: Dict[str, Any]) -> Dict[str, Any]:
        """Centralized check for response validity."""
        if result.get("ok"):
            return result

        status = result.get("status")
        # Handle 401 Unauthorized globally
        if status == 401:
            print("Error: Unauthorized (401). Your token has expired or is invalid.", file=sys.stderr)
            
            # Optionally clear the token since it's dead
            cfg = load_config()
            if "token" in cfg:
                cfg.pop("token", None)
                save_config(cfg)
                print(f"Removed expired token from {config_path()}", file=sys.stderr)
            
            print("Please run `python -m tracko_cli login` again.", file=sys.stderr)
            sys.exit(1)

        # Connection refused / timeout
        if status is None or result.get("text", "").startswith("urllib.error.URLError"):
            print(f"Error: Could not connect to API at {self.base_url}", file=sys.stderr)
            sys.exit(1)

        return result

    def get(self, path: str, params: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        url = join_url(self.base_url, path)
        if params:
            filtered_params = {k: v for k, v in params.items() if v is not None}
            if filtered_params:
                url += "?" + urllib.parse.urlencode(filtered_params)
        return self._handle_response(http_request("GET", url, token=self.token))

    def post(self, path: str, json_body: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        url = join_url(self.base_url, path)
        return self._handle_response(http_request("POST", url, token=self.token, json_body=json_body))

    def put(self, path: str, json_body: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        url = join_url(self.base_url, path)
        return self._handle_response(http_request("PUT", url, token=self.token, json_body=json_body))

    def patch(self, path: str, json_body: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        url = join_url(self.base_url, path)
        return self._handle_response(http_request("PATCH", url, token=self.token, json_body=json_body))

    def delete(self, path: str) -> Dict[str, Any]:
        url = join_url(self.base_url, path)
        return self._handle_response(http_request("DELETE", url, token=self.token))
