import json
import ssl
import time
import urllib.error
import urllib.request


DEFAULT_BASE_URL = "http://localhost:8080"

# Create unverified SSL context for self-signed certificates
SSL_CONTEXT = ssl._create_unverified_context()


def join_url(base_url: str, path: str) -> str:
    base_url = base_url.rstrip("/")
    if not path.startswith("/"):
        path = "/" + path
    return base_url + path


def http_request(
    method: str,
    url: str,
    *,
    token: str | None = None,
    json_body: dict | None = None,
    timeout: int = 30,
):
    headers = {
        "Accept": "application/json",
    }
    data = None
    if json_body is not None:
        data = json.dumps(json_body).encode("utf-8")
        headers["Content-Type"] = "application/json"

    if token:
        headers["Authorization"] = f"Bearer {token}"

    req = urllib.request.Request(
        url=url, method=method.upper(), headers=headers, data=data
    )

    started = time.time()
    try:
        # Always use SSL_CONTEXT for HTTPS requests to skip SSL verification (dev only)
        context = SSL_CONTEXT if url.startswith("https://") else None
        with urllib.request.urlopen(req, timeout=timeout, context=context) as resp:
            raw = resp.read()
            elapsed_ms = int((time.time() - started) * 1000)
            content_type = resp.headers.get("Content-Type", "")
            text = raw.decode("utf-8", errors="replace") if raw else ""
            parsed = None
            if "application/json" in content_type and text:
                try:
                    parsed = json.loads(text)
                except Exception:
                    parsed = None
            return {
                "ok": 200 <= resp.status < 300,
                "status": resp.status,
                "headers": dict(resp.headers.items()),
                "text": text,
                "json": parsed,
                "elapsed_ms": elapsed_ms,
            }
    except urllib.error.HTTPError as e:
        raw = e.read() if hasattr(e, "read") else b""
        elapsed_ms = int((time.time() - started) * 1000)
        text = raw.decode("utf-8", errors="replace") if raw else ""
        parsed = None
        content_type = e.headers.get("Content-Type", "") if e.headers else ""
        if "application/json" in content_type and text:
            try:
                parsed = json.loads(text)
            except Exception:
                parsed = None
        return {
            "ok": False,
            "status": e.code,
            "headers": dict(e.headers.items()) if e.headers else {},
            "text": text,
            "json": parsed,
            "elapsed_ms": elapsed_ms,
        }
