import argparse
import urllib.parse
from tracko_cli.core.config import get_token_from_args_or_config
from tracko_cli.core.http import http_request, join_url
from tracko_cli.utils.formatting import print_result


def cmd_budget_current(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, "/api/budget/current")
    result = http_request("GET", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_budget_view(args: argparse.Namespace) -> int:
    import datetime
    token, base_url = get_token_from_args_or_config(args)
    now = datetime.datetime.now()
    month = args.month if args.month is not None else now.month
    year = args.year if args.year is not None else now.year
    
    query = urllib.parse.urlencode({"month": month, "year": year})
    url = join_url(base_url, "/api/budget") + "?" + query
    
    result = http_request("GET", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1


def cmd_budget_allocate(args: argparse.Namespace) -> int:
    import datetime
    token, base_url = get_token_from_args_or_config(args)
    now = datetime.datetime.now()
    month = args.month if args.month is not None else now.month
    year = args.year if args.year is not None else now.year
    
    url = join_url(base_url, "/api/budget/allocate")
    body = {
        "categoryId": int(args.category_id),
        "amount": float(args.amount),
        "month": month,
        "year": year
    }
    
    result = http_request("POST", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1


def cmd_budget_available(args: argparse.Namespace) -> int:
    import datetime
    token, base_url = get_token_from_args_or_config(args)
    now = datetime.datetime.now()
    month = args.month if args.month is not None else now.month
    year = args.year if args.year is not None else now.year
    
    params = {"month": month, "year": year}
    query = urllib.parse.urlencode(params)
    url = join_url(base_url, "/api/budget/available") + "?" + query
    
    result = http_request("GET", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1
