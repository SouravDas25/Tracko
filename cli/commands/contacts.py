import argparse
import json

from ..core.config import get_token_from_args_or_config
from ..core.api import make_api_client, sdk_call

import tracko_sdk
from tracko_sdk.models.contact_save_request import ContactSaveRequest

from ..utils.formatting import print_table


def _print_raw(data) -> None:
    if data is None:
        print("null")
        return
    if hasattr(data, "to_dict"):
        print(json.dumps(data.to_dict(), indent=2, default=str))
    else:
        print(json.dumps(data, indent=2, default=str))


def setup_parser(subparsers):
    sp = subparsers.add_parser("contacts")
    sub = sp.add_subparsers(dest="contacts_cmd", required=True)

    sub.add_parser("list").set_defaults(func=cmd_contacts_list)

    sp2 = sub.add_parser("add")
    sp2.add_argument("--name", required=True)
    sp2.add_argument("--phone")
    sp2.add_argument("--email")
    sp2.set_defaults(func=cmd_contacts_add)

    sp2 = sub.add_parser("get")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_contacts_get)

    sp2 = sub.add_parser("update")
    sp2.add_argument("--id", required=True, type=int)
    sp2.add_argument("--name", required=True)
    sp2.add_argument("--phone")
    sp2.add_argument("--email")
    sp2.set_defaults(func=cmd_contacts_update)

    sp2 = sub.add_parser("delete")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_contacts_delete)


def cmd_contacts_list(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.ContactControllerApi(api_client)
        result = sdk_call(lambda: api.list_mine())
    if result is None:
        return 1
    if args.raw:
        _print_raw(result)
        return 0
    rows = result.get("result", []) if isinstance(result, dict) else []
    if isinstance(rows, list):
        columns = [("id", "ID"), ("name", "Name"), ("phoneNo", "Phone"), ("email", "Email")]
        print_table(rows, columns, max_widths={"name": 28, "phoneNo": 18, "email": 36}, right_align={"id"})
        return 0
    _print_raw(result)
    return 0


def cmd_contacts_add(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    req = ContactSaveRequest(name=args.name, phone_no=args.phone, email=args.email)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.ContactControllerApi(api_client)
        result = sdk_call(lambda: api.create5(req))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_contacts_get(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.ContactControllerApi(api_client)
        result = sdk_call(lambda: api.get_one(id=int(args.id)))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_contacts_update(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    req = ContactSaveRequest(name=args.name, phone_no=args.phone, email=args.email)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.ContactControllerApi(api_client)
        result = sdk_call(lambda: api.update3(id=int(args.id), contact_save_request=req))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_contacts_delete(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.ContactControllerApi(api_client)
        result = sdk_call(lambda: api.delete5(id=int(args.id)))
    if result is None:
        return 1
    _print_raw(result)
    return 0
