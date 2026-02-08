import argparse
import datetime
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request


DEFAULT_BASE_URL = "http://localhost:8080"

# Optional: python-dateutil for robust datetime parsing
try:
    from dateutil import parser as date_parser
    from dateutil import tz as date_tz
except Exception:
    date_parser = None
    date_tz = None


def _config_path() -> str:
    home = os.path.expanduser("~")
    return os.path.join(home, ".tracko-cli.json")


def load_config() -> dict:
    path = _config_path()
    if not os.path.exists(path):
        return {}
    try:
        with open(path, "r", encoding="utf-8") as f:
            return json.load(f) or {}
    except Exception:
        return {}


def save_config(cfg: dict) -> None:
    path = _config_path()
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(cfg, f, indent=2, sort_keys=True)
    os.replace(tmp, path)


def _join_url(base_url: str, path: str) -> str:
    base_url = base_url.rstrip("/")
    if not path.startswith("/"):
        path = "/" + path
    return base_url + path


def http_request(method: str, url: str, *, token: str | None = None, json_body: dict | None = None, timeout: int = 30):
    headers = {
        "Accept": "application/json",
    }
    data = None
    if json_body is not None:
        data = json.dumps(json_body).encode("utf-8")
        headers["Content-Type"] = "application/json"

    if token:
        headers["Authorization"] = f"Bearer {token}"

    req = urllib.request.Request(url=url, method=method.upper(), headers=headers, data=data)

    started = time.time()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read()
            elapsed_ms = int((time.time() - started) * 1000)
            content_type = resp.headers.get("Content-Type", "")
            text = raw.decode("utf-8", errors="replace") if raw else ""
            parsed = None
            if "application/json" in content_type and text:
                try:
                    parsed = json.loads(text)
                except Exception:
                    parsed = None
            return {
                "ok": 200 <= resp.status < 300,
                "status": resp.status,
                "headers": dict(resp.headers.items()),
                "text": text,
                "json": parsed,
                "elapsed_ms": elapsed_ms,
            }
    except urllib.error.HTTPError as e:
        raw = e.read() if hasattr(e, "read") else b""
        elapsed_ms = int((time.time() - started) * 1000)
        text = raw.decode("utf-8", errors="replace") if raw else ""
        parsed = None
        content_type = e.headers.get("Content-Type", "") if e.headers else ""
        if "application/json" in content_type and text:
            try:
                parsed = json.loads(text)
            except Exception:
                parsed = None
        return {
            "ok": False,
            "status": e.code,
            "headers": dict(e.headers.items()) if e.headers else {},
            "text": text,
            "json": parsed,
            "elapsed_ms": elapsed_ms,
        }


def _get_id_name_map(base_url: str, token: str | None, path: str) -> dict[int, str]:
    url = _join_url(base_url, path)
    result = http_request("GET", url, token=token)
    payload = result.get("json")
    if not (result.get("ok") and isinstance(payload, dict) and isinstance(payload.get("result"), list)):
        return {}

    out: dict[int, str] = {}
    for item in payload.get("result"):
        if not isinstance(item, dict):
            continue
        try:
            _id = int(item.get("id"))
        except Exception:
            continue
        name = item.get("name")
        if name is None:
            continue
        out[_id] = str(name)
    return out


def print_result(result: dict, *, raw: bool = False) -> None:
    status = result.get("status")
    elapsed_ms = result.get("elapsed_ms")
    print(f"HTTP {status} ({elapsed_ms}ms)")
    if raw:
        txt = result.get("text") or ""
        if txt:
            print(txt)
        return

    payload = result.get("json")
    if payload is not None:
        print(json.dumps(payload, indent=2, sort_keys=True))
        return

    txt = result.get("text")
    if txt:
        print(txt)


def _to_str(v) -> str:
    if v is None:
        return ""
    return str(v)


def _clip(s: str, n: int) -> str:
    if s is None:
        return ""
    s = str(s)
    if len(s) <= n:
        return s
    if n <= 1:
        return s[:n]
    return s[: n - 3] + "..."


def print_table(
    rows: list[dict],
    columns: list[tuple[str, str]],
    *,
    max_widths: dict[str, int] | None = None,
    right_align: set[str] | None = None,
    formatters: dict[str, callable] | None = None,
) -> None:
    # columns: [(key, header)]
    max_widths = max_widths or {}
    right_align = right_align or set()
    formatters = formatters or {}

    rendered_rows: list[list[str]] = []
    for r in rows:
        row_cells: list[str] = []
        for (k, _) in columns:
            v = r.get(k)
            if k in formatters:
                try:
                    v = formatters[k](v)
                except Exception:
                    v = r.get(k)
            cell = _to_str(v)
            mw = int(max_widths.get(k, 32))
            row_cells.append(_clip(cell, mw))
        rendered_rows.append(row_cells)

    headers = [h for (_, h) in columns]
    widths = [len(h) for h in headers]
    for r in rendered_rows:
        for i, cell in enumerate(r):
            widths[i] = max(widths[i], len(cell))

    def fmt_row(cells: list[str]) -> str:
        out: list[str] = []
        for i, cell in enumerate(cells):
            key = columns[i][0]
            if key in right_align:
                out.append(cell.rjust(widths[i]))
            else:
                out.append(cell.ljust(widths[i]))
        return " | ".join(out)

    print(fmt_row(headers))
    print("-+-".join("-" * w for w in widths))
    for r in rendered_rows:
        print(fmt_row(r))


def cmd_health(args: argparse.Namespace) -> int:
    url = _join_url(args.base_url, "/api/health")
    result = http_request("GET", url)
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1


# =========================
# User Currencies (secondary)
# =========================

def _render_currencies_list(result: dict, raw: bool) -> int:
    if raw:
        print_result(result, raw=True)
        return 0 if result.get("ok") else 1
    payload = result.get("json")
    rows = None
    if isinstance(payload, dict):
        rows = payload.get("result")
    if result.get("ok") and isinstance(rows, list):
        print(f"HTTP {result.get('status')} ({result.get('elapsed_ms')}ms)")
        columns = [
            ("currencyCode", "Code"),
            ("exchangeRate", "Rate"),
        ]
        print_table(
            rows,
            columns,
            right_align={"exchangeRate"},
        )
        return 0
    print_result(result, raw=False)
    return 0 if result.get("ok") else 1


def cmd_currencies_list(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, "/api/user-currencies")
    result = http_request("GET", url, token=token)
    return _render_currencies_list(result, raw=args.raw)


def cmd_currencies_add(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, "/api/user-currencies")
    body = {
        "currencyCode": str(args.code).upper(),
        "exchangeRate": float(args.rate),
    }
    result = http_request("POST", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_currencies_update(args: argparse.Namespace) -> int:
    # Backend accepts POST for create/update as per app repository logic
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, "/api/user-currencies")
    body = {
        "currencyCode": str(args.code).upper(),
        "exchangeRate": float(args.rate),
    }
    result = http_request("POST", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_currencies_delete(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    code = str(args.code).upper()
    url = _join_url(base_url, f"/api/user-currencies/{urllib.parse.quote(code)}")
    result = http_request("DELETE", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def _render_splits_result(result: dict, raw: bool) -> int:
    if raw:
        print_result(result, raw=True)
        return 0 if result.get("ok") else 1
    payload = result.get("json")
    rows = None
    if isinstance(payload, dict):
        rows = payload.get("result")
    if result.get("ok") and isinstance(rows, list):
        print(f"HTTP {result.get('status')} ({result.get('elapsed_ms')}ms)")
        columns = [
            ("id", "ID"),
            ("transactionId", "TxnID"),
            ("userId", "UserId"),
            ("contactId", "ContactId"),
            ("amount", "Amount"),
            ("isSettled", "Settled"),
            ("settledAt", "SettledAt"),
        ]
        print_table(
            rows,
            columns,
            max_widths={
                "userId": 36,
            },
            right_align={"id", "transactionId", "amount"},
        )
        return 0
    print_result(result, raw=False)
    return 0 if result.get("ok") else 1


def cmd_splits_list(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, "/api/splits")
    result = http_request("GET", url, token=token)
    return _render_splits_result(result, raw=args.raw)


def cmd_splits_for_transaction(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, f"/api/splits/transaction/{args.transaction_id}")
    result = http_request("GET", url, token=token)
    return _render_splits_result(result, raw=args.raw)


def cmd_splits_for_user(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, f"/api/splits/user/{args.user_id}")
    result = http_request("GET", url, token=token)
    return _render_splits_result(result, raw=args.raw)


def cmd_splits_unsettled(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, f"/api/splits/user/{args.user_id}/unsettled")
    result = http_request("GET", url, token=token)
    return _render_splits_result(result, raw=args.raw)


def cmd_splits_create(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, "/api/splits")
    body = {
        "transactionId": args.transaction_id,
        "userId": args.user_id,
        "amount": float(args.amount),
    }
    if args.contact_id is not None:
        body["contactId"] = int(args.contact_id)
    if args.is_settled is not None:
        try:
            body["isSettled"] = int(args.is_settled)
        except Exception:
            body["isSettled"] = 1 if str(args.is_settled).lower() in {"1", "true", "yes"} else 0
    if args.settled_at:
        body["settledAt"] = args.settled_at
    result = http_request("POST", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_splits_for_contact(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, f"/api/splits/contact/{args.contact_id}")
    result = http_request("GET", url, token=token)
    return _render_splits_result(result, raw=args.raw)


def cmd_splits_unsettled_contact(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, f"/api/splits/contact/{args.contact_id}/unsettled")
    result = http_request("GET", url, token=token)
    return _render_splits_result(result, raw=args.raw)


def cmd_categories_add(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, "/api/categories")
    body = {"name": args.name}
    result = http_request("POST", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1


def cmd_contacts_list(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, "/api/contacts")
    result = http_request("GET", url, token=token)
    if args.raw:
        print_result(result, raw=True)
        return 0 if result.get("ok") else 1
    payload = result.get("json")
    rows = None
    if isinstance(payload, dict):
        rows = payload.get("result")
    if result.get("ok") and isinstance(rows, list):
        print(f"HTTP {result.get('status')} ({result.get('elapsed_ms')}ms)")
        columns = [
            ("id", "ID"),
            ("name", "Name"),
            ("phoneNo", "Phone"),
            ("email", "Email"),
        ]
        print_table(
            rows,
            columns,
            max_widths={
                "name": 28,
                "phoneNo": 18,
                "email": 36,
            },
            right_align={"id"},
        )
        return 0
    print_result(result, raw=False)
    return 0 if result.get("ok") else 1


def cmd_contacts_add(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, "/api/contacts")
    body = {"name": args.name}
    if args.phone:
        body["phoneNo"] = args.phone
    if args.email:
        body["email"] = args.email
    result = http_request("POST", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_contacts_update(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, f"/api/contacts/{args.id}")
    body = {"name": args.name}
    if args.phone is not None:
        body["phoneNo"] = args.phone
    if args.email is not None:
        body["email"] = args.email
    result = http_request("PUT", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_contacts_delete(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, f"/api/contacts/{args.id}")
    result = http_request("DELETE", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1


def cmd_transfers_create(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, "/api/transfers")
    body = {
        "fromAccountId": int(args.from_account_id),
        "toAccountId": int(args.to_account_id),
        "amount": float(args.amount),
    }
    if args.name:
        body["name"] = args.name
    if args.comments:
        body["comments"] = args.comments
    result = http_request("POST", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result.get("ok") else 1

def cmd_login(args: argparse.Namespace) -> int:
    url = _join_url(args.base_url, "/api/login")
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
        print("Saved token to", _config_path())
        return 0

    return 1


def cmd_oauth_token(args: argparse.Namespace) -> int:
    url = _join_url(args.base_url, "/api/oauth/token")
    body = {"phoneNo": args.phone_no, "firebaseUuid": args.firebase_uuid}
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
        print("Saved token to", _config_path())
        return 0

    return 1


def _get_token_from_args_or_config(args: argparse.Namespace) -> tuple[str | None, str]:
    cfg = load_config()
    base_url = getattr(args, "base_url", None) or cfg.get("base_url") or DEFAULT_BASE_URL
    token = getattr(args, "token", None) or cfg.get("token")
    return token, base_url


def cmd_logout(args: argparse.Namespace) -> int:
    cfg = load_config()
    if "token" in cfg:
        cfg.pop("token", None)
        save_config(cfg)
    print("Logged out (token removed)")
    return 0


def cmd_users_list(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, "/api/user")
    result = http_request("GET", url, token=token)
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1


def cmd_accounts_list(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, "/api/accounts")
    result = http_request("GET", url, token=token)
    if args.raw:
        print_result(result, raw=True)
        return 0 if result["ok"] else 1

    payload = result.get("json")
    rows = None
    if isinstance(payload, dict):
        rows = payload.get("result")

    if result.get("ok") and isinstance(rows, list):
        print(f"HTTP {result.get('status')} ({result.get('elapsed_ms')}ms)")
        columns = [
            ("id", "ID"),
            ("name", "Name"),
            ("userId", "UserId"),
        ]
        print_table(
            rows,
            columns,
            max_widths={
                "name": 32,
                "userId": 36,
            },
            right_align={"id"},
        )
        return 0

    print_result(result, raw=False)
    return 0 if result["ok"] else 1


def cmd_accounts_add(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, "/api/accounts")
    body = {"name": args.name}
    result = http_request("POST", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1


def cmd_categories_list(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, "/api/categories")
    result = http_request("GET", url, token=token)
    if args.raw:
        print_result(result, raw=True)
        return 0 if result["ok"] else 1

    payload = result.get("json")
    rows = None
    if isinstance(payload, dict):
        rows = payload.get("result")

    if result.get("ok") and isinstance(rows, list):
        print(f"HTTP {result.get('status')} ({result.get('elapsed_ms')}ms)")
        columns = [
            ("id", "ID"),
            ("name", "Name"),
            ("userId", "UserId"),
        ]
        print_table(
            rows,
            columns,
            max_widths={
                "name": 32,
                "userId": 36,
            },
            right_align={"id"},
        )
        return 0

    print_result(result, raw=False)
    return 0 if result["ok"] else 1


def cmd_transactions_list(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, "/api/transactions")
    result = http_request("GET", url, token=token)
    if args.raw:
        print_result(result, raw=True)
        return 0 if result["ok"] else 1

    payload = result.get("json")
    txs = None
    if isinstance(payload, dict):
        txs = payload.get("result")

    if result.get("ok") and isinstance(txs, list):
        print(f"HTTP {result.get('status')} ({result.get('elapsed_ms')}ms)")
        def fmt_type(v):
            if v == 1 or str(v) == "1":
                return "DEBIT"
            if v == 2 or str(v) == "2":
                return "CREDIT"
            if v == 3 or str(v) == "3":
                return "TRANSFER"
            return v

        def _humanize_timedelta(delta: datetime.timedelta) -> str:
            seconds = int(max(delta.total_seconds(), 0))
            if seconds < 60:
                return f"{seconds}s ago" if seconds > 0 else "just now"
            minutes = seconds // 60
            if minutes < 60:
                return f"{minutes}m ago"
            hours = minutes // 60
            if hours < 24:
                return f"{hours}h ago"
            days = hours // 24
            if days < 30:
                return f"{days}d ago"
            months = days // 30
            if months < 12:
                return f"{months}mo ago"
            years = months // 12
            return f"{years}y ago"

        def fmt_date(v):
            if v is None:
                return ""
            s = str(v)
            if date_parser is not None:
                try:
                    dt = date_parser.parse(s)
                    if dt.tzinfo is None:
                        # assume UTC if no tz
                        dt = dt.replace(tzinfo=datetime.timezone.utc)
                    now = datetime.datetime.now(datetime.timezone.utc)
                    return _humanize_timedelta(now - dt.astimezone(datetime.timezone.utc))
                except Exception:
                    return s
            return s

        accounts = _get_id_name_map(base_url, token, "/api/accounts")
        categories = _get_id_name_map(base_url, token, "/api/categories")
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

        columns = [
            ("id", "ID"),
            ("date", "When"),
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
                "name": 24,
                "comments": 28,
                "accountName": 18,
                "categoryName": 18,
            },
            right_align={"id", "amount", "accountId", "categoryId"},
            formatters={"transactionType": fmt_type, "date": fmt_date},
        )
        return 0

    print_result(result, raw=False)
    return 0 if result["ok"] else 1


def _parse_date_to_epoch_ms(date_str: str | None) -> int:
    if date_str is None or not str(date_str).strip():
        return int(time.time() * 1000)

    s = str(date_str).strip()
    if s.isdigit():
        return int(s)

    # Prefer python-dateutil if available
    if 'date_parser' in globals() and date_parser is not None:
        try:
            dt = date_parser.parse(s)
            if dt.tzinfo is None:
                dt = dt.replace(tzinfo=datetime.timezone.utc)
            return int(dt.timestamp() * 1000)
        except Exception:
            pass

    # Fallbacks without dateutil
    # Handle trailing Z by normalizing to +00:00
    try:
        s2 = s.replace('Z', '+00:00')
        dt = datetime.datetime.fromisoformat(s2)
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=datetime.timezone.utc)
        return int(dt.timestamp() * 1000)
    except Exception:
        pass

    # Try common patterns
    patterns = [
        "%Y-%m-%dT%H:%M:%S%z",
        "%Y-%m-%dT%H:%M:%SZ",
        "%Y-%m-%dT%H:%M:%S",
        "%Y-%m-%d",
    ]
    for p in patterns:
        try:
            dt = datetime.datetime.strptime(s, p)
            if p.endswith('%z') or p.endswith('Z'):
                # set UTC for Z if no tzinfo
                if dt.tzinfo is None:
                    dt = dt.replace(tzinfo=datetime.timezone.utc)
            else:
                dt = dt.replace(tzinfo=datetime.timezone.utc)
            return int(dt.timestamp() * 1000)
        except Exception:
            continue

    # As a last resort, return current time
    return int(time.time() * 1000)


def cmd_transactions_add(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, "/api/transactions")

    tx_type = args.type.lower().strip()
    if tx_type in {"expense", "debit", "dr", "d"}:
        transaction_type = 1
    elif tx_type in {"income", "credit", "cr", "c"}:
        transaction_type = 2
    else:
        print("Invalid --type. Use income or expense.", file=sys.stderr)
        return 2

    # Build request body. By default, --amount is treated as normalized/base amount.
    # If --currency is provided, we treat --amount as the original foreign amount.
    # Per request, we DO NOT send any exchange rate to the backend; the server should
    # fetch the correct live rate and compute normalized amount.
    body = {
        "transactionType": transaction_type,
        "name": args.name,
        "comments": args.comments,
        "date": _parse_date_to_epoch_ms(args.date),
        "accountId": int(args.account_id),
        "categoryId": int(args.category_id),
        "isCountable": 1 if args.countable else 0,
    }

    currency = getattr(args, "currency", None)
    if currency:
        # Backend will fetch the exchange rate from user's configured currencies.
        # We only send originalCurrency and originalAmount.
        try:
            original_amount = float(args.amount)
        except Exception:
            print("Invalid --amount", file=sys.stderr)
            return 2
        body.update({
            "originalCurrency": str(currency).upper(),
            "originalAmount": original_amount,
        })
    else:
        # Backwards compatible: amount is already normalized/base amount
        body["amount"] = float(args.amount)

    result = http_request("POST", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1


def cmd_budget_view(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    
    now = datetime.datetime.now()
    month = int(args.month) if args.month else now.month
    year = int(args.year) if args.year else now.year
    
    params = {
        "month": month,
        "year": year,
        "includeActual": "true",
        "includeRollover": "true"
    }
    query = urllib.parse.urlencode(params)
    url = _join_url(base_url, "/api/budget") + "?" + query
    
    result = http_request("GET", url, token=token)
    
    if args.raw:
        print_result(result, raw=True)
        return 0 if result["ok"] else 1

    payload = result.get("json")
    if not (result.get("ok") and isinstance(payload, dict)):
        print_result(result, raw=False)
        return 1
    
    data = payload.get("result")
    if not isinstance(data, dict):
        # Fallback if structure is unexpected
        print_result(result, raw=False)
        return 1

    # Print Summary
    print(f"Budget for {month}/{year}")
    print(f"Total Income:     {data.get('totalIncome', 0)}")
    print(f"Rollover:         {data.get('rolloverAmount', 0)}")
    print(f"Available Assign: {data.get('availableToAssign', 0)}")
    print(f"Total Budgeted:   {data.get('totalBudget', 0)}")
    print(f"Total Spent:      {data.get('totalSpent', 0)}")
    print("-" * 60)

    # Print Categories
    categories = data.get("categories", [])
    if categories:
        columns = [
            ("categoryId", "ID"),
            ("categoryName", "Category"),
            ("allocatedAmount", "Allocated"),
            ("actualSpent", "Spent"),
            ("remainingBalance", "Remaining"),
        ]
        print_table(
            categories,
            columns,
            max_widths={"categoryName": 30},
            right_align={"categoryId", "allocatedAmount", "actualSpent", "remainingBalance"}
        )

    return 0


def cmd_budget_allocate(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    url = _join_url(base_url, "/api/budget/allocate")
    
    now = datetime.datetime.now()
    month = int(args.month) if args.month else now.month
    year = int(args.year) if args.year else now.year

    body = {
        "month": month,
        "year": year,
        "categoryId": int(args.category_id),
        "amount": float(args.amount)
    }
    
    result = http_request("POST", url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1


def cmd_request(args: argparse.Namespace) -> int:
    token, base_url = _get_token_from_args_or_config(args)
    path = args.path
    if not path.startswith("/"):
        path = "/" + path
    url = _join_url(base_url, path)

    body = None
    if args.data is not None:
        try:
            body = json.loads(args.data)
        except Exception:
            print("Invalid JSON passed to --data", file=sys.stderr)
            return 2

    result = http_request(args.method, url, token=token, json_body=body)
    print_result(result, raw=args.raw)
    return 0 if result["ok"] else 1


def build_parser() -> argparse.ArgumentParser:
    examples = (
        "Examples:\n"
        "  # Health\n"
        "  tracko_cli health\n\n"
        "  # Auth\n"
        "  tracko_cli login --username user@example.com --password password\n"
        "  tracko_cli logout\n\n"
        "  # Users\n"
        "  tracko_cli users list\n\n"
        "  # Accounts\n"
        "  tracko_cli accounts list\n"
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
        "  tracko_cli transactions add --account-id 2 --category-id 2 --amount 250 --type expense --name Lunch --comments 'Team lunch'\n\n"
        "  # Transfers (dual-record: debit source, credit destination)\n"
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
        "  tracko_cli budget allocate --category-id 1 --amount 500 --month 2 --year 2026\n\n"
        "  # Generic request\n"
        "  tracko_cli request --method GET --path /api/health\n"
        "  tracko_cli request --method POST --path /api/contacts --json '{\"name\":\"Bob\"}'\n"
    )
    p = argparse.ArgumentParser(
        prog="tracko_cli",
        description="CLI for Tracko Java Backend",
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
    sp.add_argument("--firebase-uuid", required=True)
    sp.set_defaults(func=cmd_oauth_token)

    sp = sub.add_parser("logout")
    sp.set_defaults(func=cmd_logout)

    sp = sub.add_parser("users")
    sub_users = sp.add_subparsers(dest="users_cmd", required=True)
    sp2 = sub_users.add_parser("list")
    sp2.set_defaults(func=cmd_users_list)

    sp = sub.add_parser("accounts")
    sub_acc = sp.add_subparsers(dest="accounts_cmd", required=True)
    sp2 = sub_acc.add_parser("list")
    sp2.set_defaults(func=cmd_accounts_list)
    sp2 = sub_acc.add_parser("add")
    sp2.add_argument("--name", required=True)
    sp2.set_defaults(func=cmd_accounts_add)

    sp = sub.add_parser("categories")
    sub_cat = sp.add_subparsers(dest="categories_cmd", required=True)
    sp2 = sub_cat.add_parser("list")
    sp2.set_defaults(func=cmd_categories_list)
    sp2 = sub_cat.add_parser("add")
    sp2.add_argument("--name", required=True)
    sp2.set_defaults(func=cmd_categories_add)

    # contacts
    sp = sub.add_parser("contacts")
    sub_ct = sp.add_subparsers(dest="contacts_cmd", required=True)
    sp2 = sub_ct.add_parser("list")
    sp2.set_defaults(func=cmd_contacts_list)

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

    sp = sub.add_parser("transactions")
    sub_tx = sp.add_subparsers(dest="transactions_cmd", required=True)
    sp2 = sub_tx.add_parser("list")
    sp2.set_defaults(func=cmd_transactions_list)

    sp2 = sub_tx.add_parser("add")
    sp2.add_argument("--account-id", required=True, type=int)
    sp2.add_argument("--category-id", required=True, type=int)
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

    sp = sub.add_parser("request", help="Generic request")
    sp.add_argument("--method", default="GET")
    sp.add_argument("path", help="Path like /api/health")
    sp.add_argument("--data", default=None, help='JSON string for request body, e.g. {"foo":1}')
    sp.set_defaults(func=cmd_request)

    return p


def main(argv: list[str]) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    return int(args.func(args))


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
