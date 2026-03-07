import json

from ..core.api import make_api_client


def get_id_name_map(base_url: str, token: str | None, path: str) -> dict[int, str]:
    with make_api_client(base_url, token) as api_client:
        headers = {"Accept": "application/json"}
        if token:
            headers["Authorization"] = f"Bearer {token}"
        try:
            resp = api_client.rest_client.request("GET", base_url.rstrip("/") + path, headers=headers)
            resp.read()
            payload = json.loads(resp.data) if resp.data else {}
        except Exception:
            return {}

    items = payload.get("result") if isinstance(payload, dict) else None
    if not isinstance(items, list):
        return {}

    out: dict[int, str] = {}
    for item in items:
        if not isinstance(item, dict):
            continue
        try:
            _id = int(item.get("id", 0))
        except Exception:
            continue
        name = item.get("name")
        if name is None:
            continue
        out[_id] = str(name)
    return out
