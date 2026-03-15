"""Account management commands."""
import sys

import typer
from typing import Optional
from datetime import datetime
from tracko_sdk.models.account_save_request import AccountSaveRequest
from tracko_sdk.rest import ApiException
from urllib3.exceptions import MaxRetryError, NewConnectionError

from ..core.api import get_config_for_api, get_api_client, unwrap_envelope, handle_api_error
from ..core.output import console, create_table, print_json, print_success, print_error, spinner
from ..utils.prompts import confirm

import tracko_sdk


app = typer.Typer(help="Account management")


@app.command()
def list(raw: bool = typer.Option(False, "--raw", help="Output raw JSON")):
    """List all accounts."""
    base_url, token = get_config_for_api()

    try:
        with spinner("Fetching accounts..."):
            with get_api_client(base_url, token) as client:
                accounts = unwrap_envelope(tracko_sdk.AccountsApi(client).get_all6()) or []

        if raw:
            print_json([acc.model_dump(by_alias=True) for acc in accounts])
        elif accounts:
            table = create_table(title="Accounts")
            table.add_column("ID", justify="right", style="cyan")
            table.add_column("Name", style="green")
            table.add_column("Currency", style="yellow")

            for acc in accounts:
                table.add_row(
                    str(acc.id),
                    str(acc.name),
                    str(acc.currency or ""),
                )
            console.print(table)
        else:
            print_error("No accounts found")

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def balances(raw: bool = typer.Option(False, "--raw", help="Output raw JSON")):
    """Get account balances."""
    base_url, token = get_config_for_api()

    try:
        with spinner("Fetching balances..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.AccountsApi(client).get_my_account_balances())

        print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def add(
    name: str = typer.Option(..., "--name", "-n", help="Account name"),
    currency: Optional[str] = typer.Option(None, "--currency", "-c", help="Currency code"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Create a new account."""
    base_url, token = get_config_for_api()

    try:
        req = AccountSaveRequest(name=name, currency=currency)
        with spinner(f"Creating account '{name}'..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.AccountsApi(client).create7(req))

        if raw:
            print_json(result)
        else:
            print_success(f"Account '{name}' created successfully")
            print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def get(
    id: int = typer.Argument(..., help="Account ID"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get account by ID."""
    base_url, token = get_config_for_api()

    try:
        with spinner(f"Fetching account {id}..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.AccountsApi(client).get_by_id4(id=id))

        print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def update(
    id: int = typer.Argument(..., help="Account ID"),
    name: str = typer.Option(..., "--name", "-n", help="Account name"),
    currency: Optional[str] = typer.Option(None, "--currency", "-c", help="Currency code"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Update an account."""
    base_url, token = get_config_for_api()

    try:
        req = AccountSaveRequest(name=name, currency=currency)
        with spinner(f"Updating account {id}..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.AccountsApi(client).update5(id=id, account_save_request=req))

        if raw:
            print_json(result)
        else:
            print_success(f"Account {id} updated successfully")
            print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def delete(
    id: int = typer.Argument(..., help="Account ID"),
):
    """Delete an account."""
    if not confirm(f"Delete account {id}?", default=False):
        print_error("Cancelled")
        raise typer.Exit(1)

    base_url, token = get_config_for_api()

    try:
        with spinner(f"Deleting account {id}..."):
            with get_api_client(base_url, token) as client:
                unwrap_envelope(tracko_sdk.AccountsApi(client).delete7(id=id))

        print_success(f"Account {id} deleted successfully")

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def summary(
    id: int = typer.Argument(..., help="Account ID"),
    start_date: str = typer.Option(..., "--start-date", help="Start date (YYYY-MM-DD)"),
    end_date: str = typer.Option(..., "--end-date", help="End date (YYYY-MM-DD)"),
    include_rollover: bool = typer.Option(False, "--include-rollover", help="Include rollover"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get account summary."""
    base_url, token = get_config_for_api()

    try:
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")

        with spinner(f"Fetching summary for account {id}..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.AccountsApi(client).get_account_summary(
                    id=id,
                    start_date=start,
                    end_date=end,
                    include_rollover=include_rollover if include_rollover else None,
                ))

        print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def transactions(
    id: int = typer.Argument(..., help="Account ID"),
    month: Optional[int] = typer.Option(None, "--month", help="Month"),
    year: Optional[int] = typer.Option(None, "--year", help="Year"),
    start_date: Optional[str] = typer.Option(None, "--start-date", help="Start date (YYYY-MM-DD)"),
    end_date: Optional[str] = typer.Option(None, "--end-date", help="End date (YYYY-MM-DD)"),
    category_id: Optional[int] = typer.Option(None, "--category-id", help="Category ID"),
    page: Optional[int] = typer.Option(None, "--page", help="Page number"),
    size: Optional[int] = typer.Option(None, "--size", help="Page size"),
    expand: bool = typer.Option(False, "--expand", help="Expand details"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get account transactions."""
    base_url, token = get_config_for_api()

    try:
        start = datetime.strptime(start_date, "%Y-%m-%d") if start_date else None
        end = datetime.strptime(end_date, "%Y-%m-%d") if end_date else None

        with spinner(f"Fetching transactions for account {id}..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.AccountsApi(client).get_account_transactions(
                    id=id,
                    month=month,
                    year=year,
                    start_date=start,
                    end_date=end,
                    category_id=category_id,
                    page=page,
                    size=size,
                    expand=expand if expand else None,
                ))

        print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)
