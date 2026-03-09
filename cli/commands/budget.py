"""Budget management commands."""
import typer
from typing import Optional
from datetime import datetime
from rich.progress import BarColumn

from ..core.config import get_active_profile_config
from ..core.api import make_api_client, sdk_call_unwrapped
from ..core.output import console, create_table, print_json, print_success, print_error, spinner

import tracko_sdk
from tracko_sdk.models.budget_allocation_request_dto import BudgetAllocationRequestDTO


app = typer.Typer(help="Budget management")


@app.command()
def view(
    month: Optional[int] = typer.Option(None, "--month", help="Month (1-12)"),
    year: Optional[int] = typer.Option(None, "--year", help="Year"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """View budget for a specific month."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    # Default to current month/year
    now = datetime.now()
    month = month or now.month
    year = year or now.year
    
    try:
        with spinner(f"Fetching budget for {month}/{year}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.BudgetApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_budget(month=month, year=year))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            budget_dict = result.to_dict() if hasattr(result, "to_dict") else result
            
            console.print(f"\n[bold cyan]Budget for {month}/{year}[/bold cyan]")
            console.print(f"Total Budget: [green]{budget_dict.get('totalBudget', 0):.2f}[/green]")
            console.print(f"Total Income: [green]{budget_dict.get('totalIncome', 0):.2f}[/green]")
            console.print(f"Total Spent: [red]{budget_dict.get('totalSpent', 0):.2f}[/red]")
            console.print(f"Available to Assign: [yellow]{budget_dict.get('availableToAssign', 0):.2f}[/yellow]")
            
            categories = budget_dict.get('categories', [])
            if categories:
                table = create_table(title="Category Allocations")
                table.add_column("Category", style="cyan")
                table.add_column("Allocated", justify="right", style="green")
                table.add_column("Spent", justify="right", style="red")
                table.add_column("Remaining", justify="right", style="yellow")
                table.add_column("Usage", style="magenta")
                
                for cat in categories:
                    allocated = cat.get('allocatedAmount', 0)
                    spent = cat.get('actualSpent', 0)
                    remaining = cat.get('remainingBalance', 0)
                    usage_pct = (spent / allocated * 100) if allocated > 0 else 0
                    
                    table.add_row(
                        cat.get('categoryName', ''),
                        f"{allocated:.2f}",
                        f"{spent:.2f}",
                        f"{remaining:.2f}",
                        f"{usage_pct:.1f}%"
                    )
                console.print(table)
    except Exception as e:
        print_error(f"Failed to view budget: {e}")
        raise typer.Exit(1)


@app.command()
def current(raw: bool = typer.Option(False, "--raw", help="Output raw JSON")):
    """View current month's budget."""
    now = datetime.now()
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner("Fetching current budget..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.BudgetApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_current_budget())
        
        if result is None:
            raise typer.Exit(1)
        
        print_json(result)
    except Exception as e:
        print_error(f"Failed to get current budget: {e}")
        raise typer.Exit(1)


@app.command()
def allocate(
    category_id: int = typer.Option(..., "--category-id", help="Category ID"),
    amount: float = typer.Option(..., "--amount", "-a", help="Amount to allocate"),
    month: Optional[int] = typer.Option(None, "--month", help="Month (1-12)"),
    year: Optional[int] = typer.Option(None, "--year", help="Year"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Allocate budget to a category."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
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
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.BudgetApi(api_client)
                result = sdk_call_unwrapped(lambda: api.allocate_funds(req))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            print_success(f"Allocated {amount} to category {category_id} for {month}/{year}")
            print_json(result)
    except Exception as e:
        print_error(f"Failed to allocate budget: {e}")
        raise typer.Exit(1)


@app.command()
def available(
    month: Optional[int] = typer.Option(None, "--month", help="Month (1-12)"),
    year: Optional[int] = typer.Option(None, "--year", help="Year"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get available amount to assign."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    # Default to current month/year
    now = datetime.now()
    month = month or now.month
    year = year or now.year
    
    try:
        with spinner(f"Fetching available amount for {month}/{year}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.BudgetApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_available_to_assign(month=month, year=year))
        
        if result is None:
            raise typer.Exit(1)
        
        print_json(result)
    except Exception as e:
        print_error(f"Failed to get available amount: {e}")
        raise typer.Exit(1)
