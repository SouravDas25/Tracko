"""Authentication commands."""
import sys

import typer
from tracko_sdk.models.login_request import LoginRequest
from tracko_sdk.rest import ApiException
from urllib3.exceptions import MaxRetryError, NewConnectionError

from ..core.api import get_config_for_api, get_api_client, handle_api_error
from ..core.config import config_path, get_active_profile_name, update_profile
from ..core.output import print_success, print_error, spinner
from ..utils.prompts import prompt

import tracko_sdk


app = typer.Typer(help="Authentication commands")


@app.command()
def login(
    username: str = typer.Option(..., "--username", "-u", prompt=True, help="Username or email"),
    password: str = typer.Option(None, "--password", "-p", help="Password (prompted if not provided)"),
):
    """Login to Tracko API."""
    if not password:
        password = prompt("Password", password=True)

    base_url, _ = get_config_for_api(require_token=False)

    try:
        with spinner("Logging in..."):
            with get_api_client(base_url) as client:
                response = tracko_sdk.AuthenticationApi(client).login(
                    LoginRequest(username=username, password=password)
                )

        if not response or not response.token:
            print_error("Login failed: No token received")
            raise typer.Exit(1)

        active_profile = get_active_profile_name()
        update_profile(active_profile, {"base_url": base_url, "token": response.token})
        print_success(f"Logged in successfully as {username}")
        print_success(f"Token saved to profile '{active_profile}' in {config_path()}")

    except ApiException as e:
        handle_api_error(e, auth_call=True)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def logout():
    """Logout and clear saved token."""
    active_profile = get_active_profile_name()
    update_profile(active_profile, {"token": None})
    print_success(f"Logged out from profile '{active_profile}'")
