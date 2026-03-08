import argparse
import json

from ..core.config import get_token_from_args_or_config
from ..core.api import make_api_client, sdk_call_unwrapped

import tracko_sdk


def setup_parser(subparsers):
    sp = subparsers.add_parser("health", help="Check API health")
    sp.set_defaults(func=cmd_health)


def cmd_health(args: argparse.Namespace) -> int:
    _, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url) as api_client:
        api = tracko_sdk.HealthApi(api_client)
        result = sdk_call_unwrapped(lambda: api.health())
    if result is None:
        return 1
    if hasattr(result, "to_dict"):
        print(json.dumps(result.to_dict(), indent=2, default=str))
    else:
        print(json.dumps(result, indent=2, default=str))
    return 0
