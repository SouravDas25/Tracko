import argparse
import urllib.parse
from tracko_cli.core.config import get_token_from_args_or_config
from tracko_cli.core.http import http_request, join_url
from tracko_cli.utils.formatting import print_result, print_table


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
    url = join_url(base_url, "/api/user-currencies")
    result = http_request("GET", url, token=token)
    return render_currencies_list(result, raw=args.raw)


def cmd_currencies_add(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, "/api/user-currencies")
    body = {
        "currencyCode": str(args.code).upper(),
        "exchangeRate": float(args.rate),
    }
    result = http_request("POST", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_currencies_update(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, "/api/user-currencies")
    body = {
        "currencyCode": str(args.code).upper(),
        "exchangeRate": float(args.rate),
    }
    result = http_request("POST", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_currencies_delete(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    code = str(args.code).upper()
    url = join_url(base_url, f"/api/user-currencies/{urllib.parse.quote(code)}")
    result = http_request("DELETE", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1
