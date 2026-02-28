import argparse
from tracko_cli.core.config import get_token_from_args_or_config
from tracko_cli.core.client import TrackoClient
from tracko_cli.utils.formatting import print_result


def setup_parser(subparsers):
    sp = subparsers.add_parser("health", help="Check API health")
    sp.set_defaults(func=cmd_health)


def cmd_health(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    client = TrackoClient(base_url)
    result = client.get("/api/health")
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1
