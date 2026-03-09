"""Currency management commands."""
import typer
from typing import Optional

from ..core.config import get_active_profile_config
from ..core.api import make_api_client, sdk_call_unwrapped
from ..core.output import console, create_table, print_json, print_success, print_error, spinner
from ..utils.prompts import confirm

import tracko_sdk
from tracko_sdk.models.user_currency_request import UserCurrencyRequest


app = typer.Typer(help="Currency management")


@app.command()
def list(raw: bool = typer.Option(False, "--raw", help="Output raw JSON")):
    """List all configured currencies."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner("Fetching currencies..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.UserCurrenciesApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_all())
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            currencies = result if result else []
            if currencies:
                table = create_table(title="Currencies")
                table.add_column("ID", justify="right", style="cyan")
                table.add_column("Code", style="green")
                table.add_column("Exchange Rate", justify="right", style="yellow")
                
                for curr in currencies:
                    curr_dict = curr.to_dict() if hasattr(curr, "to_dict") else curr
                    table.add_row(
                        str(curr_dict.get("id", "")),
                        str(curr_dict.get("currencyCode", "")),
                        f"{float(curr_dict.get('exchangeRate', 0)):.4f}"
                    )
                console.print(table)
            else:
                print_error("No currencies found")
    except Exception as e:
        print_error(f"Failed to list currencies: {e}")
        raise typer.Exit(1)


@app.command()
def add(
    code: str = typer.Option(..., "--code", "-c", help="Currency code (e.g., USD, EUR)"),
    rate: float = typer.Option(..., "--rate", "-r", help="Exchange rate"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Add a new currency configuration."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        req = UserCurrencyRequest(currency_code=code, exchange_rate=rate)
        with spinner(f"Adding currency {code}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.UserCurrenciesApi(api_client)
                result = sdk_call_unwrapped(lambda: api.save(req))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            print_success(f"Currency {code} added with rate {rate}")
            print_json(result)
    except Exception as e:
        print_error(f"Failed to add currency: {e}")
        raise typer.Exit(1)


@app.command()
def update(
    code: str = typer.Option(..., "--code", "-c", help="Currency code"),
    rate: float = typer.Option(..., "--rate", "-r", help="New exchange rate"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Update currency exchange rate."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        req = UserCurrencyRequest(currency_code=code, exchange_rate=rate)
        with spinner(f"Updating currency {code}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.UserCurrenciesApi(api_client)
                result = sdk_call_unwrapped(lambda: api.save(req))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            print_success(f"Currency {code} updated to rate {rate}")
            print_json(result)
    except Exception as e:
        print_error(f"Failed to update currency: {e}")
        raise typer.Exit(1)


@app.command()
def delete(
    code: str = typer.Argument(..., help="Currency code to delete"),
):
    """Delete a currency configuration."""
    if not confirm(f"Delete currency {code}?", default=False):
        print_error("Cancelled")
        raise typer.Exit(1)
    
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner(f"Deleting currency {code}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.UserCurrenciesApi(api_client)
                result = sdk_call_unwrapped(lambda: api.delete(currency_code=code))
        
        print_success(f"Currency {code} deleted successfully")
    except Exception as e:
        print_error(f"Failed to delete currency: {e}")
        raise typer.Exit(1)
