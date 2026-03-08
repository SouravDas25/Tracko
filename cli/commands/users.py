import argparse
import json

from ..core.config import get_token_from_args_or_config
from ..core.api import make_api_client, sdk_call_unwrapped

import tracko_sdk
from tracko_sdk.models.user_save_request import UserSaveRequest


def _print_raw(data) -> None:
    if data is None:
        print("null")
        return
    if hasattr(data, "to_dict"):
        print(json.dumps(data.to_dict(), indent=2, default=str))
    else:
        print(json.dumps(data, indent=2, default=str))


def setup_parser(subparsers):
    sp = subparsers.add_parser("users")
    sub = sp.add_subparsers(dest="users_cmd", required=True)

    sub.add_parser("list").set_defaults(func=cmd_users_list)
    sub.add_parser("me").set_defaults(func=cmd_users_me)

    sp2 = sub.add_parser("get")
    sp2.add_argument("--id", required=True)
    sp2.set_defaults(func=cmd_users_get)

    sp2 = sub.add_parser("find-phone")
    sp2.add_argument("--phone-no", required=True)
    sp2.set_defaults(func=cmd_users_find_phone)

    sp2 = sub.add_parser("upsert")
    sp2.add_argument("--phone-no", required=True)
    sp2.add_argument("--password", required=True)
    sp2.add_argument("--name")
    sp2.add_argument("--email")
    sp2.add_argument("--profile-pic")
    sp2.add_argument("--base-currency")
    sp2.add_argument("--shadow", action="store_true", default=None)
    sp2.add_argument("--not-shadow", action="store_false", dest="shadow")
    sp2.set_defaults(func=cmd_users_upsert)


def cmd_users_list(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.UsersApi(api_client)
        result = sdk_call_unwrapped(lambda: api.show())
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_users_me(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.UsersApi(api_client)
        result = sdk_call_unwrapped(lambda: api.me())
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_users_get(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.UsersApi(api_client)
        result = sdk_call_unwrapped(lambda: api.show1(id=args.id))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_users_find_phone(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.UsersApi(api_client)
        result = sdk_call_unwrapped(lambda: api.show_by_phone(phone_no=str(args.phone_no)))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_users_upsert(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    req = UserSaveRequest(
        phone_no=str(args.phone_no),
        password=str(args.password),
        name=args.name,
        email=args.email,
        profile_pic=args.profile_pic,
        base_currency=args.base_currency,
        is_shadow=1 if args.shadow else (0 if args.shadow is False else None),
    )
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.UsersApi(api_client)
        result = sdk_call_unwrapped(lambda: api.create(req))
    if result is None:
        return 1
    _print_raw(result)
    return 0
