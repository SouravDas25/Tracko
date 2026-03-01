import argparse
import urllib.parse
from ..core.config import get_token_from_args_or_config
from ..core.client import TrackoClient
from ..utils.formatting import print_result


def setup_parser(subparsers):
    sp = subparsers.add_parser("exchange-rates")
    sub_er = sp.add_subparsers(dest="exchange_rates_cmd", required=True)
    sp2 = sub_er.add_parser("get")
    sp2.add_argument("--base-currency", required=True)
    sp2.set_defaults(func=cmd_exchange_rates_get)

    sp_js = subparsers.add_parser("json-store")
    sub_js = sp_js.add_subparsers(dest="json_store_cmd", required=True)

    sp2 = sub_js.add_parser("list")
    sp2.set_defaults(func=cmd_json_store_list)

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
    client = TrackoClient(base_url, token)
    result = client.get(
        f"/api/exchange-rates/{urllib.parse.quote(str(args.base_currency))}"
    )
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_json_store_list(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    result = client.get("/api/json-store")
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_json_store_get(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    result = client.get(f"/api/json-store/{urllib.parse.quote(str(args.name))}")
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_json_store_create(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)

    body = {
        "name": args.name,
        "value": args.value,
    }
    result = client.post("/api/json-store", json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_json_store_update(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)

    body = {"value": args.value}
    result = client.put(
        f"/api/json-store/{urllib.parse.quote(str(args.name))}", json_body=body
    )
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_json_store_delete(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    result = client.delete(f"/api/json-store/{urllib.parse.quote(str(args.name))}")
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1
