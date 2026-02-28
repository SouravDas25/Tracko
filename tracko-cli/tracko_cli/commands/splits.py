import argparse
from tracko_cli.core.config import get_token_from_args_or_config
from tracko_cli.core.http import http_request, join_url
from tracko_cli.utils.formatting import print_result, print_table


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
    url = join_url(base_url, "/api/splits")
    result = http_request("GET", url, token=token)
    return render_splits_result(result, raw=args.raw)


def cmd_splits_for_transaction(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, f"/api/splits/transaction/{args.transaction_id}")
    result = http_request("GET", url, token=token)
    return render_splits_result(result, raw=args.raw)


def cmd_splits_for_user(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, f"/api/splits/user/{args.user_id}")
    result = http_request("GET", url, token=token)
    return render_splits_result(result, raw=args.raw)


def cmd_splits_unsettled(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, f"/api/splits/user/{args.user_id}/unsettled")
    result = http_request("GET", url, token=token)
    return render_splits_result(result, raw=args.raw)


def cmd_splits_create(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, "/api/splits")
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
            body["isSettled"] = 1 if str(args.is_settled).lower() in {"1", "true", "yes"} else 0
    if args.settled_at:
        body["settledAt"] = args.settled_at
    result = http_request("POST", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_splits_for_contact(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, f"/api/splits/contact/{args.contact_id}")
    result = http_request("GET", url, token=token)
    return render_splits_result(result, raw=args.raw)


def cmd_splits_unsettled_contact(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, f"/api/splits/contact/{args.contact_id}/unsettled")
    result = http_request("GET", url, token=token)
    return render_splits_result(result, raw=args.raw)


def cmd_splits_get(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, f"/api/splits/{int(args.id)}")
    result = http_request("GET", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_splits_settle(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, f"/api/splits/settle/{int(args.id)}")
    result = http_request("PATCH", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_splits_unsettle(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, f"/api/splits/unsettle/{int(args.id)}")
    result = http_request("PATCH", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_splits_delete(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, f"/api/splits/{int(args.id)}")
    result = http_request("DELETE", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1
