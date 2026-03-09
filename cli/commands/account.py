"""Account management commands."""
import typer
from typing import Optional
from datetime import datetime

from ..core.config import get_active_profile_config
from ..core.api import make_api_client, sdk_call_unwrapped
from ..core.output import console, create_table, print_json, print_success, print_error, spinner
from ..utils.prompts import confirm

import tracko_sdk
from tracko_sdk.models.account_save_request import AccountSaveRequest


app = typer.Typer(help="Account management")


@app.command()
def list(raw: bool = typer.Option(False, "--raw", help="Output raw JSON")):
    """List all accounts."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner("Fetching accounts..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.AccountsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_all6())
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            accounts = result if result else []
            if accounts:
                table = create_table(title="Accounts")
                table.add_column("ID", justify="right", style="cyan")
                table.add_column("Name", style="green")
                table.add_column("Currency", style="yellow")
                
                for acc in accounts:
                    acc_dict = acc.to_dict() if hasattr(acc, "to_dict") else acc
                    table.add_row(
                        str(acc_dict.get("id", "")),
                        str(acc_dict.get("name", "")),
                        str(acc_dict.get("currency", ""))
                    )
                console.print(table)
            else:
                print_error("No accounts found")
    except Exception as e:
        print_error(f"Failed to list accounts: {e}")
        raise typer.Exit(1)


@app.command()
def balances(raw: bool = typer.Option(False, "--raw", help="Output raw JSON")):
    """Get account balances."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner("Fetching balances..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.AccountsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_my_account_balances())
        
        if result is None:
            raise typer.Exit(1)
        
        print_json(result)
    except Exception as e:
        print_error(f"Failed to get balances: {e}")
        raise typer.Exit(1)


@app.command()
def add(
    name: str = typer.Option(..., "--name", "-n", help="Account name"),
    currency: Optional[str] = typer.Option(None, "--currency", "-c", help="Currency code"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Create a new account."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        req = AccountSaveRequest(name=name, currency=currency)
        with spinner(f"Creating account '{name}'..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.AccountsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.create7(req))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            print_success(f"Account '{name}' created successfully")
            print_json(result)
    except Exception as e:
        print_error(f"Failed to create account: {e}")
        raise typer.Exit(1)


@app.command()
def get(
    id: int = typer.Argument(..., help="Account ID"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get account by ID."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner(f"Fetching account {id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.AccountsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_by_id4(id=id))
        
        if result is None:
            raise typer.Exit(1)
        
        print_json(result)
    except Exception as e:
        print_error(f"Failed to get account: {e}")
        raise typer.Exit(1)


@app.command()
def update(
    id: int = typer.Argument(..., help="Account ID"),
    name: str = typer.Option(..., "--name", "-n", help="Account name"),
    currency: Optional[str] = typer.Option(None, "--currency", "-c", help="Currency code"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Update an account."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        req = AccountSaveRequest(name=name, currency=currency)
        with spinner(f"Updating account {id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.AccountsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.update5(id=id, account_save_request=req))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            print_success(f"Account {id} updated successfully")
            print_json(result)
    except Exception as e:
        print_error(f"Failed to update account: {e}")
        raise typer.Exit(1)


@app.command()
def delete(
    id: int = typer.Argument(..., help="Account ID"),
):
    """Delete an account."""
    if not confirm(f"Delete account {id}?", default=False):
        print_error("Cancelled")
        raise typer.Exit(1)
    
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner(f"Deleting account {id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.AccountsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.delete7(id=id))
        
        print_success(f"Account {id} deleted successfully")
    except Exception as e:
        print_error(f"Failed to delete account: {e}")
        raise typer.Exit(1)


@app.command()
def summary(
    id: int = typer.Argument(..., help="Account ID"),
    start_date: str = typer.Option(..., "--start-date", help="Start date (YYYY-MM-DD)"),
    end_date: str = typer.Option(..., "--end-date", help="End date (YYYY-MM-DD)"),
    include_rollover: bool = typer.Option(False, "--include-rollover", help="Include rollover"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get account summary."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")
        
        with spinner(f"Fetching summary for account {id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.AccountsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_account_summary(
                    id=id,
                    start_date=start,
                    end_date=end,
                    include_rollover=include_rollover if include_rollover else None,
                ))
        
        if result is None:
            raise typer.Exit(1)
        
        print_json(result)
    except Exception as e:
        print_error(f"Failed to get summary: {e}")
        raise typer.Exit(1)


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
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        start = datetime.strptime(start_date, "%Y-%m-%d") if start_date else None
        end = datetime.strptime(end_date, "%Y-%m-%d") if end_date else None
        
        with spinner(f"Fetching transactions for account {id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.AccountsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_account_transactions(
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
        
        if result is None:
            raise typer.Exit(1)
        
        print_json(result)
    except Exception as e:
        print_error(f"Failed to get transactions: {e}")
        raise typer.Exit(1)
