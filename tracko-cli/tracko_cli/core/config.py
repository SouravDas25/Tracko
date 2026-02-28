import json
import os

from .http import DEFAULT_BASE_URL


def config_path() -> str:
    return os.path.join(os.path.dirname(__file__), os.pardir, os.pardir, ".tracko-cli.json")


def load_config() -> dict:
    path = os.path.abspath(config_path())
    if not os.path.exists(path):
        return {}
    try:
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f) or {}
    except Exception:
        return {}


def save_config(cfg: dict) -> None:
    path = os.path.abspath(config_path())
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(cfg, f, indent=2, sort_keys=True)
    os.replace(tmp, path)


def get_token_from_args_or_config(args) -> tuple[str | None, str]:
    cfg = load_config()
    base_url = getattr(args, "base_url", None) or cfg.get("base_url") or DEFAULT_BASE_URL
    token = getattr(args, "token", None) or cfg.get("token")
    return token, base_url
