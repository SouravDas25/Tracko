from tracko_cli.core.http import http_request, join_url


def get_id_name_map(base_url: str, token: str | None, path: str) -> dict[int, str]:
    url = join_url(base_url, path)
    result = http_request("GET", url, token=token)
    payload = result.get("json")
    if not (result.get("ok") and isinstance(payload, dict) and isinstance(payload.get("result"), list)):
        return {}

    out: dict[int, str] = {}
    for item in payload.get("result"):
        if not isinstance(item, dict):
            continue
        try:
            _id = int(item.get("id"))
        except Exception:
            continue
        name = item.get("name")
        if name is None:
            continue
        out[_id] = str(name)
    return out
