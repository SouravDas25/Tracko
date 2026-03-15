"""Authentication commands."""
import typer

from ..core.config import (
    config_path,
    get_active_profile_config,
    get_active_profile_name,
    update_profile,
)
from ..core.api import make_api_client, sdk_call
from ..core.output import print_success, print_error, spinner
from ..utils.prompts import prompt

import tracko_sdk
from tracko_sdk.models.login_request import LoginRequest


app = typer.Typer(help="Authentication commands")


@app.command()
def login(
    username: str = typer.Option(..., "--username", "-u", prompt=True, help="Username or email"),
    password: str = typer.Option(None, "--password", "-p", help="Password (prompted if not provided)"),
):
    """Login to Tracko API."""
    if not password:
        password = prompt("Password", password=True)
    
    config = get_active_profile_config()
    base_url = config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner("Logging in..."):
            with make_api_client(base_url) as api_client:
                api = tracko_sdk.AuthenticationApi(api_client)
                result = sdk_call(
                    lambda: api.login(LoginRequest(username=username, password=password)),
                    auth_call=True
                )
        
        if result is None:
            print_error("Login failed. Check your credentials.")
            raise typer.Exit(1)
        
        token = result.get("token") if isinstance(result, dict) else getattr(result, "token", None)
        
        if token:
            active_profile = get_active_profile_name()
            update_profile(active_profile, {"base_url": base_url, "token": token})
            print_success(f"Logged in successfully as {username}")
            print_success(f"Token saved to profile '{active_profile}' in {config_path()}")
        else:
            print_error("Login failed: No token received")
            raise typer.Exit(1)
    
    except Exception as e:
        print_error(f"Login failed: {e}")
        raise typer.Exit(1)


@app.command()
def logout():
    """Logout and clear saved token."""
    active_profile = get_active_profile_name()
    update_profile(active_profile, {"token": None})
    print_success(f"Logged out from profile '{active_profile}'")
