"""Category management commands."""
import typer
from typing import Optional

from ..core.config import get_active_profile_config
from ..core.api import make_api_client, sdk_call_unwrapped
from ..core.output import console, create_table, print_json, print_success, print_error, spinner
from ..utils.prompts import confirm

import tracko_sdk
from tracko_sdk.models.category_save_request import CategorySaveRequest


app = typer.Typer(help="Category management")


@app.command()
def list(raw: bool = typer.Option(False, "--raw", help="Output raw JSON")):
    """List all categories."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner("Fetching categories..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.CategoriesApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_all5())
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            categories = result if result else []
            if categories and len(categories) > 0:
                table = create_table(title="Categories")
                table.add_column("ID", justify="right", style="cyan")
                table.add_column("Name", style="green")
                table.add_column("Type", style="yellow")
                
                for cat in categories:
                    cat_dict = cat.to_dict() if hasattr(cat, "to_dict") else cat
                    table.add_row(
                        str(cat_dict.get("id", "")),
                        cat_dict.get("name", ""),
                        cat_dict.get("categoryType", "")
                    )
                console.print(table)
            else:
                print_error("No categories found")
    except Exception as e:
        print_error(f"Failed to list categories: {e}")
        raise typer.Exit(1)


@app.command()
def add(
    name: str = typer.Option(..., "--name", "-n", help="Category name"),
    category_type: Optional[str] = typer.Option(None, "--type", "-t", help="Category type (INCOME/EXPENSE)"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Create a new category."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        req = CategorySaveRequest(name=name, category_type=category_type)
        with spinner(f"Creating category '{name}'..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.CategoriesApi(api_client)
                result = sdk_call_unwrapped(lambda: api.create6(req))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            print_success(f"Category '{name}' created successfully")
            print_json(result)
    except Exception as e:
        print_error(f"Failed to create category: {e}")
        raise typer.Exit(1)


@app.command()
def get(
    id: int = typer.Argument(..., help="Category ID"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get category by ID."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner(f"Fetching category {id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.CategoriesApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_by_id3(id=id))
        
        if result is None:
            raise typer.Exit(1)
        
        print_json(result)
    except Exception as e:
        print_error(f"Failed to get category: {e}")
        raise typer.Exit(1)


@app.command()
def update(
    id: int = typer.Argument(..., help="Category ID"),
    name: str = typer.Option(..., "--name", "-n", help="Category name"),
    category_type: Optional[str] = typer.Option(None, "--type", "-t", help="Category type"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Update a category."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        req = CategorySaveRequest(name=name, category_type=category_type)
        with spinner(f"Updating category {id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.CategoriesApi(api_client)
                result = sdk_call_unwrapped(lambda: api.update4(id=id, category_save_request=req))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            print_success(f"Category {id} updated successfully")
            print_json(result)
    except Exception as e:
        print_error(f"Failed to update category: {e}")
        raise typer.Exit(1)


@app.command()
def delete(
    id: int = typer.Argument(..., help="Category ID"),
):
    """Delete a category."""
    if not confirm(f"Delete category {id}?", default=False):
        print_error("Cancelled")
        raise typer.Exit(1)
    
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner(f"Deleting category {id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.CategoriesApi(api_client)
                result = sdk_call_unwrapped(lambda: api.delete6(id=id))
        
        print_success(f"Category {id} deleted successfully")
    except Exception as e:
        print_error(f"Failed to delete category: {e}")
        raise typer.Exit(1)
