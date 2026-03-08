import argparse
import datetime
import json

from ..core.config import get_token_from_args_or_config
from ..core.api import make_api_client, sdk_call_unwrapped

import tracko_sdk
from tracko_sdk.models.budget_allocation_request_dto import BudgetAllocationRequestDTO


def _print_raw(data) -> None:
    if data is None:
        print("null")
        return
    if hasattr(data, "to_dict"):
        print(json.dumps(data.to_dict(), indent=2, default=str))
    else:
        print(json.dumps(data, indent=2, default=str))


def setup_parser(subparsers):
    sp = subparsers.add_parser("budget")
    sub = sp.add_subparsers(dest="budget_cmd", required=True)

    sp2 = sub.add_parser("view")
    sp2.add_argument("--month", type=int)
    sp2.add_argument("--year", type=int)
    sp2.set_defaults(func=cmd_budget_view)

    sp2 = sub.add_parser("allocate")
    sp2.add_argument("--category-id", required=True, type=int)
    sp2.add_argument("--amount", required=True, type=float)
    sp2.add_argument("--month", type=int)
    sp2.add_argument("--year", type=int)
    sp2.set_defaults(func=cmd_budget_allocate)

    sp2 = sub.add_parser("available")
    sp2.add_argument("--month", type=int)
    sp2.add_argument("--year", type=int)
    sp2.set_defaults(func=cmd_budget_available)

    sub.add_parser("current").set_defaults(func=cmd_budget_current)


def _now_month_year():
    now = datetime.datetime.now()
    return now.month, now.year


def cmd_budget_view(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    month, year = _now_month_year()
    month = args.month if args.month is not None else month
    year = args.year if args.year is not None else year
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.BudgetApi(api_client)
        result = sdk_call_unwrapped(lambda: api.get_budget(month=month, year=year))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_budget_allocate(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    month, year = _now_month_year()
    month = args.month if args.month is not None else month
    year = args.year if args.year is not None else year
    req = BudgetAllocationRequestDTO(
        category_id=int(args.category_id),
        amount=float(args.amount),
        month=month,
        year=year,
    )
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.BudgetApi(api_client)
        result = sdk_call_unwrapped(lambda: api.allocate_funds(req))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_budget_available(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    month, year = _now_month_year()
    month = args.month if args.month is not None else month
    year = args.year if args.year is not None else year
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.BudgetApi(api_client)
        result = sdk_call_unwrapped(lambda: api.get_available_to_assign(month=month, year=year))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_budget_current(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.BudgetApi(api_client)
        result = sdk_call_unwrapped(lambda: api.get_current_budget())
    if result is None:
        return 1
    _print_raw(result)
    return 0
