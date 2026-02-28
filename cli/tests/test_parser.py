from tracko_cli.main import build_parser

def test_parser_basic_health():
    parser = build_parser()
    args = parser.parse_args(["health"])
    assert args.cmd == "health"

def test_parser_accounts_list():
    parser = build_parser()
    args = parser.parse_args(["accounts", "list"])
    assert args.cmd == "accounts"
    assert args.accounts_cmd == "list"
