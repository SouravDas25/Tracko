"""Miscellaneous commands - Exchange rates and JSON store."""
import typer
from typing import Optional

from ..core.config import get_active_profile_config
from ..core.api import make_api_client, sdk_call_unwrapped
from ..core.output import console, create_table, print_json, print_success, print_error, spinner
from ..utils.prompts import confirm

import tracko_sdk
from tracko_sdk.models.json_store import JsonStore


# Exchange Rate Commands
exchange_app = typer.Typer(help="Exchange rate operations")


@exchange_app.command()
def get(
    base: str = typer.Option("USD", "--base", "-b", help="Base currency code"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get current exchange rates."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner(f"Fetching exchange rates for {base}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.ExchangeRatesApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_rates(base_code=base))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            rates_dict = result.to_dict() if hasattr(result, "to_dict") else result
            
            console.print(f"\n[bold cyan]Exchange Rates (Base: {rates_dict.get('baseCode', base)})[/bold cyan]")
            
            rates = rates_dict.get('rates', {})
            if rates:
                table = create_table()
                table.add_column("Currency", style="cyan")
                table.add_column("Rate", justify="right", style="green")
                
                for currency, rate in sorted(rates.items()):
                    table.add_row(currency, f"{rate:.4f}")
                console.print(table)
            else:
                print_error("No rates found")
    except Exception as e:
        print_error(f"Failed to get exchange rates: {e}")
        raise typer.Exit(1)


# JSON Store Commands
store_app = typer.Typer(help="JSON store operations")


@store_app.command()
def list(raw: bool = typer.Option(False, "--raw", help="Output raw JSON")):
    """List all JSON store entries."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner("Fetching JSON store entries..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.JSONStoreApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_all4())
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            entries = result if result else []
            if entries:
                table = create_table(title="JSON Store")
                table.add_column("Name", style="cyan")
                table.add_column("Value", style="green")
                
                for entry in entries:
                    entry_dict = entry.to_dict() if hasattr(entry, "to_dict") else entry
                    value = str(entry_dict.get("value", ""))
                    if len(value) > 50:
                        value = value[:47] + "..."
                    table.add_row(
                        entry_dict.get("name", ""),
                        value
                    )
                console.print(table)
            else:
                print_error("No entries found")
    except Exception as e:
        print_error(f"Failed to list entries: {e}")
        raise typer.Exit(1)


@store_app.command()
def get(
    name: str = typer.Argument(..., help="Entry name"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get JSON store entry by name."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner(f"Fetching entry '{name}'..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.JSONStoreApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_by_name(name=name))
        
        if result is None:
            raise typer.Exit(1)
        
        print_json(result)
    except Exception as e:
        print_error(f"Failed to get entry: {e}")
        raise typer.Exit(1)


@store_app.command()
def create(
    name: str = typer.Option(..., "--name", "-n", help="Entry name"),
    value: str = typer.Option(..., "--value", "-v", help="JSON value"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Create a JSON store entry."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        entry = JsonStore(name=name, value=value)
        
        with spinner(f"Creating entry '{name}'..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.JSONStoreApi(api_client)
                result = sdk_call_unwrapped(lambda: api.create4(entry))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            print_success(f"Entry '{name}' created successfully")
            print_json(result)
    except Exception as e:
        print_error(f"Failed to create entry: {e}")
        raise typer.Exit(1)


@store_app.command()
def update(
    name: str = typer.Argument(..., help="Entry name"),
    value: str = typer.Option(..., "--value", "-v", help="New JSON value"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Update a JSON store entry."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        entry = JsonStore(name=name, value=value)
        
        with spinner(f"Updating entry '{name}'..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.JSONStoreApi(api_client)
                result = sdk_call_unwrapped(lambda: api.update2(name=name, json_store=entry))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            print_success(f"Entry '{name}' updated successfully")
            print_json(result)
    except Exception as e:
        print_error(f"Failed to update entry: {e}")
        raise typer.Exit(1)


@store_app.command()
def delete(
    name: str = typer.Argument(..., help="Entry name"),
):
    """Delete a JSON store entry."""
    if not confirm(f"Delete entry '{name}'?", default=False):
        print_error("Cancelled")
        raise typer.Exit(1)
    
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner(f"Deleting entry '{name}'..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.JSONStoreApi(api_client)
                result = sdk_call_unwrapped(lambda: api.delete4(name=name))
        
        print_success(f"Entry '{name}' deleted successfully")
    except Exception as e:
        print_error(f"Failed to delete entry: {e}")
        raise typer.Exit(1)
