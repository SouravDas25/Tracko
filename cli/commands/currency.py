"""Currency management commands."""
import sys

import typer
from tracko_sdk.models.user_currency_request import UserCurrencyRequest
from tracko_sdk.rest import ApiException
from urllib3.exceptions import MaxRetryError, NewConnectionError

from ..core.api import get_config_for_api, get_api_client, unwrap_envelope, handle_api_error
from ..core.output import console, create_table, print_json, print_success, print_error, spinner
from ..utils.prompts import confirm

import tracko_sdk


app = typer.Typer(help="Currency management")


@app.command()
def list(raw: bool = typer.Option(False, "--raw", help="Output raw JSON")):
    """List all configured currencies."""
    base_url, token = get_config_for_api()

    try:
        with spinner("Fetching currencies..."):
            with get_api_client(base_url, token) as client:
                currencies = unwrap_envelope(tracko_sdk.UserCurrenciesApi(client).get_all()) or []

        if raw:
            print_json([c.model_dump(by_alias=True) for c in currencies])
        elif currencies:
            table = create_table(title="Currencies")
            table.add_column("ID", justify="right", style="cyan")
            table.add_column("Code", style="green")
            table.add_column("Exchange Rate", justify="right", style="yellow")

            for curr in currencies:
                table.add_row(
                    str(curr.id),
                    str(curr.currency_code),
                    f"{float(curr.exchange_rate):.4f}",
                )
            console.print(table)
        else:
            print_error("No currencies found")

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def add(
    code: str = typer.Option(..., "--code", "-c", help="Currency code (e.g., USD, EUR)"),
    rate: float = typer.Option(..., "--rate", "-r", help="Exchange rate"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Add a new currency configuration."""
    base_url, token = get_config_for_api()

    try:
        req = UserCurrencyRequest(currency_code=code, exchange_rate=rate)
        with spinner(f"Adding currency {code}..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.UserCurrenciesApi(client).save(req))

        if raw:
            print_json(result)
        else:
            print_success(f"Currency {code} added with rate {rate}")
            print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def update(
    code: str = typer.Option(..., "--code", "-c", help="Currency code"),
    rate: float = typer.Option(..., "--rate", "-r", help="New exchange rate"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Update currency exchange rate."""
    base_url, token = get_config_for_api()

    try:
        req = UserCurrencyRequest(currency_code=code, exchange_rate=rate)
        with spinner(f"Updating currency {code}..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.UserCurrenciesApi(client).save(req))

        if raw:
            print_json(result)
        else:
            print_success(f"Currency {code} updated to rate {rate}")
            print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def delete(
    code: str = typer.Argument(..., help="Currency code to delete"),
):
    """Delete a currency configuration."""
    if not confirm(f"Delete currency {code}?", default=False):
        print_error("Cancelled")
        raise typer.Exit(1)

    base_url, token = get_config_for_api()

    try:
        with spinner(f"Deleting currency {code}..."):
            with get_api_client(base_url, token) as client:
                unwrap_envelope(tracko_sdk.UserCurrenciesApi(client).delete(code=code))

        print_success(f"Currency {code} deleted successfully")

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)
