import argparse
import urllib.parse
from tracko_cli.core.config import get_token_from_args_or_config
from tracko_cli.core.http import http_request, join_url
from tracko_cli.utils.formatting import print_result


def cmd_stats_summary(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    params: dict[str, object] = {
        "range": args.range,
        "transactionType": int(args.transaction_type),
    }
    if args.date is not None:
        params["date"] = args.date
    url = join_url(base_url, "/api/stats/summary") + "?" + urllib.parse.urlencode(params)
    result = http_request("GET", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_stats_category_summary(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    params: dict[str, object] = {
        "range": args.range,
        "transactionType": int(args.transaction_type),
        "categoryId": int(args.category_id),
    }
    if args.date is not None:
        params["date"] = args.date
    url = join_url(base_url, "/api/stats/category-summary") + "?" + urllib.parse.urlencode(params)
    result = http_request("GET", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1
