import argparse

from ..core.config import (
    config_path,
    get_token_from_args_or_config,
    get_active_profile_name,
    update_profile,
)
from ..core.api import make_api_client, sdk_call

import tracko_sdk
from tracko_sdk.models.login_request import LoginRequest
from tracko_sdk.models.authication_request import AuthicationRequest


def setup_parser(subparsers):
    sp_login = subparsers.add_parser("login", help="Login via /api/login")
    sp_login.add_argument("--username", required=True)
    sp_login.add_argument("--password", required=True)
    sp_login.set_defaults(func=cmd_login)

    sp_oauth = subparsers.add_parser("oauth-token", help="Login via /api/oauth/token")
    sp_oauth.add_argument("--phone-no", required=True)
    sp_oauth.add_argument("--password", required=True)
    sp_oauth.set_defaults(func=cmd_oauth_token)

    sp_logout = subparsers.add_parser("logout", help="Clear saved token")
    sp_logout.set_defaults(func=cmd_logout)


def _extract_token(result) -> str | None:
    if result is None:
        return None
    if isinstance(result, dict):
        return result.get("token")
    return getattr(result, "token", None)


def cmd_login(args: argparse.Namespace) -> int:
    _, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url) as api_client:
        api = tracko_sdk.SessionControllerApi(api_client)
        result = sdk_call(lambda: api.login(LoginRequest(username=args.username, password=args.password)))

    if args.raw:
        import json
        print(json.dumps(result if isinstance(result, dict) else vars(result) if hasattr(result, '__dict__') else str(result), default=str))

    token = _extract_token(result)
    if token:
        active_profile = get_active_profile_name()
        update_profile(active_profile, {"base_url": base_url, "token": token})
        print(f"Saved token to profile '{active_profile}' in {config_path()}")
        return 0
    return 1


def cmd_oauth_token(args: argparse.Namespace) -> int:
    _, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url) as api_client:
        api = tracko_sdk.SessionControllerApi(api_client)
        result = sdk_call(lambda: api.sign_in(AuthicationRequest(phone_no=args.phone_no, password=args.password)))

    if args.raw:
        import json
        print(json.dumps(result if isinstance(result, dict) else str(result), default=str))

    token = _extract_token(result)
    if token:
        active_profile = get_active_profile_name()
        update_profile(active_profile, {"base_url": base_url, "token": token})
        print(f"Saved token to profile '{active_profile}' in {config_path()}")
        return 0
    return 1


def cmd_logout(args: argparse.Namespace) -> int:
    active_profile = get_active_profile_name()
    update_profile(active_profile, {"token": None})
    print(f"Logged out from profile '{active_profile}' (token removed)")
    return 0
