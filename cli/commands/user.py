"""User management commands."""
import typer
from typing import Optional

from ..core.config import get_active_profile_config
from ..core.api import make_api_client, sdk_call_unwrapped
from ..core.output import console, create_table, print_json, print_success, print_error, spinner

import tracko_sdk
from tracko_sdk.models.user_save_request import UserSaveRequest


app = typer.Typer(help="User management")


@app.command()
def list(raw: bool = typer.Option(False, "--raw", help="Output raw JSON")):
    """List all users (admin only)."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner("Fetching users..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.UsersApi(api_client)
                result = sdk_call_unwrapped(lambda: api.show())
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            users = result if result else []
            if users:
                table = create_table(title="Users")
                table.add_column("ID", justify="right", style="cyan")
                table.add_column("Name", style="green")
                table.add_column("Email", style="yellow")
                table.add_column("Phone", style="blue")
                
                for user in users:
                    user_dict = user.to_dict() if hasattr(user, "to_dict") else user
                    table.add_row(
                        str(user_dict.get("id", "")),
                        str(user_dict.get("name", "")),
                        str(user_dict.get("email", "")),
                        str(user_dict.get("phoneNo", ""))
                    )
                console.print(table)
            else:
                print_error("No users found")
    except Exception as e:
        print_error(f"Failed to list users: {e}")
        raise typer.Exit(1)


@app.command()
def me(raw: bool = typer.Option(False, "--raw", help="Output raw JSON")):
    """Get current user info."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner("Fetching user info..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.UsersApi(api_client)
                result = sdk_call_unwrapped(lambda: api.me())
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            user_dict = result.to_dict() if hasattr(result, "to_dict") else result
            console.print(f"[cyan]Name:[/cyan] {user_dict.get('name', '')}")
            console.print(f"[cyan]Email:[/cyan] {user_dict.get('email', '')}")
            console.print(f"[cyan]Phone:[/cyan] {user_dict.get('phoneNo', '')}")
            console.print(f"[cyan]Base Currency:[/cyan] {user_dict.get('baseCurrency', '')}")
    except Exception as e:
        print_error(f"Failed to get user info: {e}")
        raise typer.Exit(1)


@app.command()
def get(
    id: int = typer.Argument(..., help="User ID"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get user by ID."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner(f"Fetching user {id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.UsersApi(api_client)
                result = sdk_call_unwrapped(lambda: api.show1(id=id))
        
        if result is None:
            raise typer.Exit(1)
        
        print_json(result)
    except Exception as e:
        print_error(f"Failed to get user: {e}")
        raise typer.Exit(1)


@app.command()
def find_phone(
    phone: str = typer.Argument(..., help="Phone number"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Find user by phone number."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner(f"Searching for phone {phone}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.UsersApi(api_client)
                result = sdk_call_unwrapped(lambda: api.show_by_phone(phone_no=phone))
        
        if result is None:
            raise typer.Exit(1)
        
        print_json(result)
    except Exception as e:
        print_error(f"Failed to find user: {e}")
        raise typer.Exit(1)


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
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        req = UserSaveRequest(
            name=name,
            email=email,
            phone_no=phone,
            password=password,
            base_currency=base_currency
        )
        with spinner(f"Upserting user '{name}'..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.UsersApi(api_client)
                result = sdk_call_unwrapped(lambda: api.create(req))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            print_success(f"User '{name}' upserted successfully")
            print_json(result)
    except Exception as e:
        print_error(f"Failed to upsert user: {e}")
        raise typer.Exit(1)
