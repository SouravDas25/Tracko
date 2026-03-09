"""Statistics and analytics commands."""
import typer
from typing import Optional

from ..core.config import get_active_profile_config
from ..core.api import make_api_client, sdk_call_unwrapped
from ..core.output import console, create_table, print_json, print_error, spinner

import tracko_sdk


app = typer.Typer(help="Statistics and analytics")


@app.command()
def summary(
    range: str = typer.Option(..., "--range", "-r", help="Range (WEEK/MONTH/YEAR/CUSTOM)"),
    transaction_type: str = typer.Option(..., "--type", "-t", help="Type (INCOME/EXPENSE)"),
    start_date: Optional[str] = typer.Option(None, "--start-date", help="Start date (YYYY-MM-DD)"),
    end_date: Optional[str] = typer.Option(None, "--end-date", help="End date (YYYY-MM-DD)"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get transaction statistics summary."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner("Calculating statistics..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.StatisticsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_stats(
                    range=range,
                    transaction_type=transaction_type,
                    start_date=start_date,
                    end_date=end_date
                ))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            stats_dict = result.to_dict() if hasattr(result, "to_dict") else result
            
            console.print(f"\n[bold cyan]Statistics Summary[/bold cyan]")
            console.print(f"Range: [yellow]{stats_dict.get('range', '')}[/yellow]")
            console.print(f"Type: [blue]{stats_dict.get('transactionType', '')}[/blue]")
            console.print(f"Total: [green]{stats_dict.get('total', 0):.2f}[/green]")
            
            series = stats_dict.get('series', [])
            if series:
                table = create_table(title="Time Series")
                table.add_column("Period", style="cyan")
                table.add_column("Amount", justify="right", style="green")
                
                for point in series:
                    table.add_row(
                        point.get('label', ''),
                        f"{point.get('value', 0):.2f}"
                    )
                console.print(table)
            
            categories = stats_dict.get('categories', [])
            if categories:
                table = create_table(title="By Category")
                table.add_column("Category", style="cyan")
                table.add_column("Amount", justify="right", style="green")
                
                for cat in categories:
                    table.add_row(
                        cat.get('categoryName', ''),
                        f"{cat.get('amount', 0):.2f}"
                    )
                console.print(table)
    except Exception as e:
        print_error(f"Failed to get statistics: {e}")
        raise typer.Exit(1)


@app.command()
def category_summary(
    category_id: int = typer.Option(..., "--category-id", help="Category ID"),
    range: str = typer.Option(..., "--range", "-r", help="Range (WEEK/MONTH/YEAR/CUSTOM)"),
    transaction_type: str = typer.Option(..., "--type", "-t", help="Type (INCOME/EXPENSE)"),
    start_date: Optional[str] = typer.Option(None, "--start-date", help="Start date (YYYY-MM-DD)"),
    end_date: Optional[str] = typer.Option(None, "--end-date", help="End date (YYYY-MM-DD)"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get statistics for a specific category."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner(f"Calculating statistics for category {category_id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.StatisticsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_category_stats(
                    category_id=category_id,
                    range=range,
                    transaction_type=transaction_type,
                    start_date=start_date,
                    end_date=end_date
                ))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            stats_dict = result.to_dict() if hasattr(result, "to_dict") else result
            
            console.print(f"\n[bold cyan]Category Statistics[/bold cyan]")
            console.print(f"Category ID: [yellow]{stats_dict.get('categoryId', '')}[/yellow]")
            console.print(f"Range: [yellow]{stats_dict.get('range', '')}[/yellow]")
            console.print(f"Type: [blue]{stats_dict.get('transactionType', '')}[/blue]")
            console.print(f"Total: [green]{stats_dict.get('total', 0):.2f}[/green]")
            
            series = stats_dict.get('series', [])
            if series:
                table = create_table(title="Time Series")
                table.add_column("Period", style="cyan")
                table.add_column("Amount", justify="right", style="green")
                
                for point in series:
                    table.add_row(
                        point.get('label', ''),
                        f"{point.get('value', 0):.2f}"
                    )
                console.print(table)
    except Exception as e:
        print_error(f"Failed to get category statistics: {e}")
        raise typer.Exit(1)
