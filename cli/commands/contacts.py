import argparse
from ..core.config import get_token_from_args_or_config
from ..core.client import TrackoClient
from ..utils.formatting import print_result, print_table


def setup_parser(subparsers):
    sp = subparsers.add_parser("contacts")
    sub_con = sp.add_subparsers(dest="contacts_cmd", required=True)

    sp2 = sub_con.add_parser("list")
    sp2.set_defaults(func=cmd_contacts_list)

    sp2 = sub_con.add_parser("add")
    sp2.add_argument("--name", required=True)
    sp2.add_argument("--phone")
    sp2.add_argument("--email")
    sp2.set_defaults(func=cmd_contacts_add)

    sp2 = sub_con.add_parser("get")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_contacts_get)

    sp2 = sub_con.add_parser("update")
    sp2.add_argument("--id", required=True, type=int)
    sp2.add_argument("--name", required=True)
    sp2.add_argument("--phone")
    sp2.add_argument("--email")
    sp2.set_defaults(func=cmd_contacts_update)

    sp2 = sub_con.add_parser("delete")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_contacts_delete)


def cmd_contacts_list(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    result = client.get("/api/contacts")
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
    client = TrackoClient(base_url, token)

    body = {"name": args.name}
    if args.phone:
        body["phoneNo"] = args.phone
    if args.email:
        body["email"] = args.email
    result = client.post("/api/contacts", json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_contacts_get(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    result = client.get(f"/api/contacts/{int(args.id)}")
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_contacts_update(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)

    body = {"name": args.name}
    if args.phone is not None:
        body["phoneNo"] = args.phone
    if args.email is not None:
        body["email"] = args.email
    result = client.put(f"/api/contacts/{args.id}", json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_contacts_delete(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    result = client.delete(f"/api/contacts/{args.id}")
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1
