import argparse
import sys
import urllib.parse
from tracko_cli.core.config import get_token_from_args_or_config
from tracko_cli.core.http import http_request, join_url
from tracko_cli.utils.dates import parse_date_to_epoch_ms
from tracko_cli.utils.formatting import print_result, print_table
from tracko_cli.utils.lookups import get_id_name_map


def cmd_transactions_get(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, f"/api/transactions/{int(args.id)}")
    result = http_request("GET", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_transactions_update(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    tx_id = int(args.id)
    url = join_url(base_url, f"/api/transactions/{tx_id}")

    body: dict = {}

    if args.type is not None:
        tx_type = args.type.lower().strip()
        if tx_type in {"expense", "debit", "dr", "d"}:
            body["transactionType"] = 1
        elif tx_type in {"income", "credit", "cr", "c"}:
            body["transactionType"] = 2
        else:
            print("Invalid --type. Use income or expense.", file=sys.stderr)
            return 2

    if args.account_id is not None:
        body["accountId"] = int(args.account_id)

    if args.category_id is not None:
        body["categoryId"] = int(args.category_id)

    if args.name is not None:
        body["name"] = args.name

    if args.comments is not None:
        body["comments"] = args.comments

    if args.date is not None:
        body["date"] = parse_date_to_epoch_ms(args.date)

    if args.countable is not None:
        body["isCountable"] = 1 if args.countable else 0

    currency = getattr(args, "currency", None)
    if currency is not None:
        body["originalCurrency"] = str(currency).upper()
        if args.amount is not None:
            try:
                body["originalAmount"] = float(args.amount)
            except Exception:
                print("Invalid --amount", file=sys.stderr)
                return 2
        exchange_rate = getattr(args, "exchange_rate", None)
        if exchange_rate is not None:
            body["exchangeRate"] = float(exchange_rate)
    elif args.amount is not None:
        try:
            body["amount"] = float(args.amount)
        except Exception:
            print("Invalid --amount", file=sys.stderr)
            return 2

    if not body:
        print("No fields to update provided.", file=sys.stderr)
        return 1

    result = http_request("PUT", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_transactions_delete(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, f"/api/transactions/{int(args.id)}")
    result = http_request("DELETE", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_transactions_list(args: argparse.Namespace) -> int:
    import datetime
    token, base_url = get_token_from_args_or_config(args)
    now = datetime.datetime.now()
    month = args.month if args.month is not None else now.month
    year = args.year if args.year is not None else now.year
    page = args.page if args.page is not None else 0
    size = args.size if args.size is not None else 500

    query = urllib.parse.urlencode({
        "month": month,
        "year": year,
        "page": page,
        "size": size,
    })
    url = join_url(base_url, "/api/transactions") + "?" + query
    result = http_request("GET", url, token=token)
    if args.raw:
        print_result(result, raw=True)
        return 0 if result["ok"] else 1

    payload = result.get("json")
    txs = None
    meta = None
    if isinstance(payload, dict):
        result_payload = payload.get("result")
        if isinstance(result_payload, dict):
            txs = result_payload.get("transactions")
            meta = result_payload

    if result.get("ok") and isinstance(txs, list):
        print(f"HTTP {result.get('status')} ({result.get('elapsed_ms')}ms)")
        if isinstance(meta, dict):
            print(
                f"Month={meta.get('month')} Year={meta.get('year')} "
                f"Page={meta.get('page')} Size={meta.get('size')} "
                f"Total={meta.get('totalElements')}"
            )
        def fmt_type(v):
            if v == 1 or str(v) == "1":
                return "DEBIT"
            if v == 2 or str(v) == "2":
                return "CREDIT"
            if v == 3 or str(v) == "3":
                return "TRANSFER"
            return v

        def parse_tx_datetime(v) -> datetime.datetime | None:
            if v is None:
                return None
            local_tz = datetime.datetime.now().astimezone().tzinfo
            try:
                if isinstance(v, (int, float)):
                    ts = float(v)
                    if ts > 10_000_000_000:
                        ts = ts / 1000.0
                    dt = datetime.datetime.fromtimestamp(ts, tz=datetime.timezone.utc)
                    return dt.astimezone(local_tz)
            except Exception:
                pass

            s = str(v).strip()
            if not s:
                return None

            if s.isdigit():
                try:
                    ts = float(int(s))
                    if ts > 10_000_000_000:
                        ts = ts / 1000.0
                    dt = datetime.datetime.fromtimestamp(ts, tz=datetime.timezone.utc)
                    return dt.astimezone(local_tz)
                except Exception:
                    return None

            try:
                from dateutil import parser as date_parser
                dt = date_parser.parse(s)
                if dt.tzinfo is None:
                    dt = dt.replace(tzinfo=datetime.timezone.utc)
                return dt.astimezone(local_tz)
            except Exception:
                pass

            try:
                s2 = s.replace("Z", "+00:00")
                dt = datetime.datetime.fromisoformat(s2)
                if dt.tzinfo is None:
                    dt = dt.replace(tzinfo=datetime.timezone.utc)
                return dt.astimezone(local_tz)
            except Exception:
                return None

        accounts = get_id_name_map(base_url, token, "/api/accounts")
        categories = get_id_name_map(base_url, token, "/api/categories")
        for tx in txs:
            if not isinstance(tx, dict):
                continue
            try:
                aid = int(tx.get("accountId")) if tx.get("accountId") is not None else None
            except Exception:
                aid = None
            try:
                cid = int(tx.get("categoryId")) if tx.get("categoryId") is not None else None
            except Exception:
                cid = None
            tx["accountName"] = accounts.get(aid, "") if aid is not None else ""
            tx["categoryName"] = categories.get(cid, "") if cid is not None else ""

            dt = parse_tx_datetime(tx.get("date"))
            tx["date"] = dt.date().isoformat() if dt is not None else ""
            tx["time"] = dt.strftime("%H:%M:%S") if dt is not None else ""

        columns = [
            ("id", "ID"),
            ("date", "Date"),
            ("time", "Time"),
            ("name", "Name"),
            ("transactionType", "Type"),
            ("amount", "Amount"),
            ("accountId", "AcctId"),
            ("accountName", "Acct"),
            ("categoryId", "CatId"),
            ("categoryName", "Cat"),
            ("isCountable", "Cnt"),
            ("comments", "Comments"),
        ]
        print_table(
            txs,
            columns,
            max_widths={
                "date": 10,
                "time": 8,
                "name": 24,
                "comments": 28,
                "accountName": 18,
                "categoryName": 18,
            },
            right_align={"id", "amount", "accountId", "categoryId"},
            formatters={"transactionType": fmt_type},
        )
        return 0

    print_result(result, raw=False)
    return 0 if result["ok"] else 1


def cmd_transactions_add(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, "/api/transactions")

    def _resolve_id_by_name(path: str, name: str, label: str) -> int | None:
        if not name or not str(name).strip():
            return None
        resp = http_request("GET", join_url(base_url, path), token=token)
        payload = resp.get("json")
        rows = payload.get("result") if isinstance(payload, dict) else None
        if not (resp.get("ok") and isinstance(rows, list)):
            print(f"Failed to load {label} list to resolve name.", file=sys.stderr)
            return None

        wanted = str(name).strip().casefold()
        matches: list[dict] = []
        for item in rows:
            if not isinstance(item, dict):
                continue
            n = item.get("name")
            if n is None:
                continue
            if str(n).strip().casefold() == wanted:
                matches.append(item)

        if not matches:
            print(f"No {label} found with name: {name}", file=sys.stderr)
            return None
        if len(matches) > 1:
            ids = []
            for m in matches:
                try:
                    ids.append(str(int(m.get("id"))))
                except Exception:
                    continue
            suffix = f" (matching IDs: {', '.join(ids)})" if ids else ""
            print(f"Ambiguous {label} name: {name}{suffix}", file=sys.stderr)
            return None

        try:
            return int(matches[0].get("id"))
        except Exception:
            print(f"Failed to resolve {label} id for name: {name}", file=sys.stderr)
            return None

    tx_type = args.type.lower().strip()
    if tx_type in {"expense", "debit", "dr", "d"}:
        transaction_type = 1
    elif tx_type in {"income", "credit", "cr", "c"}:
        transaction_type = 2
    else:
        print("Invalid --type. Use income or expense.", file=sys.stderr)
        return 2

    account_id = getattr(args, "account_id", None)
    if account_id is None:
        account_name = getattr(args, "account_name", None)
        if not account_name:
            print("Missing account. Provide --account-id or --account-name", file=sys.stderr)
            return 2
        account_id = _resolve_id_by_name("/api/accounts", account_name, "account")
        if account_id is None:
            return 2

    category_id = getattr(args, "category_id", None)
    if category_id is None:
        category_name = getattr(args, "category_name", None)
        if not category_name:
            print("Missing category. Provide --category-id or --category-name", file=sys.stderr)
            return 2
        category_id = _resolve_id_by_name("/api/categories", category_name, "category")
        if category_id is None:
            return 2

    body = {
        "transactionType": transaction_type,
        "name": args.name,
        "comments": args.comments,
        "date": parse_date_to_epoch_ms(args.date),
        "accountId": int(account_id),
        "categoryId": int(category_id),
        "isCountable": 1 if args.countable else 0,
    }

    currency = getattr(args, "currency", None)
    if currency:
        try:
            original_amount = float(args.amount)
        except Exception:
            print("Invalid --amount", file=sys.stderr)
            return 2
        body["originalAmount"] = original_amount
        body["originalCurrency"] = str(currency).upper()
        exchange_rate = getattr(args, "exchange_rate", None)
        if exchange_rate is not None:
            body["exchangeRate"] = float(exchange_rate)
    else:
        try:
            body["amount"] = float(args.amount)
        except Exception:
            print("Invalid --amount", file=sys.stderr)
            return 2

    result = http_request("POST", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1


def cmd_transactions_import_csv(args: argparse.Namespace) -> int:
    import csv
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, "/api/transactions")

    try:
        with open(args.file, "r", encoding="utf-8") as f:
            reader = csv.DictReader(f)
            rows = list(reader)
    except Exception as e:
        print(f"Failed to read CSV file: {e}", file=sys.stderr)
        return 1

    success_count = 0
    failure_count = 0

    for idx, row in enumerate(rows, start=2):
        try:
            date_str = row.get("Date", "").strip()
            tx_type = row.get("Transaction Type", "").strip().upper()
            desc = row.get("Description", "").strip()
            amount_str = row.get("Amount", "").strip()

            if not amount_str:
                print(f"Row {idx}: Skipped (Empty amount)")
                failure_count += 1
                continue

            try:
                raw_amount = float(amount_str.replace(",", ""))
            except ValueError:
                print(f"Row {idx}: Skipped (Invalid amount format: {amount_str})")
                failure_count += 1
                continue

            is_debit = False
            if raw_amount < 0:
                is_debit = True
                amount = abs(raw_amount)
            elif tx_type in {"DEBIT", "DR", "EXPENSE"}:
                is_debit = True
                amount = raw_amount
            else:
                amount = raw_amount

            transaction_type = 1 if is_debit else 2
            
            import datetime
            try:
                dt = datetime.datetime.strptime(date_str, "%d/%m/%Y")
                epoch_ms = int(dt.timestamp() * 1000)
            except ValueError:
                epoch_ms = parse_date_to_epoch_ms(date_str)

            body = {
                "transactionType": transaction_type,
                "name": desc[:100] if desc else "Imported Transaction",
                "comments": f"Imported: {tx_type}" if tx_type else None,
                "date": epoch_ms,
                "accountId": int(args.account_id),
                "isCountable": 1,
                "amount": amount,
            }

            result = http_request("POST", url, token=token, json_body=body)
            if result.get("ok"):
                success_count += 1
                print(f"Row {idx}: Added {desc} ({amount})")
            else:
                failure_count += 1
                print(f"Row {idx}: Failed - HTTP {result.get('status')} {result.get('text')}")
                
        except Exception as e:
            print(f"Row {idx}: Error processing row - {e}")
            failure_count += 1

    print(f"\nImport Complete: {success_count} added, {failure_count} failed")
    return 0 if failure_count == 0 else 1


def cmd_transfers_create(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    url = join_url(base_url, "/api/transactions")
    
    body = {
        "accountId": int(args.from_account_id),
        "toAccountId": int(args.to_account_id),
        "amount": float(args.amount),
        "transactionType": 1,
        "name": args.name or "Transfer",
        "comments": args.comments,
        "isCountable": 0,
    }
    
    result = http_request("POST", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1


def cmd_transactions_summary(args: argparse.Namespace) -> int:
    import datetime
    token, base_url = get_token_from_args_or_config(args)
    
    start_date = args.start_date or f"{datetime.datetime.now().year}-01-01"
    end_date = args.end_date or f"{datetime.datetime.now().year}-12-31"
    
    params = {
        "startDate": start_date,
        "endDate": end_date,
        "includeRollover": "true" if args.include_rollover else "false"
    }
    if args.account_ids:
        params["accountIds"] = args.account_ids
    
    query = urllib.parse.urlencode(params)
    url = join_url(base_url, "/api/transactions/summary") + "?" + query
    
    result = http_request("GET", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1


def cmd_transactions_total_income(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    query = urllib.parse.urlencode({"startDate": args.start_date, "endDate": args.end_date})
    url = join_url(base_url, "/api/transactions/total-income") + "?" + query
    result = http_request("GET", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_transactions_total_expense(args: argparse.Namespace) -> int:
    token, base_url = get_token_from_args_or_config(args)
    query = urllib.parse.urlencode({"startDate": args.start_date, "endDate": args.end_date})
    url = join_url(base_url, "/api/transactions/total-expense") + "?" + query
    result = http_request("GET", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1
