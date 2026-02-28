from .commands import (
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

from .core.config import (
    config_path,
    get_token_from_args_or_config,
)
from .core.http import join_url
from .utils.dates import parse_date_to_epoch_ms
from .utils.lookups import get_id_name_map


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
  python -m cli health

  # Auth
  python -m cli login --username user@example.com --password password
  python -m cli --base-url http://192.168.1.10:8080 login --username user@example.com --password password
  python -m cli logout

  # Users
  python -m cli users list

  # Accounts
  python -m cli accounts list
  python -m cli accounts balances
  python -m cli accounts add --name HDFC

  # Categories
  python -m cli categories list
  python -m cli categories add --name FOOD

  # Contacts
  python -m cli contacts list
  python -m cli contacts add --name Alice --phone 99999 --email alice@example.com
  python -m cli contacts update --id 1 --name 'Alice B'
  python -m cli contacts delete --id 1

  # Transactions
  python -m cli transactions list
  python -m cli transactions summary --start-date 2026-01-01 --end-date 2026-12-31
  python -m cli transactions add --account-id 2 --category-id 2 --amount 250 --type expense --name Lunch --comments 'Team lunch'
  python -m cli transactions get --id 1
  python -m cli transactions update --id 1 --account-id 2 --category-id 2 --amount 300 --type expense --name 'Lunch (updated)' --comments 'Updated from CLI'
  python -m cli transactions delete --id 1

  # Transfers (now uses unified transactions API)
  python -m cli transfers create --from-account-id 2 --to-account-id 3 --amount 500 --name 'Move to Savings' --comments 'Feb savings'

  # Splits (list)
  python -m cli splits list
  python -m cli splits for-transaction --transaction-id 6
  python -m cli splits for-user --user-id 575e15bc-...
  python -m cli splits unsettled --user-id 575e15bc-...
  python -m cli splits for-contact --contact-id 1
  python -m cli splits unsettled-contact --contact-id 1

  # Splits (create)
  python -m cli splits create --transaction-id 6 --user-id 575e15bc-... --amount 125 --contact-id 1

  # Budget
  python -m cli budget view --month 2 --year 2026
  python -m cli budget allocate --category-id 1 --amount 500 --month 2 --year 2026
  python -m cli budget available --month 2 --year 2026

  # Currency settings
  python -m cli currencies list
  python -m cli currencies add --code USD --rate 0.85
"""
    p = argparse.ArgumentParser(
        description="Tracko CLI (dev/admin tool)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=examples,
        prog="cli",
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
