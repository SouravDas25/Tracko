import argparse
import urllib.parse
from ..core.config import get_token_from_args_or_config
from ..core.client import TrackoClient
from ..utils.formatting import print_result


def setup_parser(subparsers):
    sp = subparsers.add_parser("users")
    sub_usr = sp.add_subparsers(dest="users_cmd", required=True)

    sp2 = sub_usr.add_parser("list")
    sp2.set_defaults(func=cmd_users_list)

    sp2 = sub_usr.add_parser("me")
    sp2.set_defaults(func=cmd_users_me)

    sp2 = sub_usr.add_parser("get")
    sp2.add_argument("--id", required=True)
    sp2.set_defaults(func=cmd_users_get)

    sp2 = sub_usr.add_parser("find-phone")
    sp2.add_argument("--phone-no", required=True)
    sp2.set_defaults(func=cmd_users_find_phone)

    sp2 = sub_usr.add_parser("upsert")
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
    client = TrackoClient(base_url, token)
    result = client.get("/api/user")
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1


def cmd_users_me(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    result = client.get("/api/user/me")
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_users_get(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    result = client.get(f"/api/user/{urllib.parse.quote(str(args.id))}")
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_users_find_phone(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    q = urllib.parse.urlencode({"phone_no": str(args.phone_no)})
    result = client.get("/api/user/byPhoneNo" + "?" + q)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_users_upsert(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)

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
    result = client.post("/api/user/create", json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1
