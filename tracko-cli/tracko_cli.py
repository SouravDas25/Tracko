import argparse
import csv
import datetime
import json
import os
import sys
import time
import urllib.parse

from tracko_cli.core.config import config_path, get_token_from_args_or_config, load_config, save_config
from tracko_cli.core.http import DEFAULT_BASE_URL, http_request, join_url
from tracko_cli.utils.dates import parse_date_to_epoch_ms
from tracko_cli.utils.formatting import print_result, print_table
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



from tracko_cli.commands.auth import cmd_login, cmd_oauth_token, cmd_logout
from tracko_cli.commands.users import (
    cmd_users_list, cmd_users_me, cmd_users_get, cmd_users_find_phone, cmd_users_upsert
)
from tracko_cli.commands.accounts import (
    cmd_accounts_list, cmd_accounts_add, cmd_accounts_get, cmd_accounts_update,
    cmd_accounts_delete, cmd_accounts_summary, cmd_accounts_transactions, cmd_accounts_balances
)
from tracko_cli.commands.categories import (
    cmd_categories_list, cmd_categories_add, cmd_categories_get, cmd_categories_update, cmd_categories_delete
)
from tracko_cli.commands.contacts import (
    cmd_contacts_list, cmd_contacts_add, cmd_contacts_get, cmd_contacts_update, cmd_contacts_delete
)
from tracko_cli.commands.transactions import (
    cmd_transactions_get, cmd_transactions_update, cmd_transactions_delete, cmd_transactions_list,
    cmd_transactions_add, cmd_transactions_import_csv, cmd_transfers_create, cmd_transactions_summary,
    cmd_transactions_total_income, cmd_transactions_total_expense
)
from tracko_cli.commands.splits import (
    cmd_splits_list, cmd_splits_for_transaction, cmd_splits_for_user, cmd_splits_unsettled,
    cmd_splits_create, cmd_splits_for_contact, cmd_splits_unsettled_contact, cmd_splits_get,
    cmd_splits_settle, cmd_splits_unsettle, cmd_splits_delete
)
from tracko_cli.commands.budget import (
    cmd_budget_view, cmd_budget_allocate, cmd_budget_available, cmd_budget_current
)
from tracko_cli.commands.currencies import (
    cmd_currencies_list, cmd_currencies_add, cmd_currencies_update, cmd_currencies_delete
)
from tracko_cli.commands.stats import cmd_stats_summary, cmd_stats_category_summary
from tracko_cli.commands.health import cmd_health
from tracko_cli.commands.misc import (
    cmd_exchange_rates_get, cmd_json_store_list, cmd_json_store_get, cmd_json_store_create,
    cmd_json_store_update, cmd_json_store_delete
)


def build_parser() -> argparse.ArgumentParser:
    examples = (
        "Examples:\n"
        "  # Health\n"
        "  tracko_cli health\n\n"
        "  # Auth\n"
        "  tracko_cli login --username user@example.com --password password\n"
        "  tracko_cli --base-url http://192.168.1.10:8080 login --username user@example.com --password password\n"
        "  tracko_cli logout\n\n"
        "  # Users\n"
        "  tracko_cli users list\n\n"
        "  # Accounts\n"
        "  tracko_cli accounts list\n"
        "  tracko_cli accounts balances\n"
        "  tracko_cli accounts add --name HDFC\n\n"
        "  # Categories\n"
        "  tracko_cli categories list\n"
        "  tracko_cli categories add --name FOOD\n\n"
        "  # Contacts\n"
        "  tracko_cli contacts list\n"
        "  tracko_cli contacts add --name Alice --phone 99999 --email alice@example.com\n"
        "  tracko_cli contacts update --id 1 --name 'Alice B'\n"
        "  tracko_cli contacts delete --id 1\n\n"
        "  # Transactions\n"
        "  tracko_cli transactions list\n"
        "  tracko_cli transactions summary --start-date 2026-01-01 --end-date 2026-12-31\n"
        "  tracko_cli transactions add --account-id 2 --category-id 2 --amount 250 --type expense --name Lunch --comments 'Team lunch'\n"
        "  tracko_cli transactions get --id 1\n"
        "  tracko_cli transactions update --id 1 --account-id 2 --category-id 2 --amount 300 --type expense --name 'Lunch (updated)' --comments 'Updated from CLI'\n"
        "  tracko_cli transactions delete --id 1\n\n"
        "  # Transfers (now uses unified transactions API)\n"
        "  tracko_cli transfers create --from-account-id 2 --to-account-id 3 --amount 500 --name 'Move to Savings' --comments 'Feb savings'\n\n"
        "  # Splits (list)\n"
        "  tracko_cli splits list\n"
        "  tracko_cli splits for-transaction --transaction-id 6\n"
        "  tracko_cli splits for-user --user-id 575e15bc-...\n"
        "  tracko_cli splits unsettled --user-id 575e15bc-...\n"
        "  tracko_cli splits for-contact --contact-id 1\n"
        "  tracko_cli splits unsettled-contact --contact-id 1\n\n"
        "  # Splits (create)\n"
        "  tracko_cli splits create --transaction-id 6 --user-id 575e15bc-... --amount 125 --contact-id 1\n\n"
        "  # Budget\n"
        "  tracko_cli budget view --month 2 --year 2026\n"
        "  tracko_cli budget allocate --category-id 1 --amount 500 --month 2 --year 2026\n"
        "  tracko_cli budget available --month 2 --year 2026\n\n"
        "  # Currency settings\n"
        "  tracko_cli currencies list\n"
        "  tracko_cli currencies add --code USD --rate 0.85\n\n"
    )
    p = argparse.ArgumentParser(
        prog="tracko_cli",
        description="Tracko CLI (dev/admin tool)",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=examples,
    )
    p.add_argument("--base-url", default=load_config().get("base_url", DEFAULT_BASE_URL))
    p.add_argument("--token", default=None, help="Override token (otherwise uses saved token)")
    p.add_argument("--raw", action="store_true", help="Print raw response body")

    sub = p.add_subparsers(dest="cmd", required=True)

    sp = sub.add_parser("health")
    sp.set_defaults(func=cmd_health)

    sp = sub.add_parser("login", help="Login via /api/login")
    sp.add_argument("--username", required=True)
    sp.add_argument("--password", required=True)
    sp.set_defaults(func=cmd_login)

    sp = sub.add_parser("oauth-token", help="Login via /api/oauth/token")
    sp.add_argument("--phone-no", required=True)
    sp.add_argument("--password", required=True)
    sp.set_defaults(func=cmd_oauth_token)

    sp = sub.add_parser("logout")
    sp.set_defaults(func=cmd_logout)

    sp = sub.add_parser("users")
    sub_users = sp.add_subparsers(dest="users_cmd", required=True)
    sp2 = sub_users.add_parser("list")
    sp2.set_defaults(func=cmd_users_list)

    sp2 = sub_users.add_parser("me")
    sp2.set_defaults(func=cmd_users_me)

    sp2 = sub_users.add_parser("get")
    sp2.add_argument("--id", required=True, help="User id")
    sp2.set_defaults(func=cmd_users_get)

    sp2 = sub_users.add_parser("find-phone")
    sp2.add_argument("--phone-no", required=True)
    sp2.set_defaults(func=cmd_users_find_phone)

    sp2 = sub_users.add_parser("upsert")
    sp2.add_argument("--phone-no", required=True)
    sp2.add_argument("--password", required=True, help="Password")
    sp2.add_argument("--name")
    sp2.add_argument("--email")
    sp2.add_argument("--profile-pic")
    sp2.add_argument("--base-currency")
    sh = sp2.add_mutually_exclusive_group()
    sh.add_argument("--shadow", action="store_true", default=None)
    sh.add_argument("--not-shadow", action="store_false", dest="shadow")
    sp2.set_defaults(func=cmd_users_upsert)

    sp2 = sub_users.add_parser("create")
    sp2.add_argument("--phone-no", required=True)
    sp2.add_argument("--password", required=True, help="Password")
    sp2.add_argument("--name")
    sp2.add_argument("--email")
    sp2.add_argument("--profile-pic")
    sp2.add_argument("--base-currency")
    sh = sp2.add_mutually_exclusive_group()
    sh.add_argument("--shadow", action="store_true", default=None)
    sh.add_argument("--not-shadow", action="store_false", dest="shadow")
    sp2.set_defaults(func=cmd_users_upsert)

    sp = sub.add_parser("accounts")
    sub_acc = sp.add_subparsers(dest="accounts_cmd", required=True)
    sp2 = sub_acc.add_parser("list")
    sp2.set_defaults(func=cmd_accounts_list)
    sp2 = sub_acc.add_parser("add")
    sp2.add_argument("--name", required=True)
    sp2.add_argument("--currency")
    sp2.set_defaults(func=cmd_accounts_add)

    sp2 = sub_acc.add_parser("get")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_accounts_get)

    sp2 = sub_acc.add_parser("update")
    sp2.add_argument("--id", required=True, type=int)
    sp2.add_argument("--name", required=True)
    sp2.add_argument("--currency")
    sp2.set_defaults(func=cmd_accounts_update)

    sp2 = sub_acc.add_parser("delete")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_accounts_delete)

    sp2 = sub_acc.add_parser("summary")
    sp2.add_argument("--id", required=True, type=int)
    sp2.add_argument("--start-date", required=True, help="Start date (YYYY-MM-DD)")
    sp2.add_argument("--end-date", required=True, help="End date (YYYY-MM-DD)")
    sp2.add_argument("--include-rollover", action="store_true")
    sp2.set_defaults(func=cmd_accounts_summary)

    sp2 = sub_acc.add_parser("transactions")
    sp2.add_argument("--id", required=True, type=int)
    sp2.add_argument("--month", type=int)
    sp2.add_argument("--year", type=int)
    sp2.add_argument("--start-date")
    sp2.add_argument("--end-date")
    sp2.add_argument("--category-id", type=int)
    sp2.add_argument("--page", type=int, default=0)
    sp2.add_argument("--size", type=int, default=500)
    sp2.add_argument("--expand", action="store_true")
    sp2.set_defaults(func=cmd_accounts_transactions)

    sp2 = sub_acc.add_parser("balances")
    sp2.set_defaults(func=cmd_accounts_balances)

    sp = sub.add_parser("categories")
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
    sp2.add_argument("--category-type")
    sp2.set_defaults(func=cmd_categories_update)

    sp2 = sub_cat.add_parser("delete")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_categories_delete)

    # contacts
    sp = sub.add_parser("contacts")
    sub_ct = sp.add_subparsers(dest="contacts_cmd", required=True)
    sp2 = sub_ct.add_parser("list")
    sp2.set_defaults(func=cmd_contacts_list)

    sp2 = sub_ct.add_parser("get")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_contacts_get)

    sp2 = sub_ct.add_parser("add")
    sp2.add_argument("--name", required=True)
    sp2.add_argument("--phone")
    sp2.add_argument("--email")
    sp2.set_defaults(func=cmd_contacts_add)

    sp2 = sub_ct.add_parser("update")
    sp2.add_argument("--id", required=True, type=int)
    sp2.add_argument("--name", required=True)
    sp2.add_argument("--phone")
    sp2.add_argument("--email")
    sp2.set_defaults(func=cmd_contacts_update)

    sp2 = sub_ct.add_parser("delete")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_contacts_delete)

    # transfers
    sp = sub.add_parser("transfers")
    sub_tr = sp.add_subparsers(dest="transfers_cmd", required=True)
    sp2 = sub_tr.add_parser("create")
    sp2.add_argument("--from-account-id", required=True, type=int)
    sp2.add_argument("--to-account-id", required=True, type=int)
    sp2.add_argument("--amount", required=True, type=float)
    sp2.add_argument("--name")
    sp2.add_argument("--comments")
    sp2.set_defaults(func=cmd_transfers_create)

    # splits
    sp = sub.add_parser("splits")
    sub_spl = sp.add_subparsers(dest="splits_cmd", required=True)
    sp2 = sub_spl.add_parser("list")
    sp2.set_defaults(func=cmd_splits_list)

    sp2 = sub_spl.add_parser("get")
    sp2.add_argument("--id", type=int, required=True)
    sp2.set_defaults(func=cmd_splits_get)

    sp2 = sub_spl.add_parser("for-transaction")
    sp2.add_argument("--transaction-id", type=int, required=True)
    sp2.set_defaults(func=cmd_splits_for_transaction)

    sp2 = sub_spl.add_parser("for-user")
    sp2.add_argument("--user-id", required=True)
    sp2.set_defaults(func=cmd_splits_for_user)

    sp2 = sub_spl.add_parser("unsettled")
    sp2.add_argument("--user-id", required=True)
    sp2.set_defaults(func=cmd_splits_unsettled)

    sp2 = sub_spl.add_parser("for-contact")
    sp2.add_argument("--contact-id", type=int, required=True)
    sp2.set_defaults(func=cmd_splits_for_contact)

    sp2 = sub_spl.add_parser("unsettled-contact")
    sp2.add_argument("--contact-id", type=int, required=True)
    sp2.set_defaults(func=cmd_splits_unsettled_contact)

    sp2 = sub_spl.add_parser("create")
    sp2.add_argument("--transaction-id", type=int, required=True)
    sp2.add_argument("--user-id", required=True)
    sp2.add_argument("--amount", required=True)
    sp2.add_argument("--contact-id", type=int)
    sp2.add_argument("--is-settled")
    sp2.add_argument("--settled-at")
    sp2.set_defaults(func=cmd_splits_create)

    sp2 = sub_spl.add_parser("settle")
    sp2.add_argument("--id", type=int, required=True)
    sp2.set_defaults(func=cmd_splits_settle)

    sp2 = sub_spl.add_parser("unsettle")
    sp2.add_argument("--id", type=int, required=True)
    sp2.set_defaults(func=cmd_splits_unsettle)

    sp2 = sub_spl.add_parser("delete")
    sp2.add_argument("--id", type=int, required=True)
    sp2.set_defaults(func=cmd_splits_delete)

    sp = sub.add_parser("transactions", help="Transactions operations")
    sub_tx = sp.add_subparsers(dest="transactions_cmd", required=True)
    sp2 = sub_tx.add_parser("list", help="List transactions (paginated) for a given month/year")
    sp2.add_argument("--month", type=int, help="Month (1-12). Defaults to current month.")
    sp2.add_argument("--year", type=int, help="Year (YYYY). Defaults to current year.")
    sp2.add_argument("--page", type=int, default=0, help="Zero-based page index (default: 0).")
    sp2.add_argument("--size", type=int, default=500, help="Page size (default: 500).")
    sp2.set_defaults(func=cmd_transactions_list)

    sp2 = sub_tx.add_parser("add", help="Create a transaction")
    sp2.add_argument("--account-id", type=int, help="Account ID (alternative to --account-name)")
    sp2.add_argument("--account-name", help="Account name (alternative to --account-id)")
    sp2.add_argument("--category-id", type=int, help="Category ID (alternative to --category-name)")
    sp2.add_argument("--category-name", help="Category name (alternative to --category-id)")
    sp2.add_argument("--amount", required=True, type=float)
    sp2.add_argument("--type", required=True, choices=["income", "expense"], help="income -> CREDIT(2), expense -> DEBIT(1)")
    sp2.add_argument("--name", required=True, help="Transaction title/name")
    sp2.add_argument("--comments", default=None)
    sp2.add_argument("--date", default=None, help="YYYY-MM-DD or ISO-8601 or epoch-ms (default: now)")
    sp2.add_argument("--countable", action="store_true", default=True)
    sp2.add_argument("--not-countable", action="store_false", dest="countable")
    # Multi-currency support: when provided, --amount is treated as original amount
    sp2.add_argument("--currency", help="Original currency code (e.g., USD, EUR). Backend will fetch the configured exchange rate.")
    sp2.add_argument("--exchange-rate", type=float, help="Optional: explicit exchange rate. If omitted, backend uses user's configured rate.")
    sp2.set_defaults(func=cmd_transactions_add)

    sample_csv = (
        "Sample CSV Format:\n"
        "Date,Transaction Type,Description,Amount\n"
        "20/01/2025,DEBIT,Lunch,150.00\n"
        "21/01/2025,CREDIT,Salary,50000.00\n"
        "\n"
        "Notes:\n"
        "- Date format: DD/MM/YYYY\n"
        "- Amount: Negative for expense, positive for income (or use Transaction Type column if amount is absolute)\n"
        "- Transaction Type: Optional. Used for comments.\n"
    )
    sp2 = sub_tx.add_parser(
        "import-csv", 
        help="Import transactions from a CSV file",
        description=sample_csv,
        formatter_class=argparse.RawDescriptionHelpFormatter
    )
    sp2.add_argument("--file", required=True, help="Path to CSV file")
    sp2.add_argument("--account-id", required=True, type=int, help="Target Account ID")
    # category-id is now automatic
    sp2.set_defaults(func=cmd_transactions_import_csv)

    sp2 = sub_tx.add_parser("get", help="Get a transaction by id")
    sp2.add_argument("--id", required=True, type=int, help="Transaction ID")
    sp2.set_defaults(func=cmd_transactions_get)

    sp2 = sub_tx.add_parser("update", help="Update a transaction by id")
    sp2.add_argument("--id", required=True, type=int, help="Transaction ID")
    sp2.add_argument("--account-id", type=int)
    sp2.add_argument("--category-id", type=int)
    sp2.add_argument("--amount", type=float)
    sp2.add_argument("--type", choices=["income", "expense"], help="income -> CREDIT(2), expense -> DEBIT(1)")
    sp2.add_argument("--name", help="Transaction title/name")
    sp2.add_argument("--comments", default=None)
    sp2.add_argument("--date", default=None, help="YYYY-MM-DD or ISO-8601 or epoch-ms (default: no change)")
    sp2.add_argument("--countable", action="store_true", default=None)
    sp2.add_argument("--not-countable", action="store_false", dest="countable")
    sp2.add_argument("--currency", help="Original currency code (e.g., USD, EUR). Backend will fetch the configured exchange rate.")
    sp2.add_argument("--exchange-rate", type=float, help="Optional: explicit exchange rate. If omitted, backend uses user's configured rate.")
    sp2.set_defaults(func=cmd_transactions_update)

    sp2 = sub_tx.add_parser("delete", help="Delete a transaction by id")
    sp2.add_argument("--id", required=True, type=int, help="Transaction ID")
    sp2.set_defaults(func=cmd_transactions_delete)

    sp2 = sub_tx.add_parser("summary", help="Get income/expense/balance summary for a date range")
    sp2.add_argument("--start-date", help="Start date (YYYY-MM-DD). Defaults to current year start.")
    sp2.add_argument("--end-date", help="End date (YYYY-MM-DD). Defaults to current year end.")
    sp2.add_argument("--account-ids", help="Comma-separated account IDs to filter by")
    sp2.add_argument("--include-rollover", action="store_true", help="Include rollover in calculations")
    sp2.set_defaults(func=cmd_transactions_summary)

    sp2 = sub_tx.add_parser("total-income", help="Get total income for a date range")
    sp2.add_argument("--start-date", required=True, help="Start date (YYYY-MM-DD)")
    sp2.add_argument("--end-date", required=True, help="End date (YYYY-MM-DD)")
    sp2.set_defaults(func=cmd_transactions_total_income)

    sp2 = sub_tx.add_parser("total-expense", help="Get total expense for a date range")
    sp2.add_argument("--start-date", required=True, help="Start date (YYYY-MM-DD)")
    sp2.add_argument("--end-date", required=True, help="End date (YYYY-MM-DD)")
    sp2.set_defaults(func=cmd_transactions_total_expense)

    # budget
    sp = sub.add_parser("budget")
    sub_budget = sp.add_subparsers(dest="budget_cmd", required=True)

    sp2 = sub_budget.add_parser("view")
    sp2.add_argument("--month", type=int, help="Month (1-12)")
    sp2.add_argument("--year", type=int, help="Year (YYYY)")
    sp2.set_defaults(func=cmd_budget_view)

    sp2 = sub_budget.add_parser("allocate")
    sp2.add_argument("--category-id", required=True, type=int)
    sp2.add_argument("--amount", required=True, type=float)
    sp2.add_argument("--month", type=int, help="Month (1-12)")
    sp2.add_argument("--year", type=int, help="Year (YYYY)")
    sp2.set_defaults(func=cmd_budget_allocate)

    sp2 = sub_budget.add_parser("available")
    sp2.add_argument("--month", type=int, help="Month (1-12)")
    sp2.add_argument("--year", type=int, help="Year (YYYY)")
    sp2.set_defaults(func=cmd_budget_available)

    sp2 = sub_budget.add_parser("current")
    sp2.set_defaults(func=cmd_budget_current)

    sp = sub.add_parser("stats")
    sub_stats = sp.add_subparsers(dest="stats_cmd", required=True)

    sp2 = sub_stats.add_parser("summary")
    sp2.add_argument("--range", required=True, choices=["weekly", "monthly", "yearly"])
    sp2.add_argument("--transaction-type", required=True, type=int, choices=[1, 2])
    sp2.add_argument("--date", help="Anchor date (YYYY-MM-DD)")
    sp2.set_defaults(func=cmd_stats_summary)

    sp2 = sub_stats.add_parser("category-summary")
    sp2.add_argument("--range", required=True, choices=["weekly", "monthly", "yearly"])
    sp2.add_argument("--transaction-type", required=True, type=int, choices=[1, 2])
    sp2.add_argument("--category-id", required=True, type=int)
    sp2.add_argument("--date", help="Anchor date (YYYY-MM-DD)")
    sp2.set_defaults(func=cmd_stats_category_summary)

    sp = sub.add_parser("exchange-rates")
    sub_er = sp.add_subparsers(dest="exchange_rates_cmd", required=True)
    sp2 = sub_er.add_parser("get")
    sp2.add_argument("--base-currency", required=True)
    sp2.set_defaults(func=cmd_exchange_rates_get)

    sp = sub.add_parser("json-store")
    sub_js = sp.add_subparsers(dest="json_store_cmd", required=True)

    sp2 = sub_js.add_parser("list")
    sp2.set_defaults(func=cmd_json_store_list)

    sp2 = sub_js.add_parser("get")
    sp2.add_argument("--name", required=True)
    sp2.set_defaults(func=cmd_json_store_get)

    sp2 = sub_js.add_parser("create")
    sp2.add_argument("--name", required=True)
    sp2.add_argument("--value", required=True)
    sp2.set_defaults(func=cmd_json_store_create)

    sp2 = sub_js.add_parser("update")
    sp2.add_argument("--name", required=True)
    sp2.add_argument("--value", required=True)
    sp2.set_defaults(func=cmd_json_store_update)

    sp2 = sub_js.add_parser("delete")
    sp2.add_argument("--name", required=True)
    sp2.set_defaults(func=cmd_json_store_delete)

    # user currencies
    sp = sub.add_parser("currencies")
    sub_curr = sp.add_subparsers(dest="currencies_cmd", required=True)

    sp2 = sub_curr.add_parser("list")
    sp2.set_defaults(func=cmd_currencies_list)

    sp2 = sub_curr.add_parser("add")
    sp2.add_argument("--code", required=True, help="Currency code, e.g., USD")
    sp2.add_argument("--rate", required=True, type=float, help="Exchange rate to base per 1 unit of this currency")
    sp2.set_defaults(func=cmd_currencies_add)

    sp2 = sub_curr.add_parser("update")
    sp2.add_argument("--code", required=True, help="Currency code, e.g., USD")
    sp2.add_argument("--rate", required=True, type=float, help="Exchange rate to base per 1 unit of this currency")
    sp2.set_defaults(func=cmd_currencies_update)

    sp2 = sub_curr.add_parser("delete")
    sp2.add_argument("--code", required=True, help="Currency code to delete, e.g., USD")
    sp2.set_defaults(func=cmd_currencies_delete)

    return p


def main(argv: list[str]) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    return int(args.func(args))


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
