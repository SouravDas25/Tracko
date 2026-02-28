import argparse
from tracko_cli.core.http import http_request, join_url
from tracko_cli.utils.formatting import print_result


def cmd_health(args: argparse.Namespace) -> int:
    url = join_url(args.base_url, "/api/health")
    result = http_request("GET", url)
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1
