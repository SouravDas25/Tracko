import argparse
import csv
import datetime
import json
import sys

from ..core.config import get_token_from_args_or_config
from ..core.api import make_api_client, sdk_call

import tracko_sdk
from tracko_sdk.models.transaction_request import TransactionRequest
from ..utils.formatting import print_table
from ..utils.lookups import get_id_name_map


# ---------------------------------------------------------------------------
# Parser setup
# ---------------------------------------------------------------------------

def setup_parser(subparsers):
    sp = subparsers.add_parser("transactions")
    sub_tx = sp.add_subparsers(dest="transactions_cmd", required=True)

    sp2 = sub_tx.add_parser("list")
    sp2.add_argument("--month", type=int)
    sp2.add_argument("--year", type=int)
    sp2.add_argument("--page", type=int)
    sp2.add_argument("--size", type=int)
    sp2.set_defaults(func=cmd_transactions_list)

    sp2 = sub_tx.add_parser("add")
    sp2.add_argument("--account-id", type=int)
    sp2.add_argument("--account-name")
    sp2.add_argument("--category-id", type=int)
    sp2.add_argument("--category-name")
    sp2.add_argument("--amount", required=True, type=float)
    sp2.add_argument(
        "--type", required=True,
        choices=["income", "expense", "debit", "dr", "d", "credit", "cr", "c"],
    )
    sp2.add_argument("--name", required=True)
    sp2.add_argument("--comments", default=None)
    sp2.add_argument("--date", default=None)
    sp2.add_argument("--countable", action="store_true", default=None)
    sp2.add_argument("--not-countable", action="store_false", dest="countable")
    sp2.add_argument("--currency")
    sp2.add_argument("--exchange-rate", type=float)
    sp2.set_defaults(func=cmd_transactions_add)

    sp2 = sub_tx.add_parser("import-csv")
    sp2.add_argument("--file", required=True)
    sp2.add_argument("--account-id", required=True, type=int)
    sp2.set_defaults(func=cmd_transactions_import_csv)

    sp2 = sub_tx.add_parser("get")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_transactions_get)

    sp2 = sub_tx.add_parser("update")
    sp2.add_argument("--id", required=True, type=int)
    sp2.add_argument("--account-id", type=int)
    sp2.add_argument("--category-id", type=int)
    sp2.add_argument("--amount", type=float)
    sp2.add_argument("--type", choices=["income", "expense"])
    sp2.add_argument("--name")
    sp2.add_argument("--comments", default=None)
    sp2.add_argument("--date", default=None)
    sp2.add_argument("--countable", action="store_true", default=None)
    sp2.add_argument("--not-countable", action="store_false", dest="countable")
    sp2.add_argument("--currency")
    sp2.add_argument("--exchange-rate", type=float)
    sp2.add_argument("--to-account-id", type=int)
    sp2.add_argument("--from-account-id", type=int)
    sp2.set_defaults(func=cmd_transactions_update)

    sp2 = sub_tx.add_parser("delete")
    sp2.add_argument("--id", required=True, type=int)
    sp2.set_defaults(func=cmd_transactions_delete)

    sp2 = sub_tx.add_parser("summary")
    sp2.add_argument("--start-date")
    sp2.add_argument("--end-date")
    sp2.add_argument("--account-ids")
    sp2.add_argument("--include-rollover", action="store_true")
    sp2.set_defaults(func=cmd_transactions_summary)

    sp2 = sub_tx.add_parser("total-income")
    sp2.add_argument("--start-date", required=True)
    sp2.add_argument("--end-date", required=True)
    sp2.set_defaults(func=cmd_transactions_total_income)

    sp2 = sub_tx.add_parser("total-expense")
    sp2.add_argument("--start-date", required=True)
    sp2.add_argument("--end-date", required=True)
    sp2.set_defaults(func=cmd_transactions_total_expense)


def setup_transfers_parser(subparsers):
    sp = subparsers.add_parser("transfers")
    sub_tr = sp.add_subparsers(dest="transfers_cmd", required=True)
    sp2 = sub_tr.add_parser("create")
    sp2.add_argument("--from-account-id", required=True, type=int)
    sp2.add_argument("--to-account-id", required=True, type=int)
    sp2.add_argument("--amount", required=True, type=float)
    sp2.add_argument("--name", default=None)
    sp2.add_argument("--comments", default=None)
    sp2.set_defaults(func=cmd_transfers_create)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _parse_type(type_str: str) -> str:
    """Map CLI --type value to transactionType enum string ('1' or '2')."""
    if type_str.lower() in {"expense", "debit", "dr", "d"}:
        return "1"
    if type_str.lower() in {"income", "credit", "cr", "c"}:
        return "2"
    raise ValueError(f"Unknown type: {type_str}")


def _parse_date(date_str: str | None) -> datetime.datetime:
    """Parse a date string or default to now (UTC)."""
    if not date_str:
        return datetime.datetime.now(tz=datetime.timezone.utc)
    try:
        from dateutil import parser as dp
        dt = dp.parse(date_str)
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=datetime.timezone.utc)
        return dt
    except Exception:
        pass
    for fmt in ("%Y-%m-%d", "%d/%m/%Y", "%m/%d/%Y"):
        try:
            dt = datetime.datetime.strptime(date_str, fmt)
            return dt.replace(tzinfo=datetime.timezone.utc)
        except ValueError:
            continue
    raise ValueError(f"Cannot parse date: {date_str}")


def _get_user_base_currency(api_client) -> str:
    user_api = tracko_sdk.UserControllerApi(api_client)
    result = sdk_call(lambda: user_api.me())
    if result and isinstance(result, dict):
        data = result.get("result") if "result" in result else result
        if isinstance(data, dict):
            return data.get("baseCurrency", "INR")
    return "INR"



def _print_raw(data) -> None:
    if data is None:
        print("null")
        return
    if hasattr(data, "to_dict"):
        print(json.dumps(data.to_dict(), indent=2, default=str))
    elif isinstance(data, dict):
        print(json.dumps(data, indent=2, default=str))
    else:
        print(json.dumps(data, indent=2, default=str))


def _fmt_type(v) -> str:
    mapping = {"1": "DEBIT", "2": "CREDIT", "3": "TRANSFER", 1: "DEBIT", 2: "CREDIT", 3: "TRANSFER"}
    return mapping.get(v, str(v))


def _parse_tx_datetime(v) -> datetime.datetime | None:
    if v is None:
        return None
    local_tz = datetime.datetime.now().astimezone().tzinfo
    if isinstance(v, datetime.datetime):
        return v.astimezone(local_tz)
    try:
        if isinstance(v, (int, float)):
            ts = float(v)
            if ts > 10_000_000_000:
                ts /= 1000.0
            return datetime.datetime.fromtimestamp(ts, tz=datetime.timezone.utc).astimezone(local_tz)
    except Exception:
        pass
    s = str(v).strip()
    if s.isdigit():
        try:
            ts = float(int(s))
            if ts > 10_000_000_000:
                ts /= 1000.0
            return datetime.datetime.fromtimestamp(ts, tz=datetime.timezone.utc).astimezone(local_tz)
        except Exception:
            return None
    try:
        from dateutil import parser as dp
        dt = dp.parse(s)
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=datetime.timezone.utc)
        return dt.astimezone(local_tz)
    except Exception:
        pass
    return None


# ---------------------------------------------------------------------------
# Commands
# ---------------------------------------------------------------------------

def _get_transactions_page(base_url: str, token: str | None, month: int, year: int, page: int, size: int) -> dict | None:
    """Fetch transactions page using raw rest client to handle backend's result wrapper."""
    import urllib.parse
    params = urllib.parse.urlencode({"month": month, "year": year, "page": page, "size": size})
    url = base_url.rstrip("/") + "/api/transactions?" + params
    headers = {"Accept": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    with make_api_client(base_url, token) as api_client:
        try:
            resp = api_client.rest_client.request("GET", url, headers=headers)
            resp.read()
            payload = json.loads(resp.data) if resp.data else {}
        except Exception as e:
            print(f"Error: {e}", file=sys.stderr)
            return None
    # Backend wraps in {"result": {...TransactionsPageDTO...}}
    inner = payload.get("result") if isinstance(payload, dict) else payload
    return inner if isinstance(inner, dict) else payload


def cmd_transactions_list(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    now = datetime.datetime.now()
    month = args.month if args.month is not None else now.month
    year = args.year if args.year is not None else now.year
    page = args.page if args.page is not None else 0
    size = args.size if args.size is not None else 500

    result = _get_transactions_page(base_url, token, month, year, page, size)
    if result is None:
        return 1

    if args.raw:
        _print_raw(result)
        return 0

    txs = result.get("transactions") if isinstance(result, dict) else None
    if not isinstance(txs, list):
        _print_raw(result)
        return 0

    print(f"Month={result.get('month', month)} "
          f"Year={result.get('year', year)} "
          f"Page={result.get('page', page)} "
          f"Total={result.get('totalElements', '?')}")

    accounts = get_id_name_map(base_url, token, "/api/accounts")
    categories = get_id_name_map(base_url, token, "/api/categories")

    for tx in txs:
        if not isinstance(tx, dict):
            continue
        aid = tx.get("accountId")
        cid = tx.get("categoryId")
        tx["accountName"] = accounts.get(int(aid), "") if aid is not None else ""
        tx["categoryName"] = categories.get(int(cid), "") if cid is not None else ""
        dt = _parse_tx_datetime(tx.get("date"))
        tx["date"] = dt.date().isoformat() if dt else ""
        tx["time"] = dt.strftime("%H:%M:%S") if dt else ""

    columns = [
        ("id", "ID"), ("date", "Date"), ("time", "Time"), ("name", "Name"),
        ("transactionType", "Type"), ("amount", "Amount"),
        ("accountId", "AcctId"), ("accountName", "Acct"),
        ("categoryId", "CatId"), ("categoryName", "Cat"),
        ("isCountable", "Cnt"), ("comments", "Comments"),
    ]
    print_table(
        txs, columns,
        max_widths={"date": 10, "time": 8, "name": 24, "comments": 28, "accountName": 18, "categoryName": 18},
        right_align={"id", "amount", "accountId", "categoryId"},
        formatters={"transactionType": _fmt_type},
    )
    return 0


def cmd_transactions_get(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.TransactionControllerApi(api_client)
        result = sdk_call(lambda: api.get_by_id(id=int(args.id)))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_transactions_add(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)

    try:
        tx_type = _parse_type(args.type)
    except ValueError as e:
        print(str(e), file=sys.stderr)
        return 2

    with make_api_client(base_url, token) as api_client:
        # Resolve account
        account_id = getattr(args, "account_id", None)
        if account_id is None:
            account_name = getattr(args, "account_name", None)
            if not account_name:
                print("Missing account. Provide --account-id or --account-name", file=sys.stderr)
                return 2
            id_map = get_id_name_map(base_url, token, "/api/accounts")
            matches = [k for k, v in id_map.items() if str(v).strip().casefold() == account_name.strip().casefold()]
            if not matches:
                print(f"No account found with name: {account_name}", file=sys.stderr)
                return 2
            account_id = matches[0]

        # Resolve category
        category_id = getattr(args, "category_id", None)
        if category_id is None:
            category_name = getattr(args, "category_name", None)
            if not category_name:
                print("Missing category. Provide --category-id or --category-name", file=sys.stderr)
                return 2
            id_map = get_id_name_map(base_url, token, "/api/categories")
            matches = [k for k, v in id_map.items() if str(v).strip().casefold() == category_name.strip().casefold()]
            if not matches:
                print(f"No category found with name: {category_name}", file=sys.stderr)
                return 2
            category_id = matches[0]

        currency = getattr(args, "currency", None) or _get_user_base_currency(api_client)

        req = TransactionRequest(
            transactionType=tx_type,
            name=args.name,
            comments=args.comments,
            date=_parse_date(args.date),
            accountId=int(account_id),
            categoryId=int(category_id),
            isCountable=1 if args.countable else 0,
            originalCurrency=str(currency).upper(),
            originalAmount=float(args.amount),
            exchangeRate=float(args.exchange_rate) if getattr(args, "exchange_rate", None) is not None else None,
        )

        api = tracko_sdk.TransactionControllerApi(api_client)
        result = sdk_call(lambda: api.create1(req))

    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_transactions_update(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    tx_id = int(args.id)

    kwargs: dict = {}

    if args.type is not None:
        try:
            kwargs["transactionType"] = _parse_type(args.type)
        except ValueError as e:
            print(str(e), file=sys.stderr)
            return 2

    if args.account_id is not None:
        kwargs["accountId"] = int(args.account_id)
    if args.category_id is not None:
        kwargs["categoryId"] = int(args.category_id)
    if args.name is not None:
        kwargs["name"] = args.name
    if args.comments is not None:
        kwargs["comments"] = args.comments
    if args.date is not None:
        kwargs["date"] = _parse_date(args.date)
    if args.countable is not None:
        kwargs["isCountable"] = 1 if args.countable else 0
    if getattr(args, "currency", None) is not None:
        kwargs["originalCurrency"] = str(args.currency).upper()
    if args.amount is not None:
        kwargs["originalAmount"] = float(args.amount)
    if getattr(args, "exchange_rate", None) is not None:
        kwargs["exchangeRate"] = float(args.exchange_rate)
    if getattr(args, "to_account_id", None) is not None:
        kwargs["toAccountId"] = int(args.to_account_id)
    if getattr(args, "from_account_id", None) is not None:
        kwargs["fromAccountId"] = int(args.from_account_id)

    if not kwargs:
        print("No fields to update provided.", file=sys.stderr)
        return 1

    req = TransactionRequest(**kwargs)

    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.TransactionControllerApi(api_client)
        result = sdk_call(lambda: api.update(id=tx_id, transaction_request=req))

    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_transactions_delete(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.TransactionControllerApi(api_client)
        result = sdk_call(lambda: api.delete1(id=int(args.id)))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_transactions_summary(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    now = datetime.datetime.now()
    start = _parse_date(args.start_date or f"{now.year}-01-01")
    end = _parse_date(args.end_date or f"{now.year}-12-31")

    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.TransactionControllerApi(api_client)
        result = sdk_call(lambda: api.get_my_summary(
            start_date=start,
            end_date=end,
            account_ids=getattr(args, "account_ids", None),
            include_rollover=args.include_rollover,
        ))

    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_transactions_total_income(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.TransactionControllerApi(api_client)
        result = sdk_call(lambda: api.get_my_total_income(
            start_date=_parse_date(args.start_date),
            end_date=_parse_date(args.end_date),
        ))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_transactions_total_expense(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.TransactionControllerApi(api_client)
        result = sdk_call(lambda: api.get_my_total_expense(
            start_date=_parse_date(args.start_date),
            end_date=_parse_date(args.end_date),
        ))
    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_transfers_create(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)

    with make_api_client(base_url, token) as api_client:
        currency = _get_user_base_currency(api_client)
        req = TransactionRequest(
            accountId=int(args.from_account_id),
            toAccountId=int(args.to_account_id),
            originalAmount=float(args.amount),
            originalCurrency=currency,
            transactionType="1",
            name=args.name or "Transfer",
            comments=args.comments,
            isCountable=0,
        )
        api = tracko_sdk.TransactionControllerApi(api_client)
        result = sdk_call(lambda: api.create1(req))

    if result is None:
        return 1
    _print_raw(result)
    return 0


def cmd_transactions_import_csv(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)

    try:
        with open(args.file, "r", encoding="utf-8") as f:
            rows = list(csv.DictReader(f))
    except Exception as e:
        print(f"Failed to read CSV file: {e}", file=sys.stderr)
        return 1

    success_count = 0
    failure_count = 0

    with make_api_client(base_url, token) as api_client:
        currency = _get_user_base_currency(api_client)
        api = tracko_sdk.TransactionControllerApi(api_client)

        for idx, row in enumerate(rows, start=2):
            try:
                date_str = row.get("Date", "").strip()
                tx_type_str = row.get("Transaction Type", "").strip().upper()
                desc = row.get("Description", "").strip()
                amount_str = row.get("Amount", "").strip()

                if not amount_str:
                    print(f"Row {idx}: Skipped (empty amount)")
                    failure_count += 1
                    continue

                try:
                    raw_amount = float(amount_str.replace(",", ""))
                except ValueError:
                    print(f"Row {idx}: Skipped (invalid amount: {amount_str})")
                    failure_count += 1
                    continue

                is_debit = raw_amount < 0 or tx_type_str in {"DEBIT", "DR", "EXPENSE"}
                amount = abs(raw_amount)

                try:
                    dt = datetime.datetime.strptime(date_str, "%d/%m/%Y").replace(tzinfo=datetime.timezone.utc)
                except ValueError:
                    dt = _parse_date(date_str)

                req = TransactionRequest(
                    transactionType="1" if is_debit else "2",
                    name=desc[:100] if desc else "Imported Transaction",
                    comments=f"Imported: {tx_type_str}" if tx_type_str else None,
                    date=dt,
                    accountId=int(args.account_id),
                    isCountable=1,
                    originalAmount=amount,
                    originalCurrency=currency,
                )
                result = sdk_call(lambda: api.create1(req))
                if result is not None:
                    success_count += 1
                    print(f"Row {idx}: Added {desc} ({amount})")
                else:
                    failure_count += 1
                    print(f"Row {idx}: Failed")

            except Exception as e:
                print(f"Row {idx}: Error — {e}")
                failure_count += 1

    print(f"\nImport complete: {success_count} added, {failure_count} failed")
    return 0 if failure_count == 0 else 1
