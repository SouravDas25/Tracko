import argparse
from tracko_cli.core.config import get_token_from_args_or_config
from tracko_cli.core.client import TrackoClient
from tracko_cli.utils.formatting import print_result, print_table


def setup_parser(subparsers):
    sp = subparsers.add_parser("splits")
    sub_spl = sp.add_subparsers(dest="splits_cmd", required=True)

    sp2 = sub_spl.add_parser("list")
    sp2.set_defaults(func=cmd_splits_list)

    sp2 = sub_spl.add_parser("for-transaction")
    sp2.add_argument("--transaction-id", required=True, type=int)
    sp2.set_defaults(func=cmd_splits_for_transaction)

    sp2 = sub_spl.add_parser("for-user")
    sp2.add_argument("--user-id", required=True)
    sp2.set_defaults(func=cmd_splits_for_user)

    sp2 = sub_spl.add_parser("unsettled")
    sp2.add_argument("--user-id", required=True)
    sp2.set_defaults(func=cmd_splits_unsettled)

    sp2 = sub_spl.add_parser("for-contact")
    sp2.add_argument("--contact-id", required=True, type=int)
    sp2.set_defaults(func=cmd_splits_for_contact)

    sp2 = sub_spl.add_parser("unsettled-contact")
    sp2.add_argument("--contact-id", required=True, type=int)
    sp2.set_defaults(func=cmd_splits_unsettled_contact)

    sp2 = sub_spl.add_parser("create")
    sp2.add_argument("--transaction-id", required=True, type=int)
    sp2.add_argument("--user-id", required=True)
    sp2.add_argument("--amount", required=True, type=float)
    sp2.add_argument("--contact-id", type=int)
    sp2.add_argument("--is-settled")
    sp2.add_argument("--settled-at")
    sp2.set_defaults(func=cmd_splits_create)

    sp2 = sub_spl.add_parser("get")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_splits_get)

    sp2 = sub_spl.add_parser("settle")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_splits_settle)

    sp2 = sub_spl.add_parser("unsettle")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_splits_unsettle)

    sp2 = sub_spl.add_parser("delete")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_splits_delete)


def render_splits_result(result: dict, raw: bool) -> int:
    if raw:
        print_result(result, raw=True)
        return 0 if result.get("ok") else 1
    payload = result.get("json")
    rows = None
    if isinstance(payload, dict):
        rows = payload.get("result")
    if result.get("ok") and isinstance(rows, list):
        print(f"HTTP {result.get('status')} ({result.get('elapsed_ms')}ms)")
        columns = [
            ("id", "ID"),
            ("transactionId", "TxnID"),
            ("userId", "UserId"),
            ("contactId", "ContactId"),
            ("amount", "Amount"),
            ("isSettled", "Settled"),
            ("settledAt", "SettledAt"),
        ]
        print_table(
            rows,
            columns,
            max_widths={
                "userId": 36,
            },
            right_align={"id", "transactionId", "amount"},
        )
        return 0
    print_result(result, raw=False)
    return 0 if result.get("ok") else 1


def cmd_splits_list(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    result = client.get("/api/splits")
    return render_splits_result(result, raw=args.raw)


def cmd_splits_for_transaction(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    result = client.get(f"/api/splits/transaction/{args.transaction_id}")
    return render_splits_result(result, raw=args.raw)


def cmd_splits_for_user(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    result = client.get(f"/api/splits/user/{args.user_id}")
    return render_splits_result(result, raw=args.raw)


def cmd_splits_unsettled(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    result = client.get(f"/api/splits/user/{args.user_id}/unsettled")
    return render_splits_result(result, raw=args.raw)


def cmd_splits_create(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)

    body = {
        "transactionId": args.transaction_id,
        "userId": args.user_id,
        "amount": float(args.amount),
    }
    if args.contact_id is not None:
        body["contactId"] = int(args.contact_id)
    if args.is_settled is not None:
        try:
            body["isSettled"] = int(args.is_settled)
        except Exception:
            body["isSettled"] = (
                1 if str(args.is_settled).lower() in {"1", "true", "yes"} else 0
            )
    if args.settled_at:
        body["settledAt"] = args.settled_at
    result = client.post("/api/splits", json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_splits_for_contact(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    result = client.get(f"/api/splits/contact/{args.contact_id}")
    return render_splits_result(result, raw=args.raw)


def cmd_splits_unsettled_contact(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    result = client.get(f"/api/splits/contact/{args.contact_id}/unsettled")
    return render_splits_result(result, raw=args.raw)


def cmd_splits_get(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    result = client.get(f"/api/splits/{int(args.id)}")
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_splits_settle(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    result = client.patch(f"/api/splits/settle/{int(args.id)}")
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_splits_unsettle(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    result = client.patch(f"/api/splits/unsettle/{int(args.id)}")
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_splits_delete(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    result = client.delete(f"/api/splits/{int(args.id)}")
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1
