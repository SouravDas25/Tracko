"""Contact management commands."""
import sys

import typer
from typing import Optional
from tracko_sdk.models.contact_save_request import ContactSaveRequest
from tracko_sdk.rest import ApiException
from urllib3.exceptions import MaxRetryError, NewConnectionError

from ..core.api import get_config_for_api, get_api_client, unwrap_envelope, handle_api_error
from ..core.output import console, create_table, print_json, print_success, print_error, spinner
from ..utils.prompts import confirm

import tracko_sdk


app = typer.Typer(help="Contact management")


@app.command()
def list(raw: bool = typer.Option(False, "--raw", help="Output raw JSON")):
    """List all contacts."""
    base_url, token = get_config_for_api()

    try:
        with spinner("Fetching contacts..."):
            with get_api_client(base_url, token) as client:
                contacts = unwrap_envelope(tracko_sdk.ContactsApi(client).list_mine()) or []

        if raw:
            print_json([c.model_dump(by_alias=True) for c in contacts])
        elif contacts:
            table = create_table(title="Contacts")
            table.add_column("ID", justify="right", style="cyan")
            table.add_column("Name", style="green")
            table.add_column("Phone", style="yellow")
            table.add_column("Email", style="blue")

            for contact in contacts:
                table.add_row(
                    str(contact.id),
                    str(contact.name),
                    str(contact.phone_no or ""),
                    str(contact.email or ""),
                )
            console.print(table)
        else:
            print_error("No contacts found")

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def add(
    name: str = typer.Option(..., "--name", "-n", help="Contact name"),
    phone: Optional[str] = typer.Option(None, "--phone", "-p", help="Phone number"),
    email: Optional[str] = typer.Option(None, "--email", "-e", help="Email address"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Create a new contact."""
    base_url, token = get_config_for_api()

    try:
        req = ContactSaveRequest(name=name, phone_no=phone, email=email)
        with spinner(f"Creating contact '{name}'..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.ContactsApi(client).create5(req))

        if raw:
            print_json(result)
        else:
            print_success(f"Contact '{name}' created successfully")
            print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def get(
    id: int = typer.Argument(..., help="Contact ID"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get contact by ID."""
    base_url, token = get_config_for_api()

    try:
        with spinner(f"Fetching contact {id}..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.ContactsApi(client).get_one(id=id))

        print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def update(
    id: int = typer.Argument(..., help="Contact ID"),
    name: str = typer.Option(..., "--name", "-n", help="Contact name"),
    phone: Optional[str] = typer.Option(None, "--phone", "-p", help="Phone number"),
    email: Optional[str] = typer.Option(None, "--email", "-e", help="Email address"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Update a contact."""
    base_url, token = get_config_for_api()

    try:
        req = ContactSaveRequest(name=name, phone_no=phone, email=email)
        with spinner(f"Updating contact {id}..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.ContactsApi(client).update3(id=id, contact_save_request=req))

        if raw:
            print_json(result)
        else:
            print_success(f"Contact {id} updated successfully")
            print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def delete(
    id: int = typer.Argument(..., help="Contact ID"),
):
    """Delete a contact."""
    if not confirm(f"Delete contact {id}?", default=False):
        print_error("Cancelled")
        raise typer.Exit(1)

    base_url, token = get_config_for_api()

    try:
        with spinner(f"Deleting contact {id}..."):
            with get_api_client(base_url, token) as client:
                unwrap_envelope(tracko_sdk.ContactsApi(client).delete5(id=id))

        print_success(f"Contact {id} deleted successfully")

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)
