import argparse
import json

from ..core.config import get_token_from_args_or_config
from ..core.api import make_api_client, sdk_call

import tracko_sdk
from tracko_sdk.models.account_save_request import AccountSaveRequest
from ..utils.formatting import print_table


def _print_raw(data) -> None:
    if data is None:
        print("null")
        return
    if hasattr(data, "to_dict"):
        print(json.dumps(data.to_dict(), indent=2, default=str))
    else:
        print(json.dumps(data, indent=2, default=str))


def setup_parser(subparsers):
    sp = subparsers.add_parser("accounts", help="Account operations")
    sub = sp.add_subparsers(dest="accounts_cmd", required=True)

    sub.add_parser("list").set_defaults(func=cmd_accounts_list)
    sub.add_parser("balances").set_defaults(func=cmd_accounts_balances)

    sp2 = sub.add_parser("add")
    sp2.add_argument("--name", required=True)
    sp2.add_argument("--currency")
    sp2.set_defaults(func=cmd_accounts_add)

    sp2 = sub.add_parser("get")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_accounts_get)

    sp2 = sub.add_parser("update")
    sp2.add_argument("--id", required=True, type=int)
    sp2.add_argument("--name", required=True)
    sp2.add_argument("--currency")
    sp2.set_defaults(func=cmd_accounts_update)

    sp2 = sub.add_parser("delete")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_accounts_delete)

    sp2 = sub.add_parser("summary")
    sp2.add_argument("--id", required=True, type=int)
    sp2.add_argument("--start-date", required=True)
    sp2.add_argument("--end-date", required=True)
    sp2.add_argument("--include-rollover", action="store_true")
    sp2.set_defaults(func=cmd_accounts_summary)

    sp2 = sub.add_parser("transactions")
    sp2.add_argument("--id", required=True, type=int)
    sp2.add_argument("--page", type=int)
    sp2.add_argument("--size", type=int)
    sp2.add_argument("--expand", action="store_true")
    sp2.add_argument("--month", type=int)
    sp2.add_argument("--year", type=int)
    sp2.add_argument("--start-date")
    sp2.add_argument("--end-date")
    sp2.add_argument("--category-id", type=int)
    sp2.set_defaults(func=cmd_accounts_transactions)


def _parse_date(date_str: str):
    import datetime
    for fmt in ("%Y-%m-%d", "%d/%m/%Y"):
        try:
            return datetime.datetime.strptime(date_str, fmt).replace(tzinfo=datetime.timezone.utc)
        except ValueError:
            continue
    raise ValueError(f"Cannot parse date: {date_str}")


def cmd_accounts_list(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.AccountControllerApi(api_client)
        result = sdk_call(lambda: api.get_all6())
    if result is None:
        return 1
    if args.raw:
        _print_raw(result)
        return 0
    rows = result.get("result", []) if isinstance(result, dict) else []
    if isinstance(rows, list):
        columns = [("id", "ID"), ("name", "Name"), ("userId", "UserId")]
        print_table(rows, columns, max_widths={"name": 32, "userId": 36}, right_align={"id"})
        return 0
    _print_raw(result)
    return 0


def cmd_accounts_balances(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.AccountControllerApi(api_client)
        result = sdk_call(lambda: api.get_my_account_balances())
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_accounts_add(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    req = AccountSaveRequest(name=args.name, currency=getattr(args, "currency", None))
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.AccountControllerApi(api_client)
        result = sdk_call(lambda: api.create7(req))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_accounts_get(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.AccountControllerApi(api_client)
        result = sdk_call(lambda: api.get_by_id4(id=int(args.id)))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_accounts_update(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    req = AccountSaveRequest(name=args.name, currency=getattr(args, "currency", None))
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.AccountControllerApi(api_client)
        result = sdk_call(lambda: api.update5(id=int(args.id), account_save_request=req))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_accounts_delete(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.AccountControllerApi(api_client)
        result = sdk_call(lambda: api.delete7(id=int(args.id)))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_accounts_summary(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    start = _parse_date(args.start_date)
    end = _parse_date(args.end_date)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.AccountControllerApi(api_client)
        result = sdk_call(lambda: api.get_account_summary(
            id=int(args.id),
            start_date=start,
            end_date=end,
            include_rollover=args.include_rollover if args.include_rollover else None,
        ))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_accounts_transactions(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    start = _parse_date(args.start_date) if args.start_date else None
    end = _parse_date(args.end_date) if args.end_date else None
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.AccountControllerApi(api_client)
        result = sdk_call(lambda: api.get_account_transactions(
            id=int(args.id),
            month=args.month,
            year=args.year,
            start_date=start,
            end_date=end,
            category_id=args.category_id,
            page=args.page,
            size=args.size,
            expand=args.expand if args.expand else None,
        ))
    if result is None:
        return 1
    _print_raw(result)
    return 0
