import argparse
import urllib.parse
from ..core.config import get_token_from_args_or_config
from ..core.client import TrackoClient
from ..utils.formatting import print_result, print_table


def setup_parser(subparsers):
    sp = subparsers.add_parser("currencies")
    sub_curr = sp.add_subparsers(dest="currencies_cmd", required=True)

    sp2 = sub_curr.add_parser("list")
    sp2.set_defaults(func=cmd_currencies_list)

    sp2 = sub_curr.add_parser("add")
    sp2.add_argument("--code", required=True, help="Currency code, e.g., USD")
    sp2.add_argument(
        "--rate",
        required=True,
        type=float,
        help="Exchange rate to base per 1 unit of this currency",
    )
    sp2.set_defaults(func=cmd_currencies_add)

    sp2 = sub_curr.add_parser("update")
    sp2.add_argument("--code", required=True, help="Currency code, e.g., USD")
    sp2.add_argument(
        "--rate",
        required=True,
        type=float,
        help="Exchange rate to base per 1 unit of this currency",
    )
    sp2.set_defaults(func=cmd_currencies_update)

    sp2 = sub_curr.add_parser("delete")
    sp2.add_argument("--code", required=True, help="Currency code to delete, e.g., USD")
    sp2.set_defaults(func=cmd_currencies_delete)


def render_currencies_list(result: dict, raw: bool) -> int:
    if raw:
        print_result(result, raw=True)
        return 0 if result.get("ok") else 1
    payload = result.get("json")
    rows = None
    if isinstance(payload, dict):
        rows = payload.get("result")
    if result.get("ok") and isinstance(rows, list):
        print(f"HTTP {result.get('status')} ({result.get('elapsed_ms')}ms)")
        columns = [
            ("currencyCode", "Code"),
            ("exchangeRate", "Rate"),
        ]
        print_table(
            rows,
            columns,
            right_align={"exchangeRate"},
        )
        return 0
    print_result(result, raw=False)
    return 0 if result.get("ok") else 1


def cmd_currencies_list(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    result = client.get("/api/user-currencies")
    return render_currencies_list(result, raw=args.raw)


def cmd_currencies_add(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)

    body = {
        "currencyCode": str(args.code).upper(),
        "exchangeRate": float(args.rate),
    }
    result = client.post("/api/user-currencies", json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_currencies_update(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)

    body = {
        "currencyCode": str(args.code).upper(),
        "exchangeRate": float(args.rate),
    }
    result = client.post("/api/user-currencies", json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_currencies_delete(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    code = str(args.code).upper()
    result = client.delete(f"/api/user-currencies/{urllib.parse.quote(code)}")
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1
