from ..core.client import TrackoClient

def get_id_name_map(base_url: str, token: str | None, path: str) -> dict[int, str]:
    client = TrackoClient(base_url, token)
    result = client.get(path)
    payload = result.get("json")
    if not (result.get("ok") and isinstance(payload, dict)):
        return {}
    
    items = payload.get("result")
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
