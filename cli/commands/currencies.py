import argparse
import json

from ..core.config import get_token_from_args_or_config
from ..core.api import make_api_client, sdk_call_unwrapped

import tracko_sdk
from tracko_sdk.models.user_currency_request import UserCurrencyRequest

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
    sp = subparsers.add_parser("currencies")
    sub = sp.add_subparsers(dest="currencies_cmd", required=True)

    sub.add_parser("list").set_defaults(func=cmd_currencies_list)

    sp2 = sub.add_parser("add")
    sp2.add_argument("--code", required=True)
    sp2.add_argument("--rate", required=True, type=float)
    sp2.set_defaults(func=cmd_currencies_add)

    sp2 = sub.add_parser("update")
    sp2.add_argument("--code", required=True)
    sp2.add_argument("--rate", required=True, type=float)
    sp2.set_defaults(func=cmd_currencies_update)

    sp2 = sub.add_parser("delete")
    sp2.add_argument("--code", required=True)
    sp2.set_defaults(func=cmd_currencies_delete)


def cmd_currencies_list(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.UserCurrenciesApi(api_client)
        result = sdk_call_unwrapped(lambda: api.get_all())
    if result is None:
        return 1
    if args.raw:
        _print_raw(result)
        return 0
    rows = result if isinstance(result, list) else []
    if rows:
        columns = [("currencyCode", "Code"), ("exchangeRate", "Rate")]
        print_table(rows, columns, right_align={"exchangeRate"})
        return 0
    _print_raw(result)
    return 0


def cmd_currencies_add(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    req = UserCurrencyRequest(currency_code=str(args.code).upper(), exchange_rate=float(args.rate))
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.UserCurrenciesApi(api_client)
        result = sdk_call_unwrapped(lambda: api.save(req))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_currencies_update(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    req = UserCurrencyRequest(currency_code=str(args.code).upper(), exchange_rate=float(args.rate))
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.UserCurrenciesApi(api_client)
        result = sdk_call_unwrapped(lambda: api.save(req))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_currencies_delete(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.UserCurrenciesApi(api_client)
        result = sdk_call_unwrapped(lambda: api.delete(currency_code=str(args.code).upper()))
    if result is None:
        return 1
    _print_raw(result)
    return 0
