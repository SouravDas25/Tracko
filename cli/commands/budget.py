import argparse
import urllib.parse
from tracko_cli.core.config import get_token_from_args_or_config
from tracko_cli.core.client import TrackoClient
from tracko_cli.utils.formatting import print_result


def setup_parser(subparsers):
    sp = subparsers.add_parser("budget")
    sub_budget = sp.add_subparsers(dest="budget_cmd", required=True)

    sp2 = sub_budget.add_parser("view")
    sp2.add_argument("--month", type=int, help="Month (1-12)")
    sp2.add_argument("--year", type=int, help="Year (YYYY)")
    sp2.set_defaults(func=cmd_budget_view)

    sp2 = sub_budget.add_parser("allocate")
    sp2.add_argument("--category-id", required=True, type=int)
    sp2.add_argument("--amount", required=True, type=float)
    sp2.add_argument("--month", type=int, help="Month (1-12)")
    sp2.add_argument("--year", type=int, help="Year (YYYY)")
    sp2.set_defaults(func=cmd_budget_allocate)

    sp2 = sub_budget.add_parser("available")
    sp2.add_argument("--month", type=int, help="Month (1-12)")
    sp2.add_argument("--year", type=int, help="Year (YYYY)")
    sp2.set_defaults(func=cmd_budget_available)

    sp2 = sub_budget.add_parser("current")
    sp2.set_defaults(func=cmd_budget_current)


def cmd_budget_current(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    client = TrackoClient(base_url, token)
    result = client.get("/api/budget/current")
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_budget_view(args: argparse.Namespace) -> int:
    import datetime
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    now = datetime.datetime.now()
    month = args.month if args.month is not None else now.month
    year = args.year if args.year is not None else now.year

    query = urllib.parse.urlencode({"month": month, "year": year})
    result = client.get("/api/budget" + "?" + query)
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1


def cmd_budget_allocate(args: argparse.Namespace) -> int:
    import datetime

    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    now = datetime.datetime.now()
    month = args.month if args.month is not None else now.month
    year = args.year if args.year is not None else now.year

    body = {
        "categoryId": int(args.category_id),
        "amount": float(args.amount),
        "month": month,
        "year": year,
    }

    result = client.post("/api/budget/allocate", json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1


def cmd_budget_available(args: argparse.Namespace) -> int:
    import datetime
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    now = datetime.datetime.now()
    month = args.month if args.month is not None else now.month
    year = args.year if args.year is not None else now.year

    params = {"month": month, "year": year}
    query = urllib.parse.urlencode(params)
    result = client.get("/api/budget/available" + "?" + query)
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1
