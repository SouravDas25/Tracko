#!/usr/bin/env python3
"""
Database seeding script for Tracko
Uses the tracko SDK to populate the database with sample data
"""

import random
import json
import sys
import time
import datetime

# SDK must be on path before importing tracko_sdk
from cli.core.api import make_api_client, sdk_call
from cli.core import config as tracko_config

import tracko_sdk
from tracko_sdk.models.login_request import LoginRequest
from tracko_sdk.models.account_save_request import AccountSaveRequest
from tracko_sdk.models.category_save_request import CategorySaveRequest
from tracko_sdk.models.contact_save_request import ContactSaveRequest
from tracko_sdk.models.transaction_request import TransactionRequest
from tracko_sdk.models.budget_allocation_request_dto import BudgetAllocationRequestDTO
from tracko_sdk.models.user_currency_request import UserCurrencyRequest
from tracko_sdk.models.split import Split

DEFAULT_BASE_URL = "http://localhost:8080"


def log(message):
    timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{timestamp}] {message}")


def _result_id(result) -> int | None:
    """Extract 'id' from an SDK response (dict or object)."""
    if result is None:
        return None
    if isinstance(result, dict):
        inner = result.get("result")
        if isinstance(inner, dict):
            return inner.get("id")
        return result.get("id")
    return getattr(result, "id", None)


def _result_list(result) -> list:
    """Extract list of items from an SDK list response."""
    if result is None:
        return []
    if isinstance(result, dict):
        items = result.get("result")
        if isinstance(items, list):
            return items
    return []


def wait_for_api(base_url: str, max_attempts: int = 30) -> bool:
    log(f"Waiting for API at {base_url}...")
    for attempt in range(max_attempts):
        try:
            with make_api_client(base_url) as api_client:
                api = tracko_sdk.HealthControllerApi(api_client)
                result = api.health()
            if result is not None:
                log("API is ready!")
                return True
        except Exception:
            pass
        log(f"Attempt {attempt + 1}/{max_attempts} - API not ready, waiting 2 seconds...")
        time.sleep(2)
    log("ERROR: API not available after maximum attempts")
    return False


def login_existing_user(base_url: str) -> str | None:
    log("Logging in with existing user credentials...")
    with make_api_client(base_url) as api_client:
        api = tracko_sdk.SessionControllerApi(api_client)
        result = sdk_call(lambda: api.login(LoginRequest(username="user@example.com", password="password")))
    if result is None:
        log("Login failed")
        return None
    token = result.get("token") if isinstance(result, dict) else getattr(result, "token", None)
    if token:
        log("Login successful!")
        return token
    log("Login failed: No token in response")
    return None


def get_existing_resources_map(base_url: str, token: str, endpoint: str, key_field: str = "name") -> dict:
    with make_api_client(base_url, token) as api_client:
        headers = {"Accept": "application/json", "Authorization": f"Bearer {token}"}
        try:
            resp = api_client.rest_client.request("GET", base_url.rstrip("/") + endpoint, headers=headers)
            resp.read()
            payload = json.loads(resp.data) if resp.data else {}
        except Exception:
            return {}
    items = payload.get("result", []) if isinstance(payload, dict) else []
    return {item[key_field]: item["id"] for item in items if key_field in item and "id" in item}


def create_accounts(base_url: str, token: str) -> list:
    log("Creating sample accounts...")
    names = ["HDFC Savings", "ICICI Credit Card", "Cash Wallet", "Paytm Wallet", "Investment Account"]
    existing = get_existing_resources_map(base_url, token, "/api/accounts")
    existing_norm = {str(k).strip().casefold(): v for k, v in existing.items()}
    account_ids = []
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.AccountControllerApi(api_client)
        for name in names:
            norm = name.strip().casefold()
            if norm in existing_norm:
                acc_id = existing_norm[norm]
                account_ids.append(acc_id)
                log(f"Account already exists: {name} (ID: {acc_id})")
                continue
            result = sdk_call(lambda n=name: api.create7(AccountSaveRequest(name=n)))
            acc_id = _result_id(result)
            account_ids.append(acc_id)
            log(f"Created account: {name} (ID: {acc_id})")
    return account_ids


def create_categories(base_url: str, token: str) -> list:
    log("Creating sample categories...")
    names = [
        "Food & Dining", "Transportation", "Shopping", "Entertainment",
        "Bills & Utilities", "Healthcare", "Education", "Travel",
        "Investments", "Salary", "Freelance", "Other Income",
    ]
    existing = get_existing_resources_map(base_url, token, "/api/categories")
    existing_norm = {str(k).strip().casefold(): v for k, v in existing.items()}
    category_ids = []
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.CategoryControllerApi(api_client)
        for name in names:
            norm = name.strip().casefold()
            if norm in existing_norm:
                cat_id = existing_norm[norm]
                category_ids.append(cat_id)
                log(f"Category already exists: {name} (ID: {cat_id})")
                continue
            result = sdk_call(lambda n=name: api.create6(CategorySaveRequest(name=n)))
            cat_id = _result_id(result)
            category_ids.append(cat_id)
            log(f"Created category: {name} (ID: {cat_id})")
    return category_ids


def create_contacts(base_url: str, token: str) -> list:
    log("Creating sample contacts...")
    contacts = [
        {"name": "Alice Johnson", "phone": "9876543210", "email": "alice@example.com"},
        {"name": "Bob Smith", "phone": "9876543211", "email": "bob@example.com"},
        {"name": "Charlie Brown", "phone": "9876543212", "email": "charlie@example.com"},
        {"name": "Diana Prince", "phone": "9876543213", "email": "diana@example.com"},
        {"name": "Eve Wilson", "phone": "9876543214", "email": "eve@example.com"},
    ]
    existing = get_existing_resources_map(base_url, token, "/api/contacts")
    contact_ids = []
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.ContactControllerApi(api_client)
        for c in contacts:
            if c["name"] in existing:
                cid = existing[c["name"]]
                contact_ids.append(cid)
                log(f"Contact already exists: {c['name']} (ID: {cid})")
                continue
            req = ContactSaveRequest(name=c["name"], phone_no=c["phone"], email=c["email"])
            result = sdk_call(lambda r=req: api.create5(r))
            cid = _result_id(result)
            contact_ids.append(cid)
            log(f"Created contact: {c['name']} (ID: {cid})")
    return contact_ids


def create_transactions(base_url: str, token: str, account_ids: list, category_ids: list, base_currency: str = "INR") -> int:
    log("Creating sample transactions...")
    valid_account_ids = [a for a in account_ids if a is not None]
    valid_category_ids = [c for c in category_ids if c is not None]
    if not valid_account_ids or not valid_category_ids:
        log("No valid account/category IDs, skipping transactions")
        return 0

    now = datetime.datetime.now()
    expense_data = [
        ("Grocery Shopping", (50, 200)), ("Uber Ride", (10, 50)),
        ("Amazon Purchase", (20, 150)), ("Movie Tickets", (15, 40)),
        ("Electricity Bill", (80, 200)), ("Doctor Visit", (50, 300)),
        ("Online Course", (30, 100)), ("Flight Booking", (200, 800)),
    ]
    income_data = [
        ("Monthly Salary", (3000, 5000)),
        ("Freelance Project", (500, 2000)),
        ("Investment Returns", (100, 500)),
    ]

    transactions = []
    for months_back in range(1, 4):
        target_month = now.month - months_back
        target_year = now.year
        if target_month <= 0:
            target_month += 12
            target_year -= 1
        if target_month == 12:
            last_day = (datetime.datetime(target_year + 1, 1, 1) - datetime.timedelta(days=1)).day
        else:
            last_day = (datetime.datetime(target_year, target_month + 1, 1) - datetime.timedelta(days=1)).day
        for day in range(1, last_day + 1):
            if random.random() < 0.7:
                dt = datetime.datetime(target_year, target_month, day,
                                       random.randint(9, 21), random.randint(0, 59),
                                       tzinfo=datetime.timezone.utc)
                for _ in range(random.randint(1, 3)):
                    if random.random() < 0.7:
                        name, rng = random.choice(expense_data)
                        transactions.append(("1", name, round(random.uniform(*rng), 2),
                                             dt, random.choice(valid_account_ids), random.choice(valid_category_ids[:8])))
                    elif random.random() < 0.3:
                        name, rng = random.choice(income_data)
                        transactions.append(("2", name, round(random.uniform(*rng), 2),
                                             dt, valid_account_ids[0], random.choice(valid_category_ids[8:])))

    max_day = max(1, min(now.day, 28))
    for _ in range(10):
        day = random.randint(1, max_day)
        dt = datetime.datetime(now.year, now.month, day,
                               random.randint(9, 21), random.randint(0, 59),
                               tzinfo=datetime.timezone.utc)
        if random.random() < 0.8:
            name, rng = random.choice(expense_data)
            transactions.append(("1", name, round(random.uniform(*rng), 2),
                                 dt, random.choice(valid_account_ids), random.choice(valid_category_ids[:8])))
        else:
            name, rng = random.choice(income_data)
            transactions.append(("2", name, round(random.uniform(*rng), 2),
                                 dt, valid_account_ids[0], random.choice(valid_category_ids[8:])))

    log(f"Generated {len(transactions)} transactions, sending...")
    created_count = 0
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.TransactionControllerApi(api_client)
        for tx_type, name, amount, dt, acct_id, cat_id in transactions:
            req = TransactionRequest(
                transactionType=tx_type,
                name=name,
                date=dt,
                accountId=int(acct_id),
                categoryId=int(cat_id),
                isCountable=1,
                originalAmount=amount,
                originalCurrency=base_currency,
            )
            result = sdk_call(lambda r=req: api.create1(r))
            if result is not None:
                created_count += 1
                if created_count % 10 == 0:
                    log(f"Created {created_count} transactions...")
            else:
                log(f"Failed to create transaction: {name}")
    log(f"Created {created_count} transactions total")
    return created_count


def create_transfers(base_url: str, token: str, account_ids: list, base_currency: str = "INR") -> int:
    log("Creating sample transfer transactions...")
    valid = [a for a in account_ids if a is not None]
    if len(valid) < 2:
        log("Need at least 2 accounts for transfers, skipping...")
        return 0

    now = datetime.datetime.now(tz=datetime.timezone.utc)
    transfers = [
        ("Weekly Cash Withdrawal", valid[0], valid[2] if len(valid) > 2 else valid[1], 500.0, 7),
        ("Monthly Investment", valid[0], valid[4] if len(valid) > 4 else valid[-1], 1000.0, 3),
        ("Credit Card Payment", valid[1], valid[0], 2000.0, 10),
        ("Mobile Recharge", valid[0], valid[3] if len(valid) > 3 else valid[-1], 300.0, 2),
        ("Cash Deposit", valid[2] if len(valid) > 2 else valid[0], valid[0], 150.0, 1),
    ]

    created_count = 0
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.TransactionControllerApi(api_client)
        for name, from_id, to_id, amount, days_ago in transfers:
            req = TransactionRequest(
                transactionType="1",
                name=name,
                accountId=int(from_id),
                toAccountId=int(to_id),
                originalAmount=amount,
                originalCurrency=base_currency,
                isCountable=0,
                date=now - datetime.timedelta(days=days_ago),
            )
            result = sdk_call(lambda r=req: api.create1(r))
            if result is not None:
                created_count += 1
                log(f"Created transfer: {name} ({amount} from {from_id} to {to_id})")
            else:
                log(f"Failed to create transfer: {name}")
    log(f"Created {created_count} transfers total")
    return created_count


def create_budget_allocations(base_url: str, token: str, category_ids: list) -> int:
    log("Creating sample budget allocations...")
    valid = [c for c in category_ids if c is not None]
    if not valid:
        log("No valid category IDs, skipping budget allocations")
        return 0

    now = datetime.datetime.now()
    allocations = [
        (valid[0], 500.0, "Food & Dining"),
        (valid[1] if len(valid) > 1 else valid[0], 200.0, "Transportation"),
        (valid[2] if len(valid) > 2 else valid[0], 300.0, "Shopping"),
        (valid[3] if len(valid) > 3 else valid[0], 150.0, "Entertainment"),
        (valid[4] if len(valid) > 4 else valid[0], 250.0, "Bills & Utilities"),
        (valid[5] if len(valid) > 5 else valid[0], 100.0, "Healthcare"),
        (valid[6] if len(valid) > 6 else valid[0], 50.0, "Education"),
    ]

    created_count = 0
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.BudgetControllerApi(api_client)
        for cat_id, amount, name in allocations:
            req = BudgetAllocationRequestDTO(
                category_id=int(cat_id),
                amount=amount,
                month=now.month,
                year=now.year,
            )
            result = sdk_call(lambda r=req: api.allocate_funds(r))
            if result is not None:
                created_count += 1
                log(f"Allocated {amount} to {name}")
            else:
                log(f"Failed to allocate budget for {name}")
    log(f"Created {created_count} budget allocations")
    return created_count


def create_currencies(base_url: str, token: str) -> int:
    log("Creating sample currency configurations...")
    currencies = [
        ("EUR", 0.85), ("GBP", 0.73), ("JPY", 110.0),
        ("INR", 74.0), ("CAD", 1.25), ("AUD", 1.35),
    ]
    created_count = 0
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.UserCurrencyControllerApi(api_client)
        for code, rate in currencies:
            req = UserCurrencyRequest(currency_code=code, exchange_rate=rate)
            result = sdk_call(lambda r=req: api.save(r))
            if result is not None:
                created_count += 1
                log(f"Added currency: {code} (Rate: {rate})")
            else:
                log(f"Failed to add currency: {code}")
    log(f"Created {created_count} currency configurations")
    return created_count


def get_current_user_details(base_url: str, token: str) -> dict | None:
    with make_api_client(base_url, token) as api_client:
        api = tracko_sdk.UserControllerApi(api_client)
        result = sdk_call(lambda: api.me())
    if result is None:
        return None
    if isinstance(result, dict):
        return result.get("result") or result
    return None


def create_sample_splits(base_url: str, token: str, account_ids: list, category_ids: list, base_currency: str = "INR") -> int:
    log("Creating sample split transactions...")
    valid_accounts = [a for a in account_ids if a is not None]
    valid_cats = [c for c in category_ids if c is not None]
    if not valid_accounts or not valid_cats:
        log("No valid IDs, skipping splits")
        return 0

    now = datetime.datetime.now(tz=datetime.timezone.utc)
    with make_api_client(base_url, token) as api_client:
        tx_api = tracko_sdk.TransactionControllerApi(api_client)
        req = TransactionRequest(
            transactionType="1",
            name="Group Dinner",
            comments="Dinner with friends - to be split",
            date=now - datetime.timedelta(days=5),
            accountId=int(valid_accounts[0]),
            categoryId=int(valid_cats[0]),
            isCountable=1,
            originalAmount=300.0,
            originalCurrency=base_currency,
        )
        tx_result = sdk_call(lambda: tx_api.create1(req))
        transaction_id = _result_id(tx_result)
        if not transaction_id:
            log("No transaction ID returned, skipping splits")
            return 0

        user_api = tracko_sdk.UserControllerApi(api_client)
        me_result = sdk_call(lambda: user_api.me())
        user_id = None
        if me_result:
            user_data = me_result.get("result") if isinstance(me_result, dict) else None
            if isinstance(user_data, dict):
                user_id = user_data.get("id")
        if not user_id:
            log("Failed to get current user ID, skipping splits")
            return 0

        split_api = tracko_sdk.SplitControllerApi(api_client)
        split_count = 0
        for i, amount in enumerate([100.0, 75.0, 125.0]):
            split_req = Split(
                transaction_id=int(transaction_id),
                user_id=str(user_id),
                amount=amount,
                is_settled=1 if i % 2 == 0 else 0,
            )
            result = sdk_call(lambda r=split_req: split_api.create2(r))
            if result is not None:
                split_count += 1
                log(f"Created split: {amount} for user {user_id}")
            else:
                log(f"Failed to create split: {amount}")

    log(f"Created {split_count} splits")
    return split_count


def main():
    try:
        profile_cfg = tracko_config.get_active_profile_config()
        active_profile = tracko_config.get_active_profile_name()
        base_url = profile_cfg.get("base_url") or DEFAULT_BASE_URL
    except Exception:
        active_profile = "default"
        base_url = DEFAULT_BASE_URL

    log("Starting Tracko database seeding...")
    log(f"Active profile: {active_profile}")
    log(f"Target API: {base_url}")

    if not wait_for_api(base_url):
        sys.exit(1)

    token = login_existing_user(base_url)
    if not token:
        log("Failed to login, aborting...")
        sys.exit(1)

    log("Successfully authenticated!")

    user_details = get_current_user_details(base_url, token)
    base_currency = user_details.get("baseCurrency", "INR") if user_details else "INR"
    log(f"User base currency: {base_currency}")

    account_ids = create_accounts(base_url, token)
    category_ids = create_categories(base_url, token)
    contact_ids = create_contacts(base_url, token)

    transaction_count = create_transactions(base_url, token, account_ids, category_ids, base_currency) if account_ids and category_ids else 0
    budget_count = create_budget_allocations(base_url, token, category_ids) if category_ids else 0
    currency_count = create_currencies(base_url, token)
    transfer_count = create_transfers(base_url, token, account_ids, base_currency) if account_ids else 0
    split_count = create_sample_splits(base_url, token, account_ids, category_ids, base_currency) if account_ids and category_ids else 0

    log("\n" + "=" * 60)
    log("DATABASE SEEDING COMPLETE!")
    log("=" * 60)
    log(f"Accounts created: {len(account_ids)}")
    log(f"Categories created: {len(category_ids)}")
    log(f"Contacts created: {len(contact_ids)}")
    log(f"Transactions created: {transaction_count}")
    log(f"Budget allocations created: {budget_count}")
    log(f"Currency configurations created: {currency_count}")
    log(f"Transfers created: {transfer_count}")
    log(f"Splits created: {split_count}")
    log("=" * 60)


if __name__ == "__main__":
    main()
