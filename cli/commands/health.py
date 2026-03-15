"""Health check command."""
import sys

import typer
from urllib3.exceptions import MaxRetryError, NewConnectionError

from ..core.api import get_config_for_api, get_api_client
from ..core.output import console, print_json, print_success, print_error, spinner

import tracko_sdk


app = typer.Typer(help="Health check commands")


@app.command()
def check(
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Check API health status."""
    base_url, _ = get_config_for_api(require_token=False)

    try:
        with spinner("Checking API health..."):
            with get_api_client(base_url) as client:
                result = tracko_sdk.HealthApi(client).health()

        if result is None:
            print_error("Health check failed")
            raise typer.Exit(1)

        if raw:
            print_json(result)
        else:
            print_success(f"API is healthy at {base_url}")
            if isinstance(result, dict):
                for key, value in result.items():
                    console.print(f"  {key}: [cyan]{value}[/cyan]")

    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)
