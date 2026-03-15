"""Budget management commands."""
import sys

import typer
from typing import Optional
from datetime import datetime

from tracko_sdk.models.budget_allocation_request_dto import BudgetAllocationRequestDTO
from tracko_sdk.rest import ApiException
from urllib3.exceptions import MaxRetryError, NewConnectionError

from ..core.api import get_config_for_api, get_api_client, unwrap_envelope, handle_api_error
from ..core.output import console, create_table, print_json, print_success, print_error, spinner

import tracko_sdk


app = typer.Typer(help="Budget management")


@app.command()
def view(
    month: Optional[int] = typer.Option(None, "--month", help="Month (1-12)"),
    year: Optional[int] = typer.Option(None, "--year", help="Year"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """View budget for a specific month."""
    base_url, token = get_config_for_api()

    # Default to current month/year
    now = datetime.now()
    month = month or now.month
    year = year or now.year

    try:
        with spinner(f"Fetching budget for {month}/{year}..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.BudgetApi(client).get_budget(month=month, year=year))

        if result is None:
            raise typer.Exit(1)

        if raw:
            print_json(result)
        else:
            console.print(f"\n[bold cyan]Budget for {month}/{year}[/bold cyan]")
            console.print(f"Total Budget: [green]{result.total_budget or 0:.2f}[/green]")
            console.print(f"Total Income: [green]{result.total_income or 0:.2f}[/green]")
            console.print(f"Total Spent: [red]{result.total_spent or 0:.2f}[/red]")
            console.print(f"Available to Assign: [yellow]{result.available_to_assign or 0:.2f}[/yellow]")

            categories = result.categories or []
            if categories:
                table = create_table(title="Category Allocations")
                table.add_column("Category", style="cyan")
                table.add_column("Allocated", justify="right", style="green")
                table.add_column("Spent", justify="right", style="red")
                table.add_column("Remaining", justify="right", style="yellow")
                table.add_column("Usage", style="magenta")

                for cat in categories:
                    allocated = cat.allocated_amount or 0
                    spent = cat.actual_spent or 0
                    remaining = cat.remaining_balance or 0
                    usage_pct = (spent / allocated * 100) if allocated > 0 else 0

                    table.add_row(
                        cat.category_name or '',
                        f"{allocated:.2f}",
                        f"{spent:.2f}",
                        f"{remaining:.2f}",
                        f"{usage_pct:.1f}%"
                    )
                console.print(table)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def current(raw: bool = typer.Option(False, "--raw", help="Output raw JSON")):
    """View current month's budget."""
    base_url, token = get_config_for_api()

    try:
        with spinner("Fetching current budget..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.BudgetApi(client).get_current_budget())

        if result is None:
            raise typer.Exit(1)

        print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def allocate(
    category_id: int = typer.Option(..., "--category-id", help="Category ID"),
    amount: float = typer.Option(..., "--amount", "-a", help="Amount to allocate"),
    month: Optional[int] = typer.Option(None, "--month", help="Month (1-12)"),
    year: Optional[int] = typer.Option(None, "--year", help="Year"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Allocate budget to a category."""
    base_url, token = get_config_for_api()

    # Default to current month/year
    now = datetime.now()
    month = month or now.month
    year = year or now.year

    try:
        req = BudgetAllocationRequestDTO(
            category_id=category_id,
            amount=amount,
            month=month,
            year=year
        )

        with spinner(f"Allocating {amount} to category {category_id}..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.BudgetApi(client).allocate_funds(req))

        if result is None:
            raise typer.Exit(1)

        if raw:
            print_json(result)
        else:
            print_success(f"Allocated {amount} to category {category_id} for {month}/{year}")
            print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def available(
    month: Optional[int] = typer.Option(None, "--month", help="Month (1-12)"),
    year: Optional[int] = typer.Option(None, "--year", help="Year"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get available amount to assign."""
    base_url, token = get_config_for_api()

    # Default to current month/year
    now = datetime.now()
    month = month or now.month
    year = year or now.year

    try:
        with spinner(f"Fetching available amount for {month}/{year}..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.BudgetApi(client).get_available_to_assign(month=month, year=year))

        if result is None:
            raise typer.Exit(1)

        print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)
