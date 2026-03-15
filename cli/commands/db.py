"""Database seeding commands."""
import sys
import typer
from rich.progress import Progress, SpinnerColumn, TextColumn, BarColumn, TaskProgressColumn
import random
import datetime

from ..core.api import get_config_for_api, get_api_client, unwrap_envelope, handle_api_error
from ..core.output import console, print_success, print_error, print_info
from ..utils.prompts import confirm

import tracko_sdk
from tracko_sdk.models.account_save_request import AccountSaveRequest
from tracko_sdk.models.category_save_request import CategorySaveRequest
from tracko_sdk.models.contact_save_request import ContactSaveRequest
from tracko_sdk.models.transaction_request import TransactionRequest
from tracko_sdk.models.budget_allocation_request_dto import BudgetAllocationRequestDTO
from tracko_sdk.models.user_currency_request import UserCurrencyRequest
from tracko_sdk.rest import ApiException
from urllib3.exceptions import MaxRetryError, NewConnectionError


app = typer.Typer(help="Database operations")


SAMPLE_ACCOUNTS = [
    ("HDFC Savings", "INR"),
    ("ICICI Credit Card", "INR"),
    ("Cash Wallet", "INR"),
    ("Paytm Wallet", "INR"),
    ("Investment Account", "INR"),
]

SAMPLE_CATEGORIES = [
    ("Food & Dining", "EXPENSE"),
    ("Transportation", "EXPENSE"),
    ("Shopping", "EXPENSE"),
    ("Entertainment", "EXPENSE"),
    ("Bills & Utilities", "EXPENSE"),
    ("Healthcare", "EXPENSE"),
    ("Education", "EXPENSE"),
    ("Travel", "EXPENSE"),
    ("Investments", "EXPENSE"),
    ("Salary", "INCOME"),
    ("Freelance", "INCOME"),
    ("Other Income", "INCOME"),
]

SAMPLE_CONTACTS = [
    ("Alice", "9876543210", "alice@example.com"),
    ("Bob", "9876543211", "bob@example.com"),
    ("Charlie", "9876543212", "charlie@example.com"),
    ("Diana", "9876543213", "diana@example.com"),
    ("Eve", "9876543214", "eve@example.com"),
]

SAMPLE_CURRENCIES = [
    ("EUR", 0.85),
    ("GBP", 0.73),
    ("JPY", 110.0),
    ("CAD", 1.25),
    ("AUD", 1.35),
]


@app.command()
def seed(
    dry_run: bool = typer.Option(False, "--dry-run", help="Preview without creating"),
    skip_transactions: bool = typer.Option(False, "--skip-transactions", help="Skip transaction creation"),
):
    """Seed database with sample data."""
    if not dry_run and not confirm("This will create sample data in the database. Continue?", default=False):
        print_error("Cancelled")
        raise typer.Exit(1)

    base_url, token = get_config_for_api()

    if dry_run:
        print_info("DRY RUN MODE - No data will be created")
        print_info(f"Would create:")
        print_info(f"  - {len(SAMPLE_ACCOUNTS)} accounts")
        print_info(f"  - {len(SAMPLE_CATEGORIES)} categories")
        print_info(f"  - {len(SAMPLE_CONTACTS)} contacts")
        print_info(f"  - {len(SAMPLE_CURRENCIES)} currencies")
        if not skip_transactions:
            print_info(f"  - ~90 transactions")
        return

    try:
        with Progress(
            SpinnerColumn(),
            TextColumn("[progress.description]{task.description}"),
            BarColumn(),
            TaskProgressColumn(),
            console=console
        ) as progress:

            # Create accounts
            task = progress.add_task("Creating accounts...", total=len(SAMPLE_ACCOUNTS))
            account_ids = []
            with get_api_client(base_url, token) as client:
                api = tracko_sdk.AccountsApi(client)
                for name, currency in SAMPLE_ACCOUNTS:
                    req = AccountSaveRequest(name=name, currency=currency)
                    result = unwrap_envelope(api.create7(req))
                    if result:
                        account_ids.append(result.id)
                    progress.update(task, advance=1)

            # Create categories
            task = progress.add_task("Creating categories...", total=len(SAMPLE_CATEGORIES))
            category_ids = {"EXPENSE": [], "INCOME": []}
            with get_api_client(base_url, token) as client:
                api = tracko_sdk.CategoriesApi(client)
                for name, cat_type in SAMPLE_CATEGORIES:
                    req = CategorySaveRequest(name=name, category_type=cat_type)
                    result = unwrap_envelope(api.create6(req))
                    if result:
                        category_ids[cat_type].append(result.id)
                    progress.update(task, advance=1)

            # Create contacts
            task = progress.add_task("Creating contacts...", total=len(SAMPLE_CONTACTS))
            contact_ids = []
            with get_api_client(base_url, token) as client:
                api = tracko_sdk.ContactsApi(client)
                for name, phone, email in SAMPLE_CONTACTS:
                    req = ContactSaveRequest(name=name, phone_no=phone, email=email)
                    result = unwrap_envelope(api.create5(req))
                    if result:
                        contact_ids.append(result.id)
                    progress.update(task, advance=1)

            # Create currencies
            task = progress.add_task("Creating currencies...", total=len(SAMPLE_CURRENCIES))
            with get_api_client(base_url, token) as client:
                api = tracko_sdk.UserCurrenciesApi(client)
                for code, rate in SAMPLE_CURRENCIES:
                    req = UserCurrencyRequest(currency_code=code, exchange_rate=rate)
                    unwrap_envelope(api.save(req))
                    progress.update(task, advance=1)

            # Create transactions
            if not skip_transactions and account_ids and category_ids["EXPENSE"]:
                num_transactions = 90
                task = progress.add_task("Creating transactions...", total=num_transactions)

                with get_api_client(base_url, token) as client:
                    api = tracko_sdk.TransactionsApi(client)
                    now = datetime.datetime.now()

                    for i in range(num_transactions):
                        days_ago = random.randint(0, 90)
                        txn_date = now - datetime.timedelta(days=days_ago)

                        is_expense = random.random() < 0.8
                        txn_type = "EXPENSE" if is_expense else "INCOME"
                        cats = category_ids[txn_type]

                        req = TransactionRequest(
                            account_id=random.choice(account_ids),
                            category_id=random.choice(cats) if cats else None,
                            amount=round(random.uniform(50, 5000), 2),
                            transaction_type=txn_type,
                            name=f"Sample {txn_type.lower()} {i+1}",
                            date=txn_date
                        )
                        unwrap_envelope(api.create1(req))
                        progress.update(task, advance=1)

            # Create budget allocations
            if category_ids["EXPENSE"]:
                num_allocations = min(5, len(category_ids["EXPENSE"]))
                task = progress.add_task("Creating budget allocations...", total=num_allocations)

                with get_api_client(base_url, token) as client:
                    api = tracko_sdk.BudgetApi(client)
                    now = datetime.datetime.now()

                    for cat_id in category_ids["EXPENSE"][:num_allocations]:
                        req = BudgetAllocationRequestDTO(
                            category_id=cat_id,
                            amount=round(random.uniform(1000, 10000), 2),
                            month=now.month,
                            year=now.year
                        )
                        unwrap_envelope(api.allocate_funds(req))
                        progress.update(task, advance=1)

        print_success("Database seeded successfully!")
        print_info(f"Created {len(account_ids)} accounts, {sum(len(v) for v in category_ids.values())} categories")

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)
