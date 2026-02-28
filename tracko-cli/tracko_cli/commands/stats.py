import argparse
import urllib.parse
from tracko_cli.core.config import get_token_from_args_or_config
from tracko_cli.core.client import TrackoClient
from tracko_cli.utils.formatting import print_result


def setup_parser(subparsers):
    sp = subparsers.add_parser("stats")
    sub_stats = sp.add_subparsers(dest="stats_cmd", required=True)

    sp2 = sub_stats.add_parser("summary")
    sp2.add_argument("--range", required=True, choices=["weekly", "monthly", "yearly"])
    sp2.add_argument("--transaction-type", required=True, type=int, choices=[1, 2])
    sp2.add_argument("--date", help="Anchor date (YYYY-MM-DD)")
    sp2.set_defaults(func=cmd_stats_summary)

    sp2 = sub_stats.add_parser("category-summary")
    sp2.add_argument("--range", required=True, choices=["weekly", "monthly", "yearly"])
    sp2.add_argument("--transaction-type", required=True, type=int, choices=[1, 2])
    sp2.add_argument("--category-id", required=True, type=int)
    sp2.add_argument("--date", help="Anchor date (YYYY-MM-DD)")
    sp2.set_defaults(func=cmd_stats_category_summary)


def cmd_stats_summary(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    params: dict[str, object] = {
        "range": args.range,
        "transactionType": int(args.transaction_type),
    }
    if args.date is not None:
        params["date"] = args.date
    result = client.get("/api/stats/summary" + "?" + urllib.parse.urlencode(params))
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_stats_category_summary(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    params: dict[str, object] = {
        "range": args.range,
        "transactionType": int(args.transaction_type),
        "categoryId": int(args.category_id),
    }
    if args.date is not None:
        params["date"] = args.date
    result = client.get(
        "/api/stats/category-summary" + "?" + urllib.parse.urlencode(params)
    )
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1
