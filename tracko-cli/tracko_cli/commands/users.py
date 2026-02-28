import argparse
import urllib.parse
from tracko_cli.core.config import get_token_from_args_or_config
from tracko_cli.core.http import http_request, join_url
from tracko_cli.utils.formatting import print_result


def cmd_users_list(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, "/api/user")
    result = http_request("GET", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1


def cmd_users_me(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, "/api/user/me")
    result = http_request("GET", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_users_get(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, f"/api/user/{urllib.parse.quote(str(args.id))}")
    result = http_request("GET", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_users_find_phone(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    q = urllib.parse.urlencode({"phone_no": str(args.phone_no)})
    url = join_url(base_url, "/api/user/byPhoneNo") + "?" + q
    result = http_request("GET", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_users_upsert(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, "/api/user/save")

    body: dict = {
        "phoneNo": str(args.phone_no),
        "password": str(args.password),
    }
    if args.name is not None:
        body["name"] = args.name
    if args.email is not None:
        body["email"] = args.email
    if args.profile_pic is not None:
        body["profilePic"] = args.profile_pic
    if args.base_currency is not None:
        body["baseCurrency"] = args.base_currency
    if args.shadow is not None:
        body["isShadow"] = 1 if args.shadow else 0

    result = http_request("POST", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1
