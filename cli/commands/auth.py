import argparse
from ..core.config import (
    load_config,
    save_config,
    config_path,
    get_token_from_args_or_config,
)
from ..core.client import TrackoClient
from ..utils.formatting import print_result


def setup_parser(subparsers):
    # login
    sp_login = subparsers.add_parser("login", help="Login via /api/login")
    sp_login.add_argument("--username", required=True)
    sp_login.add_argument("--password", required=True)
    sp_login.set_defaults(func=cmd_login)

    # oauth-token
    sp_oauth = subparsers.add_parser("oauth-token", help="Login via /api/oauth/token")
    sp_oauth.add_argument("--phone-no", required=True)
    sp_oauth.add_argument("--password", required=True)
    sp_oauth.set_defaults(func=cmd_oauth_token)

    # logout
    sp_logout = subparsers.add_parser("logout", help="Clear saved token")
    sp_logout.set_defaults(func=cmd_logout)


def cmd_login(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url)
    body = {"username": args.username, "password": args.password}
    result = client.post("/api/login", json_body=body)
    print_result(result, raw=args.raw)

    new_token = None
    if isinstance(result.get("json"), dict):
        new_token = result["json"].get("token")

    if result["ok"] and new_token:
        from ..core.config import get_active_profile_name, update_profile
        
        active_profile = get_active_profile_name()
        update_profile(active_profile, {"base_url": base_url, "token": new_token})
        
        print(f"Saved token to profile '{active_profile}' in", config_path())
        return 0

    return 1


def cmd_oauth_token(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url)
    body = {"phoneNo": args.phone_no, "password": args.password}
    result = client.post("/api/oauth/token", json_body=body)
    print_result(result, raw=args.raw)

    new_token = None
    if isinstance(result.get("json"), dict):
        new_token = result["json"].get("token")

    if result["ok"] and new_token:
        from ..core.config import get_active_profile_name, update_profile
        
        active_profile = get_active_profile_name()
        update_profile(active_profile, {"base_url": base_url, "token": new_token})
        
        print(f"Saved token to profile '{active_profile}' in", config_path())
        return 0

    return 1


def cmd_logout(args: argparse.Namespace) -> int:
    from ..core.config import get_active_profile_name, update_profile
    
    active_profile = get_active_profile_name()
    # We update with token=None to remove it
    update_profile(active_profile, {"token": None})
    
    print(f"Logged out from profile '{active_profile}' (token removed)")
    return 0
