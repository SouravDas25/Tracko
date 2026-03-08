import argparse
import json

from ..core.config import get_token_from_args_or_config
from ..core.api import make_api_client, sdk_call_unwrapped

import tracko_sdk
from tracko_sdk.models.json_store import JsonStore


def _print_raw(data) -> None:
    if data is None:
        print("null")
        return
    if hasattr(data, "to_dict"):
        print(json.dumps(data.to_dict(), indent=2, default=str))
    else:
        print(json.dumps(data, indent=2, default=str))


def setup_parser(subparsers):
    sp = subparsers.add_parser("exchange-rates")
    sub_er = sp.add_subparsers(dest="exchange_rates_cmd", required=True)
    sp2 = sub_er.add_parser("get")
    sp2.add_argument("--base-currency", required=True)
    sp2.set_defaults(func=cmd_exchange_rates_get)

    sp_js = subparsers.add_parser("json-store")
    sub_js = sp_js.add_subparsers(dest="json_store_cmd", required=True)

    sub_js.add_parser("list").set_defaults(func=cmd_json_store_list)

    sp2 = sub_js.add_parser("get")
    sp2.add_argument("--name", required=True)
    sp2.set_defaults(func=cmd_json_store_get)

    sp2 = sub_js.add_parser("create")
    sp2.add_argument("--name", required=True)
    sp2.add_argument("--value", required=True)
    sp2.set_defaults(func=cmd_json_store_create)

    sp2 = sub_js.add_parser("update")
    sp2.add_argument("--name", required=True)
    sp2.add_argument("--value", required=True)
    sp2.set_defaults(func=cmd_json_store_update)

    sp2 = sub_js.add_parser("delete")
    sp2.add_argument("--name", required=True)
    sp2.set_defaults(func=cmd_json_store_delete)


def cmd_exchange_rates_get(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.ExchangeRatesApi(api_client)
        result = sdk_call_unwrapped(lambda: api.get_rates(base_currency=str(args.base_currency)))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_json_store_list(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.JSONStoreApi(api_client)
        result = sdk_call_unwrapped(lambda: api.get_all4())
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_json_store_get(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.JSONStoreApi(api_client)
        result = sdk_call_unwrapped(lambda: api.get_by_name(name=args.name))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_json_store_create(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    req = JsonStore(name=args.name, value=args.value)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.JSONStoreApi(api_client)
        result = sdk_call_unwrapped(lambda: api.create4(req))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_json_store_update(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    req = JsonStore(name=args.name, value=args.value)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.JSONStoreApi(api_client)
        result = sdk_call_unwrapped(lambda: api.update2(name=args.name, json_store=req))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_json_store_delete(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.JSONStoreApi(api_client)
        result = sdk_call_unwrapped(lambda: api.delete4(name=args.name))
    if result is None:
        return 1
    _print_raw(result)
    return 0
