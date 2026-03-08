import argparse
import json

from ..core.config import get_token_from_args_or_config
from ..core.api import make_api_client, sdk_call_unwrapped

import tracko_sdk
from tracko_sdk.models.category_save_request import CategorySaveRequest

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
    sp = subparsers.add_parser("categories")
    sub = sp.add_subparsers(dest="categories_cmd", required=True)

    sub.add_parser("list").set_defaults(func=cmd_categories_list)

    sp2 = sub.add_parser("add")
    sp2.add_argument("--name", required=True)
    sp2.set_defaults(func=cmd_categories_add)

    sp2 = sub.add_parser("get")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_categories_get)

    sp2 = sub.add_parser("update")
    sp2.add_argument("--id", required=True, type=int)
    sp2.add_argument("--name", required=True)
    sp2.add_argument("--category-type", type=int, choices=[1, 2])
    sp2.set_defaults(func=cmd_categories_update)

    sp2 = sub.add_parser("delete")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_categories_delete)


def cmd_categories_list(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.CategoriesApi(api_client)
        result = sdk_call_unwrapped(lambda: api.get_all5())
    if result is None:
        return 1
    if args.raw:
        _print_raw(result)
        return 0
    rows = result if isinstance(result, list) else []
    if rows:
        columns = [("id", "ID"), ("name", "Name"), ("userId", "UserId")]
        print_table(rows, columns, max_widths={"name": 32, "userId": 36}, right_align={"id"})
        return 0
    _print_raw(result)
    return 0


def cmd_categories_add(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    req = CategorySaveRequest(name=args.name)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.CategoriesApi(api_client)
        result = sdk_call_unwrapped(lambda: api.create6(req))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_categories_get(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.CategoriesApi(api_client)
        result = sdk_call_unwrapped(lambda: api.get_by_id3(id=int(args.id)))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_categories_update(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    category_type = str(args.category_type) if getattr(args, "category_type", None) is not None else None
    req = CategorySaveRequest(name=args.name, category_type=category_type)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.CategoriesApi(api_client)
        result = sdk_call_unwrapped(lambda: api.update4(id=int(args.id), category_save_request=req))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_categories_delete(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.CategoriesApi(api_client)
        result = sdk_call_unwrapped(lambda: api.delete6(id=int(args.id)))
    if result is None:
        return 1
    _print_raw(result)
    return 0
