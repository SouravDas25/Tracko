import argparse
import urllib.parse
from tracko_cli.core.config import get_token_from_args_or_config
from tracko_cli.core.http import http_request, join_url
from tracko_cli.utils.formatting import print_result, print_table


def cmd_accounts_list(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, "/api/accounts")
    result = http_request("GET", url, token=token)
    if args.raw:
        print_result(result, raw=True)
        return 0 if result["ok"] else 1

    payload = result.get("json")
    rows = None
    if isinstance(payload, dict):
        rows = payload.get("result")

    if result.get("ok") and isinstance(rows, list):
        print(f"HTTP {result.get('status')} ({result.get('elapsed_ms')}ms)")
        columns = [
            ("id", "ID"),
            ("name", "Name"),
            ("userId", "UserId"),
        ]
        print_table(
            rows,
            columns,
            max_widths={
                "name": 32,
                "userId": 36,
            },
            right_align={"id"},
        )
        return 0

    print_result(result, raw=False)
    return 0 if result["ok"] else 1


def cmd_accounts_add(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, "/api/accounts")
    body = {"name": args.name}
    if getattr(args, "currency", None) is not None:
        body["currency"] = args.currency
    result = http_request("POST", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1


def cmd_accounts_get(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, f"/api/accounts/{int(args.id)}")
    result = http_request("GET", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_accounts_update(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, f"/api/accounts/{int(args.id)}")
    body: dict = {"name": args.name}
    if getattr(args, "currency", None) is not None:
        body["currency"] = args.currency
    result = http_request("PUT", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_accounts_delete(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, f"/api/accounts/{int(args.id)}")
    result = http_request("DELETE", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_accounts_summary(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    query = urllib.parse.urlencode({
        "startDate": args.start_date,
        "endDate": args.end_date,
        "includeRollover": "true" if args.include_rollover else "false",
    })
    url = join_url(base_url, f"/api/accounts/{int(args.id)}/summary") + "?" + query
    result = http_request("GET", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_accounts_transactions(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    params: dict[str, object] = {
        "page": args.page,
        "size": args.size,
        "expand": "true" if args.expand else "false",
    }
    if args.month is not None:
        params["month"] = args.month
    if args.year is not None:
        params["year"] = args.year
    if args.start_date is not None:
        params["startDate"] = args.start_date
    if args.end_date is not None:
        params["endDate"] = args.end_date
    if args.category_id is not None:
        params["categoryId"] = args.category_id
    query = urllib.parse.urlencode(params)
    url = join_url(base_url, f"/api/accounts/{int(args.id)}/transactions") + "?" + query
    result = http_request("GET", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_accounts_balances(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, "/api/accounts/balances")
    result = http_request("GET", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1
