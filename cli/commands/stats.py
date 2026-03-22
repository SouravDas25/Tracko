"""Statistics and analytics commands."""
import sys
from datetime import datetime

import typer
from typing import Optional

from tracko_sdk.rest import ApiException
from urllib3.exceptions import MaxRetryError, NewConnectionError

from ..core.api import get_config_for_api, get_api_client, unwrap_envelope, handle_api_error
from ..core.output import console, create_table, print_json, print_error, spinner

import tracko_sdk


app = typer.Typer(help="Statistics and analytics")


def _parse_date(value: Optional[str]) -> Optional[datetime]:
    """Parse a YYYY-MM-DD string into a datetime, or return None."""
    if not value:
        return None
    return datetime.strptime(value, "%Y-%m-%d")


@app.command()
def summary(
    range: str = typer.Option(..., "--range", "-r", help="Range (WEEK/MONTH/YEAR/FIVE_YEAR/TEN_YEAR/CUSTOM)"),
    transaction_type: str = typer.Option(..., "--type", "-t", help="Type (INCOME/EXPENSE)"),
    start_date: Optional[str] = typer.Option(None, "--start-date", help="Start date (YYYY-MM-DD)"),
    end_date: Optional[str] = typer.Option(None, "--end-date", help="End date (YYYY-MM-DD)"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get transaction statistics summary."""
    base_url, token = get_config_for_api()

    try:
        with spinner("Calculating statistics..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.StatisticsApi(client).get_stats(
                    range=range,
                    transaction_type=transaction_type,
                    start_date=_parse_date(start_date),
                    end_date=_parse_date(end_date),
                ))

        if result is None:
            raise typer.Exit(1)

        if raw:
            print_json(result)
        else:
            console.print(f"\n[bold cyan]Statistics Summary[/bold cyan]")
            console.print(f"Range: [yellow]{result.range or ''}[/yellow]")
            console.print(f"Type: [blue]{result.transaction_type or ''}[/blue]")
            console.print(f"Total: [green]{result.total or 0:.2f}[/green]")

            series = result.series or []
            if series:
                table = create_table(title="Time Series")
                table.add_column("Period", style="cyan")
                table.add_column("Amount", justify="right", style="green")

                for point in series:
                    table.add_row(
                        point.label or '',
                        f"{point.value or 0:.2f}"
                    )
                console.print(table)

            categories = result.categories or []
            if categories:
                table = create_table(title="By Category")
                table.add_column("Category", style="cyan")
                table.add_column("Amount", justify="right", style="green")

                for cat in categories:
                    table.add_row(
                        cat.category_name or '',
                        f"{cat.amount or 0:.2f}"
                    )
                console.print(table)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def category_summary(
    category_id: int = typer.Option(..., "--category-id", help="Category ID"),
    range: str = typer.Option(..., "--range", "-r", help="Range (WEEK/MONTH/YEAR/FIVE_YEAR/TEN_YEAR/CUSTOM)"),
    transaction_type: str = typer.Option(..., "--type", "-t", help="Type (INCOME/EXPENSE)"),
    start_date: Optional[str] = typer.Option(None, "--start-date", help="Start date (YYYY-MM-DD)"),
    end_date: Optional[str] = typer.Option(None, "--end-date", help="End date (YYYY-MM-DD)"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get statistics for a specific category."""
    base_url, token = get_config_for_api()

    try:
        with spinner(f"Calculating statistics for category {category_id}..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.StatisticsApi(client).get_category_stats(
                    category_id=category_id,
                    range=range,
                    transaction_type=transaction_type,
                    start_date=_parse_date(start_date),
                    end_date=_parse_date(end_date),
                ))

        if result is None:
            raise typer.Exit(1)

        if raw:
            print_json(result)
        else:
            console.print(f"\n[bold cyan]Category Statistics[/bold cyan]")
            console.print(f"Category ID: [yellow]{category_id}[/yellow]")
            console.print(f"Range: [yellow]{result.range or ''}[/yellow]")
            console.print(f"Type: [blue]{result.transaction_type or ''}[/blue]")
            console.print(f"Total: [green]{result.total or 0:.2f}[/green]")

            series = result.series or []
            if series:
                table = create_table(title="Time Series")
                table.add_column("Period", style="cyan")
                table.add_column("Amount", justify="right", style="green")

                for point in series:
                    table.add_row(
                        point.label or '',
                        f"{point.value or 0:.2f}"
                    )
                console.print(table)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)
