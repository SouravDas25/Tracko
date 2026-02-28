from tracko_cli.commands import (
    accounts,
    auth,
    budget,
    categories,
    contacts,
    currencies,
    health,
    misc,
    splits,
    stats,
    transactions,
    users,
)

import argparse
import os
import sys

from tracko_cli.core.config import (
    config_path,
    get_token_from_args_or_config,
)
from tracko_cli.core.http import join_url
from tracko_cli.utils.dates import parse_date_to_epoch_ms
from tracko_cli.utils.lookups import get_id_name_map


# Optional: python-dateutil for robust datetime parsing
try:
    from dateutil import parser as date_parser
    from dateutil import tz as date_tz
except Exception:
    date_parser = None
    date_tz = None


def _config_path() -> str:
    return os.path.abspath(config_path())


def _join_url(base_url: str, path: str) -> str:
    return join_url(base_url, path)


def _get_token_from_args_or_config(args: argparse.Namespace) -> tuple[str | None, str]:
    return get_token_from_args_or_config(args)


def _get_id_name_map(base_url: str, token: str | None, path: str) -> dict[int, str]:
    return get_id_name_map(base_url, token, path)


def _parse_date_to_epoch_ms(date_str: str | None) -> int:
    return parse_date_to_epoch_ms(date_str)




def build_parser() -> argparse.ArgumentParser:
    examples = """Examples:
  # Health
  python -m tracko_cli health

  # Auth
  python -m tracko_cli login --username user@example.com --password password
  python -m tracko_cli --base-url http://192.168.1.10:8080 login --username user@example.com --password password
  python -m tracko_cli logout

  # Users
  python -m tracko_cli users list

  # Accounts
  python -m tracko_cli accounts list
  python -m tracko_cli accounts balances
  python -m tracko_cli accounts add --name HDFC

  # Categories
  python -m tracko_cli categories list
  python -m tracko_cli categories add --name FOOD

  # Contacts
  python -m tracko_cli contacts list
  python -m tracko_cli contacts add --name Alice --phone 99999 --email alice@example.com
  python -m tracko_cli contacts update --id 1 --name 'Alice B'
  python -m tracko_cli contacts delete --id 1

  # Transactions
  python -m tracko_cli transactions list
  python -m tracko_cli transactions summary --start-date 2026-01-01 --end-date 2026-12-31
  python -m tracko_cli transactions add --account-id 2 --category-id 2 --amount 250 --type expense --name Lunch --comments 'Team lunch'
  python -m tracko_cli transactions get --id 1
  python -m tracko_cli transactions update --id 1 --account-id 2 --category-id 2 --amount 300 --type expense --name 'Lunch (updated)' --comments 'Updated from CLI'
  python -m tracko_cli transactions delete --id 1

  # Transfers (now uses unified transactions API)
  python -m tracko_cli transfers create --from-account-id 2 --to-account-id 3 --amount 500 --name 'Move to Savings' --comments 'Feb savings'

  # Splits (list)
  python -m tracko_cli splits list
  python -m tracko_cli splits for-transaction --transaction-id 6
  python -m tracko_cli splits for-user --user-id 575e15bc-...
  python -m tracko_cli splits unsettled --user-id 575e15bc-...
  python -m tracko_cli splits for-contact --contact-id 1
  python -m tracko_cli splits unsettled-contact --contact-id 1

  # Splits (create)
  python -m tracko_cli splits create --transaction-id 6 --user-id 575e15bc-... --amount 125 --contact-id 1

  # Budget
  python -m tracko_cli budget view --month 2 --year 2026
  python -m tracko_cli budget allocate --category-id 1 --amount 500 --month 2 --year 2026
  python -m tracko_cli budget available --month 2 --year 2026

  # Currency settings
  python -m tracko_cli currencies list
  python -m tracko_cli currencies add --code USD --rate 0.85
"""
    p = argparse.ArgumentParser(
        description="Tracko CLI (dev/admin tool)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=examples,
        prog="tracko_cli",
    )
    p.add_argument("--base-url", default=None)
    p.add_argument(
        "--token", default=None, help="Override token (otherwise uses saved token)"
    )
    p.add_argument("--raw", action="store_true", help="Print raw response body")

    sub = p.add_subparsers(dest="cmd", required=True)

    # Register module parsers
    health.setup_parser(sub)
    auth.setup_parser(sub)
    users.setup_parser(sub)
    accounts.setup_parser(sub)
    categories.setup_parser(sub)
    contacts.setup_parser(sub)
    transactions.setup_parser(sub)
    transactions.setup_transfers_parser(sub)
    splits.setup_parser(sub)
    budget.setup_parser(sub)
    stats.setup_parser(sub)
    currencies.setup_parser(sub)
    misc.setup_parser(sub)

    return p


def main(argv: list[str]) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    return int(args.func(args))


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
