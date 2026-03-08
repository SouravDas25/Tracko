import argparse
import json

from ..core.config import get_token_from_args_or_config
from ..core.api import make_api_client, sdk_call_unwrapped

import tracko_sdk
from tracko_sdk.models.split import Split

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
    sp = subparsers.add_parser("splits")
    sub = sp.add_subparsers(dest="splits_cmd", required=True)

    sub.add_parser("list").set_defaults(func=cmd_splits_list)

    sp2 = sub.add_parser("for-transaction")
    sp2.add_argument("--transaction-id", required=True, type=int)
    sp2.set_defaults(func=cmd_splits_for_transaction)

    sp2 = sub.add_parser("for-user")
    sp2.add_argument("--user-id", required=True)
    sp2.set_defaults(func=cmd_splits_for_user)

    sp2 = sub.add_parser("unsettled")
    sp2.add_argument("--user-id", required=True)
    sp2.set_defaults(func=cmd_splits_unsettled)

    sp2 = sub.add_parser("for-contact")
    sp2.add_argument("--contact-id", required=True, type=int)
    sp2.set_defaults(func=cmd_splits_for_contact)

    sp2 = sub.add_parser("unsettled-contact")
    sp2.add_argument("--contact-id", required=True, type=int)
    sp2.set_defaults(func=cmd_splits_unsettled_contact)

    sp2 = sub.add_parser("create")
    sp2.add_argument("--transaction-id", required=True, type=int)
    sp2.add_argument("--user-id", required=True)
    sp2.add_argument("--amount", required=True, type=float)
    sp2.add_argument("--contact-id", type=int)
    sp2.add_argument("--is-settled")
    sp2.add_argument("--settled-at")
    sp2.set_defaults(func=cmd_splits_create)

    sp2 = sub.add_parser("get")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_splits_get)

    sp2 = sub.add_parser("settle")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_splits_settle)

    sp2 = sub.add_parser("unsettle")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_splits_unsettle)

    sp2 = sub.add_parser("delete")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_splits_delete)


def _render_splits(result, raw: bool) -> int:
    if result is None:
        return 1
    if raw:
        _print_raw(result)
        return 0
    rows = result if isinstance(result, list) else []
    if rows:
        columns = [
            ("id", "ID"), ("transactionId", "TxnID"), ("userId", "UserId"),
            ("contactId", "ContactId"), ("amount", "Amount"),
            ("isSettled", "Settled"), ("settledAt", "SettledAt"),
        ]
        print_table(rows, columns, max_widths={"userId": 36}, right_align={"id", "transactionId", "amount"})
        return 0
    _print_raw(result)
    return 0


def cmd_splits_list(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.SplitsApi(api_client)
        result = sdk_call_unwrapped(lambda: api.get_all2())
    return _render_splits(result, args.raw)


def cmd_splits_for_transaction(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.SplitsApi(api_client)
        result = sdk_call_unwrapped(lambda: api.get_by_transaction_id(transaction_id=int(args.transaction_id)))
    return _render_splits(result, args.raw)


def cmd_splits_for_user(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.SplitsApi(api_client)
        result = sdk_call_unwrapped(lambda: api.get_by_user_id(user_id=args.user_id))
    return _render_splits(result, args.raw)


def cmd_splits_unsettled(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.SplitsApi(api_client)
        result = sdk_call_unwrapped(lambda: api.get_my_unsettled(user_id=args.user_id))
    return _render_splits(result, args.raw)


def cmd_splits_for_contact(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.SplitsApi(api_client)
        result = sdk_call_unwrapped(lambda: api.get_by_contact_id(contact_id=int(args.contact_id)))
    return _render_splits(result, args.raw)


def cmd_splits_unsettled_contact(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.SplitsApi(api_client)
        result = sdk_call_unwrapped(lambda: api.get_unsettled_by_contact_id(contact_id=int(args.contact_id)))
    return _render_splits(result, args.raw)


def cmd_splits_create(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    is_settled = None
    if args.is_settled is not None:
        try:
            is_settled = int(args.is_settled)
        except Exception:
            is_settled = 1 if str(args.is_settled).lower() in {"1", "true", "yes"} else 0
    req = Split(
        transaction_id=int(args.transaction_id),
        user_id=args.user_id,
        amount=float(args.amount),
        contact_id=int(args.contact_id) if args.contact_id is not None else None,
        is_settled=is_settled,
    )
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.SplitsApi(api_client)
        result = sdk_call_unwrapped(lambda: api.create2(req))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_splits_get(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.SplitsApi(api_client)
        result = sdk_call_unwrapped(lambda: api.get_by_id1(id=int(args.id)))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_splits_settle(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.SplitsApi(api_client)
        result = sdk_call_unwrapped(lambda: api.settle(id=int(args.id)))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_splits_unsettle(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.SplitsApi(api_client)
        result = sdk_call_unwrapped(lambda: api.unsettle(id=int(args.id)))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_splits_delete(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.SplitsApi(api_client)
        result = sdk_call_unwrapped(lambda: api.delete2(id=int(args.id)))
    if result is None:
        return 1
    _print_raw(result)
    return 0
