import argparse
from tracko_cli.core.config import get_token_from_args_or_config
from tracko_cli.core.client import TrackoClient
from tracko_cli.utils.formatting import print_result, print_table


def setup_parser(subparsers):
    sp = subparsers.add_parser("categories")
    sub_cat = sp.add_subparsers(dest="categories_cmd", required=True)

    sp2 = sub_cat.add_parser("list")
    sp2.set_defaults(func=cmd_categories_list)

    sp2 = sub_cat.add_parser("add")
    sp2.add_argument("--name", required=True)
    sp2.set_defaults(func=cmd_categories_add)

    sp2 = sub_cat.add_parser("get")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_categories_get)

    sp2 = sub_cat.add_parser("update")
    sp2.add_argument("--id", required=True, type=int)
    sp2.add_argument("--name", required=True)
    sp2.add_argument(
        "--category-type", type=int, choices=[1, 2], help="1 for expense, 2 for income"
    )
    sp2.set_defaults(func=cmd_categories_update)

    sp2 = sub_cat.add_parser("delete")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_categories_delete)


def cmd_categories_list(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    result = client.get("/api/categories")
    if args.raw:
        print_result(result, raw=True)
        return 0 if result["ok"] else 1

    payload = result.get("json")
    if result.get("ok") and isinstance(payload, dict):
        from tracko_cli.core.models import CategoryListResponse
        try:
            resp_model = CategoryListResponse.model_validate(payload)
            rows = [cat.model_dump() for cat in resp_model.result]
            
            print(f"HTTP {result.get('status')} ({result.get('elapsed_ms')}ms)")
            columns = [
                ("id", "ID"),
                ("name", "Name"),
                ("userId", "UserId"),
            ]
            print_table(
                rows,
                columns,
                max_widths={
                    "name": 32,
                    "userId": 36,
                },
                right_align={"id"},
            )
            return 0
        except Exception as e:
            print(f"Warning: Failed to validate response format: {e}")
            pass

    print_result(result, raw=False)
    return 0 if result["ok"] else 1

def cmd_categories_add(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)

    body = {"name": args.name}
    result = client.post("/api/categories", json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1


def cmd_categories_get(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    result = client.get(f"/api/categories/{int(args.id)}")
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_categories_update(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)

    body: dict = {"name": args.name}
    if getattr(args, "category_type", None) is not None:
        body["categoryType"] = args.category_type
    result = client.put(f"/api/categories/{int(args.id)}", json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_categories_delete(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url, token)
    result = client.delete(f"/api/categories/{int(args.id)}")
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1
