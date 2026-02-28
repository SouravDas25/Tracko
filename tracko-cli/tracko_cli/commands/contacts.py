import argparse
from tracko_cli.core.config import get_token_from_args_or_config
from tracko_cli.core.http import http_request, join_url
from tracko_cli.utils.formatting import print_result, print_table


def cmd_contacts_list(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, "/api/contacts")
    result = http_request("GET", url, token=token)
    if args.raw:
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
            ("name", "Name"),
            ("phoneNo", "Phone"),
            ("email", "Email"),
        ]
        print_table(
            rows,
            columns,
            max_widths={
                "name": 28,
                "phoneNo": 18,
                "email": 36,
            },
            right_align={"id"},
        )
        return 0
    print_result(result, raw=False)
    return 0 if result.get("ok") else 1


def cmd_contacts_add(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, "/api/contacts")
    body = {"name": args.name}
    if args.phone:
        body["phoneNo"] = args.phone
    if args.email:
        body["email"] = args.email
    result = http_request("POST", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_contacts_get(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, f"/api/contacts/{int(args.id)}")
    result = http_request("GET", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_contacts_update(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, f"/api/contacts/{args.id}")
    body = {"name": args.name}
    if args.phone is not None:
        body["phoneNo"] = args.phone
    if args.email is not None:
        body["email"] = args.email
    result = http_request("PUT", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_contacts_delete(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, f"/api/contacts/{args.id}")
    result = http_request("DELETE", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1
