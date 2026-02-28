import argparse
import sys
from tracko_cli.core.config import load_config, save_config, config_path
from tracko_cli.core.http import http_request, join_url
from tracko_cli.utils.formatting import print_result


def cmd_login(args: argparse.Namespace) -> int:
    url = join_url(args.base_url, "/api/login")
    body = {"username": args.username, "password": args.password}
    result = http_request("POST", url, json_body=body)
    print_result(result, raw=args.raw)

    token = None
    if isinstance(result.get("json"), dict):
        token = result["json"].get("token")

    if result["ok"] and token:
        cfg = load_config()
        cfg["base_url"] = args.base_url
        cfg["token"] = token
        save_config(cfg)
        print("Saved token to", config_path())
        return 0

    return 1


def cmd_oauth_token(args: argparse.Namespace) -> int:
    url = join_url(args.base_url, "/api/oauth/token")
    body = {"phoneNo": args.phone_no, "password": args.password}
    result = http_request("POST", url, json_body=body)
    print_result(result, raw=args.raw)

    token = None
    if isinstance(result.get("json"), dict):
        token = result["json"].get("token")

    if result["ok"] and token:
        cfg = load_config()
        cfg["base_url"] = args.base_url
        cfg["token"] = token
        save_config(cfg)
        print("Saved token to", config_path())
        return 0

    return 1


def cmd_logout(args: argparse.Namespace) -> int:
    cfg = load_config()
    if "token" in cfg:
        cfg.pop("token", None)
        save_config(cfg)
    print("Logged out (token removed)")
    return 0
