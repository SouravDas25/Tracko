import argparse
import sys
from ..core.config import (
    load_config,
    save_config,
    get_active_profile_name,
    set_active_profile,
    update_profile,
    list_profiles,
    get_active_profile_config,
    DEFAULT_BASE_URL
)

def setup_parser(subparsers):
    sp = subparsers.add_parser("config", help="Manage CLI configuration and profiles")
    sp_sub = sp.add_subparsers(dest="config_cmd", required=True)

    # list
    sp_list = sp_sub.add_parser("list", help="List all profiles")
    sp_list.set_defaults(func=cmd_list)

    # use (set active)
    sp_use = sp_sub.add_parser("use", help="Set the active profile")
    sp_use.add_argument("profile", help="Name of the profile to activate")
    sp_use.set_defaults(func=cmd_use)

    # set (update current or specific profile)
    sp_set = sp_sub.add_parser("set", help="Set configuration for a profile")
    sp_set.add_argument("--profile", help="Profile to update (defaults to active)")
    sp_set.add_argument("--base-url", help="Set base URL for the profile")
    sp_set.set_defaults(func=cmd_set)

    # show (show current config)
    sp_show = sp_sub.add_parser("show", help="Show configuration for current or specific profile")
    sp_show.add_argument("--profile", help="Profile to show (defaults to active)")
    sp_show.set_defaults(func=cmd_show)


def cmd_list(args: argparse.Namespace) -> int:
    data = list_profiles()
    active = data["active"]
    profiles = data["profiles"]
    
    print("Available profiles:")
    for p in profiles:
        prefix = "* " if p == active else "  "
        print(f"{prefix}{p}")
    return 0


def cmd_use(args: argparse.Namespace) -> int:
    set_active_profile(args.profile)
    print(f"Active profile set to '{args.profile}'")
    return 0


def cmd_set(args: argparse.Namespace) -> int:
    profile = args.profile or get_active_profile_name()
    updates = {}
    
    if args.base_url:
        updates["base_url"] = args.base_url
        
    if not updates:
        print("No updates provided. Use --base-url to update configuration.")
        return 1
        
    update_profile(profile, updates)
    print(f"Updated profile '{profile}': {updates}")
    return 0


def cmd_show(args: argparse.Namespace) -> int:
    profile_name = args.profile or get_active_profile_name()
    
    # We can't use get_active_profile_config directly if we want a specific profile
    # so we load config manually
    cfg = load_config()
    profiles = cfg.get("profiles", {})
    
    if profile_name not in profiles:
        print(f"Profile '{profile_name}' not found.")
        return 1
        
    profile_data = profiles[profile_name]
    print(f"Configuration for profile '{profile_name}':")
    for k, v in profile_data.items():
        # Mask token for security
        if k == "token" and v:
            v = v[:10] + "..." + v[-5:]
        print(f"  {k}: {v}")
        
    return 0
