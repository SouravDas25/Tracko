import argparse
from tracko_cli.core.config import get_token_from_args_or_config
from tracko_cli.core.http import http_request, join_url
from tracko_cli.utils.formatting import print_result, print_table


def cmd_categories_list(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, "/api/categories")
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


def cmd_categories_add(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, "/api/categories")
    body = {"name": args.name}
    result = http_request("POST", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1


def cmd_categories_get(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, f"/api/categories/{int(args.id)}")
    result = http_request("GET", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_categories_update(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, f"/api/categories/{int(args.id)}")
    body: dict = {"name": args.name}
    if getattr(args, "category_type", None) is not None:
        body["categoryType"] = args.category_type
    result = http_request("PUT", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_categories_delete(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, f"/api/categories/{int(args.id)}")
    result = http_request("DELETE", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1
