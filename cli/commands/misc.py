"""Miscellaneous commands - Exchange rates and JSON store."""
import sys

import typer
from typing import Optional

from tracko_sdk.models.json_store import JsonStore
from tracko_sdk.rest import ApiException
from urllib3.exceptions import MaxRetryError, NewConnectionError

from ..core.api import get_config_for_api, get_api_client, unwrap_envelope, handle_api_error
from ..core.output import console, create_table, print_json, print_success, print_error, spinner
from ..utils.prompts import confirm

import tracko_sdk


# Exchange Rate Commands
exchange_app = typer.Typer(help="Exchange rate operations")


@exchange_app.command()
def get(
    base: str = typer.Option("USD", "--base", "-b", help="Base currency code"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get current exchange rates."""
    base_url, token = get_config_for_api()

    try:
        with spinner(f"Fetching exchange rates for {base}..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.ExchangeRatesApi(client).get_rates(base_currency=base))

        if result is None:
            raise typer.Exit(1)

        if raw:
            print_json(result)
        else:
            console.print(f"\n[bold cyan]Exchange Rates (Base: {result.base_code or base})[/bold cyan]")

            rates = result.rates or {}
            if rates:
                table = create_table()
                table.add_column("Currency", style="cyan")
                table.add_column("Rate", justify="right", style="green")

                for currency, rate in sorted(rates.items()):
                    table.add_row(currency, f"{rate:.4f}")
                console.print(table)
            else:
                print_error("No rates found")

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


# JSON Store Commands
store_app = typer.Typer(help="JSON store operations")


@store_app.command()
def list(raw: bool = typer.Option(False, "--raw", help="Output raw JSON")):
    """List all JSON store entries."""
    base_url, token = get_config_for_api()

    try:
        with spinner("Fetching JSON store entries..."):
            with get_api_client(base_url, token) as client:
                entries = unwrap_envelope(tracko_sdk.JSONStoreApi(client).get_all4()) or []

        if raw:
            print_json([e.model_dump(by_alias=True) for e in entries])
        elif entries:
            table = create_table(title="JSON Store")
            table.add_column("Name", style="cyan")
            table.add_column("Value", style="green")

            for entry in entries:
                value = str(entry.value or "")
                if len(value) > 50:
                    value = value[:47] + "..."
                table.add_row(entry.name or "", value)
            console.print(table)
        else:
            print_error("No entries found")

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@store_app.command()
def get(
    name: str = typer.Argument(..., help="Entry name"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get JSON store entry by name."""
    base_url, token = get_config_for_api()

    try:
        with spinner(f"Fetching entry '{name}'..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.JSONStoreApi(client).get_by_name(name=name))

        if result is None:
            raise typer.Exit(1)

        print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@store_app.command()
def create(
    name: str = typer.Option(..., "--name", "-n", help="Entry name"),
    value: str = typer.Option(..., "--value", "-v", help="JSON value"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Create a JSON store entry."""
    base_url, token = get_config_for_api()

    try:
        entry = JsonStore(name=name, value=value)

        with spinner(f"Creating entry '{name}'..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.JSONStoreApi(client).create4(entry))

        if raw:
            print_json(result)
        else:
            print_success(f"Entry '{name}' created successfully")
            print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@store_app.command()
def update(
    name: str = typer.Argument(..., help="Entry name"),
    value: str = typer.Option(..., "--value", "-v", help="New JSON value"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Update a JSON store entry."""
    base_url, token = get_config_for_api()

    try:
        entry = JsonStore(name=name, value=value)

        with spinner(f"Updating entry '{name}'..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.JSONStoreApi(client).update2(name=name, json_store=entry))

        if raw:
            print_json(result)
        else:
            print_success(f"Entry '{name}' updated successfully")
            print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@store_app.command()
def delete(
    name: str = typer.Argument(..., help="Entry name"),
):
    """Delete a JSON store entry."""
    if not confirm(f"Delete entry '{name}'?", default=False):
        print_error("Cancelled")
        raise typer.Exit(1)

    base_url, token = get_config_for_api()

    try:
        with spinner(f"Deleting entry '{name}'..."):
            with get_api_client(base_url, token) as client:
                unwrap_envelope(tracko_sdk.JSONStoreApi(client).delete4(name=name))

        print_success(f"Entry '{name}' deleted successfully")

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)
