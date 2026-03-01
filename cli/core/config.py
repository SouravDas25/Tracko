import json
import os
import sys
from typing import Dict, Any, Tuple, Optional

# Standard import for DEFAULT_BASE_URL
# We need to make sure we don't have circular imports if http imports config,
# but currently http.py does not import config.py.
from .http import DEFAULT_BASE_URL

ENV_VAR_PROFILE = "TRACKO_PROFILE"
DEFAULT_PROFILE_NAME = "default"


def config_path() -> str:
    """Return the absolute path to the config file."""
    return os.path.abspath(os.path.join(os.getcwd(), ".tracko-cli.json"))


def load_raw_config() -> Dict[str, Any]:
    """Load the raw JSON config from disk without migration logic."""
    path = config_path()
    if not os.path.exists(path):
        return {}
    try:
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f) or {}
    except Exception:
        return {}


def _migrate_config(cfg: Dict[str, Any]) -> Dict[str, Any]:
    """
    Migrate old flat config to profile-based config structure in memory.
    Old format: {"base_url": "...", "token": "..."}
    New format: {
        "active_profile": "default",
        "profiles": {
            "default": {"base_url": "...", "token": "..."}
        }
    }
    """
    # If it already has profiles, assume it's new format or mixed
    if "profiles" in cfg:
        # Ensure active_profile is set
        if "active_profile" not in cfg:
            cfg["active_profile"] = DEFAULT_PROFILE_NAME
        return cfg

    # If it has old keys at root, migrate them to default profile
    old_base_url = cfg.get("base_url")
    old_token = cfg.get("token")

    # Start fresh with new structure
    new_cfg = {
        "active_profile": DEFAULT_PROFILE_NAME,
        "profiles": {
            DEFAULT_PROFILE_NAME: {
                "base_url": old_base_url or DEFAULT_BASE_URL,
                "token": old_token
            }
        }
    }
    return new_cfg


def load_config() -> Dict[str, Any]:
    """Load config and ensure it is in the new format."""
    raw = load_raw_config()
    return _migrate_config(raw)


def save_config(cfg: Dict[str, Any]) -> None:
    """Save the config dictionary to disk."""
    path = config_path()
    tmp = path + ".tmp"
    try:
        with open(tmp, "w", encoding="utf-8") as f:
            json.dump(cfg, f, indent=2, sort_keys=True)
        os.replace(tmp, path)
    except Exception as e:
        print(f"Error saving config: {e}", file=sys.stderr)
        if os.path.exists(tmp):
            os.remove(tmp)


def get_active_profile_name() -> str:
    """
    Determine the active profile name.
    Priority:
    1. Environment variable TRACKO_PROFILE
    2. Config file 'active_profile'
    3. Default 'default'
    """
    env_profile = os.environ.get(ENV_VAR_PROFILE)
    if env_profile:
        return env_profile
    
    cfg = load_config()
    return cfg.get("active_profile", DEFAULT_PROFILE_NAME)


def get_profile_config(profile_name: str) -> Dict[str, Any]:
    """Get the configuration dict for a specific profile."""
    cfg = load_config()
    profiles = cfg.get("profiles", {})
    return profiles.get(profile_name, {})


def get_active_profile_config() -> Dict[str, Any]:
    """Get the configuration dict for the currently active profile."""
    return get_profile_config(get_active_profile_name())


def get_token_from_args_or_config(args) -> Tuple[Optional[str], str]:
    """
    Helper to resolve token and base_url.
    Priority:
    1. CLI args (--token, --base-url)
    2. Active Profile config
    3. Defaults
    """
    # 1. Args
    arg_base_url = getattr(args, "base_url", None)
    arg_token = getattr(args, "token", None)
    
    # If both provided via args, we don't need to look at config
    if arg_base_url and arg_token:
        return arg_token, arg_base_url

    # 2. Config (Active Profile)
    profile_cfg = get_active_profile_config()
    
    # Resolve
    base_url = arg_base_url or profile_cfg.get("base_url") or DEFAULT_BASE_URL
    token = arg_token or profile_cfg.get("token")
    
    return token, base_url


def update_profile(profile_name: str, updates: Dict[str, Any]) -> None:
    """Update keys in a specific profile and save."""
    cfg = load_config()
    if "profiles" not in cfg:
        cfg["profiles"] = {}
    
    if profile_name not in cfg["profiles"]:
        cfg["profiles"][profile_name] = {}
        # If creating a new profile, set default base_url if not provided
        if "base_url" not in updates:
            cfg["profiles"][profile_name]["base_url"] = DEFAULT_BASE_URL

    cfg["profiles"][profile_name].update(updates)
    save_config(cfg)


def set_active_profile(profile_name: str) -> None:
    """Set the active profile in the config file."""
    cfg = load_config()
    # Ensure structure
    if "profiles" not in cfg:
        cfg["profiles"] = {}
        
    # Create profile if it doesn't exist
    if profile_name not in cfg["profiles"]:
        cfg["profiles"][profile_name] = {
            "base_url": DEFAULT_BASE_URL,
            "token": None
        }
    
    cfg["active_profile"] = profile_name
    save_config(cfg)


def list_profiles() -> Dict[str, Any]:
    """Return dict of profiles and the active one."""
    cfg = load_config()
    return {
        "active": get_active_profile_name(),
        "profiles": list(cfg.get("profiles", {}).keys())
    }
