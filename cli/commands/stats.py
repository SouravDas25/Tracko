import argparse
import json

from ..core.config import get_token_from_args_or_config
from ..core.api import make_api_client, sdk_call

import tracko_sdk


def _print_raw(data) -> None:
    if data is None:
        print("null")
        return
    if hasattr(data, "to_dict"):
        print(json.dumps(data.to_dict(), indent=2, default=str))
    else:
        print(json.dumps(data, indent=2, default=str))


def setup_parser(subparsers):
    sp = subparsers.add_parser("stats")
    sub = sp.add_subparsers(dest="stats_cmd", required=True)

    sp2 = sub.add_parser("summary")
    sp2.add_argument("--range", required=True, choices=["weekly", "monthly", "yearly"])
    sp2.add_argument("--transaction-type", required=True, type=int, choices=[1, 2])
    sp2.add_argument("--date")
    sp2.set_defaults(func=cmd_stats_summary)

    sp2 = sub.add_parser("category-summary")
    sp2.add_argument("--range", required=True, choices=["weekly", "monthly", "yearly"])
    sp2.add_argument("--transaction-type", required=True, type=int, choices=[1, 2])
    sp2.add_argument("--category-id", required=True, type=int)
    sp2.add_argument("--date")
    sp2.set_defaults(func=cmd_stats_category_summary)


def _parse_date(date_str: str | None):
    if not date_str:
        return None
    import datetime
    for fmt in ("%Y-%m-%d", "%d/%m/%Y"):
        try:
            return datetime.datetime.strptime(date_str, fmt).replace(tzinfo=datetime.timezone.utc)
        except ValueError:
            continue
    raise ValueError(f"Cannot parse date: {date_str}")


def cmd_stats_summary(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    var_date = _parse_date(args.date)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.StatsControllerApi(api_client)
        result = sdk_call(lambda: api.get_stats(
            range=args.range,
            transaction_type=str(args.transaction_type),
            var_date=var_date,
        ))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_stats_category_summary(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    var_date = _parse_date(args.date)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.StatsControllerApi(api_client)
        result = sdk_call(lambda: api.get_category_stats(
            range=args.range,
            transaction_type=str(args.transaction_type),
            category_id=int(args.category_id),
            var_date=var_date,
        ))
    if result is None:
        return 1
    _print_raw(result)
    return 0
