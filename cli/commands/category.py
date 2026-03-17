"""Category management commands."""
import sys

import typer
from typing import Optional
from tracko_sdk.models.category_save_request import CategorySaveRequest
from tracko_sdk.rest import ApiException
from urllib3.exceptions import MaxRetryError, NewConnectionError

from ..core.api import get_config_for_api, get_api_client, unwrap_envelope, handle_api_error
from ..core.output import console, create_table, print_json, print_success, print_error, spinner
from ..utils.prompts import confirm

import tracko_sdk


app = typer.Typer(help="Category management")


@app.command()
def list(raw: bool = typer.Option(False, "--raw", help="Output raw JSON")):
    """List all categories."""
    base_url, token = get_config_for_api()

    try:
        with spinner("Fetching categories..."):
            with get_api_client(base_url, token) as client:
                categories = unwrap_envelope(tracko_sdk.CategoriesApi(client).get_all5()) or []

        if raw:
            print_json([cat.model_dump(by_alias=True) for cat in categories])
        elif categories:
            table = create_table(title="Categories")
            table.add_column("ID", justify="right", style="cyan")
            table.add_column("Name", style="green")
            table.add_column("Type", style="yellow")

            for cat in categories:
                table.add_row(
                    str(cat.id),
                    cat.name,
                    cat.category_type or "",
                )
            console.print(table)
        else:
            print_error("No categories found")

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def add(
    name: str = typer.Option(..., "--name", "-n", help="Category name"),
    category_type: Optional[str] = typer.Option(None, "--type", "-t", help="Category type (INCOME/EXPENSE)"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Create a new category."""
    base_url, token = get_config_for_api()

    try:
        req = CategorySaveRequest(name=name, category_type=category_type)
        with spinner(f"Creating category '{name}'..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.CategoriesApi(client).create6(req))

        if raw:
            print_json(result)
        else:
            print_success(f"Category '{name}' created successfully")
            print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def get(
    id: int = typer.Argument(..., help="Category ID"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get category by ID."""
    base_url, token = get_config_for_api()

    try:
        with spinner(f"Fetching category {id}..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.CategoriesApi(client).get_by_id3(id=id))

        print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def update(
    id: int = typer.Argument(..., help="Category ID"),
    name: str = typer.Option(..., "--name", "-n", help="Category name"),
    category_type: Optional[str] = typer.Option(None, "--type", "-t", help="Category type"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Update a category."""
    base_url, token = get_config_for_api()

    try:
        req = CategorySaveRequest(name=name, category_type=category_type)
        with spinner(f"Updating category {id}..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.CategoriesApi(client).update4(id=id, category_save_request=req))

        if raw:
            print_json(result)
        else:
            print_success(f"Category {id} updated successfully")
            print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def delete(
    id: int = typer.Argument(..., help="Category ID"),
):
    """Delete a category."""
    if not confirm(f"Delete category {id}?", default=False):
        print_error("Cancelled")
        raise typer.Exit(1)

    base_url, token = get_config_for_api()

    try:
        with spinner(f"Deleting category {id}..."):
            with get_api_client(base_url, token) as client:
                unwrap_envelope(tracko_sdk.CategoriesApi(client).delete6(id=id))

        print_success(f"Category {id} deleted successfully")

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)
