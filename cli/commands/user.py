"""User management commands."""
import sys

import typer
from typing import Optional
from tracko_sdk.models.user_save_request import UserSaveRequest
from tracko_sdk.rest import ApiException
from urllib3.exceptions import MaxRetryError, NewConnectionError

from ..core.api import get_config_for_api, get_api_client, unwrap_envelope, handle_api_error
from ..core.output import console, create_table, print_json, print_success, print_error, spinner

import tracko_sdk


app = typer.Typer(help="User management")


@app.command()
def list(raw: bool = typer.Option(False, "--raw", help="Output raw JSON")):
    """List all users (admin only)."""
    base_url, token = get_config_for_api()

    try:
        with spinner("Fetching users..."):
            with get_api_client(base_url, token) as client:
                users = unwrap_envelope(tracko_sdk.UsersApi(client).show()) or []

        if raw:
            print_json([u.model_dump(by_alias=True) for u in users])
        elif users:
            table = create_table(title="Users")
            table.add_column("ID", justify="right", style="cyan")
            table.add_column("Name", style="green")
            table.add_column("Email", style="yellow")
            table.add_column("Phone", style="blue")

            for user in users:
                table.add_row(
                    str(user.id or ""),
                    str(user.name or ""),
                    str(user.email or ""),
                    str(user.phone_no or ""),
                )
            console.print(table)
        else:
            print_error("No users found")

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def me(raw: bool = typer.Option(False, "--raw", help="Output raw JSON")):
    """Get current user info."""
    base_url, token = get_config_for_api()

    try:
        with spinner("Fetching user info..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.UsersApi(client).me())

        if raw:
            print_json(result)
        else:
            console.print(f"[cyan]Name:[/cyan] {result.name or ''}")
            console.print(f"[cyan]Email:[/cyan] {result.email or ''}")
            console.print(f"[cyan]Phone:[/cyan] {result.phone_no or ''}")
            console.print(f"[cyan]Base Currency:[/cyan] {result.base_currency or ''}")

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def get(
    id: int = typer.Argument(..., help="User ID"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get user by ID."""
    base_url, token = get_config_for_api()

    try:
        with spinner(f"Fetching user {id}..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.UsersApi(client).show1(id=id))

        print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def find_phone(
    phone: str = typer.Argument(..., help="Phone number"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Find user by phone number."""
    base_url, token = get_config_for_api()

    try:
        with spinner(f"Searching for phone {phone}..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.UsersApi(client).show_by_phone(phone_no=phone))

        print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def upsert(
    name: str = typer.Option(..., "--name", "-n", help="User name"),
    email: Optional[str] = typer.Option(None, "--email", "-e", help="Email address"),
    phone: Optional[str] = typer.Option(None, "--phone", "-p", help="Phone number"),
    password: Optional[str] = typer.Option(None, "--password", help="Password"),
    base_currency: Optional[str] = typer.Option(None, "--base-currency", help="Base currency"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Create or update user."""
    base_url, token = get_config_for_api()

    try:
        req = UserSaveRequest(
            name=name,
            email=email,
            phone_no=phone,
            password=password,
            base_currency=base_currency,
        )
        with spinner(f"Upserting user '{name}'..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.UsersApi(client).create(req))

        if raw:
            print_json(result)
        else:
            print_success(f"User '{name}' upserted successfully")
            print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)
