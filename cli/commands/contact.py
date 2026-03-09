"""Contact management commands."""
import typer
from typing import Optional

from ..core.config import get_active_profile_config
from ..core.api import make_api_client, sdk_call_unwrapped
from ..core.output import console, create_table, print_json, print_success, print_error, spinner
from ..utils.prompts import confirm

import tracko_sdk
from tracko_sdk.models.contact_save_request import ContactSaveRequest


app = typer.Typer(help="Contact management")


@app.command()
def list(raw: bool = typer.Option(False, "--raw", help="Output raw JSON")):
    """List all contacts."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner("Fetching contacts..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.ContactsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.list_mine())
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            contacts = result if result else []
            if contacts:
                table = create_table(title="Contacts")
                table.add_column("ID", justify="right", style="cyan")
                table.add_column("Name", style="green")
                table.add_column("Phone", style="yellow")
                table.add_column("Email", style="blue")
                
                for contact in contacts:
                    contact_dict = contact.to_dict() if hasattr(contact, "to_dict") else contact
                    table.add_row(
                        str(contact_dict.get("id", "")),
                        str(contact_dict.get("name", "")),
                        str(contact_dict.get("phoneNo", "")),
                        str(contact_dict.get("email", ""))
                    )
                console.print(table)
            else:
                print_error("No contacts found")
    except Exception as e:
        print_error(f"Failed to list contacts: {e}")
        raise typer.Exit(1)


@app.command()
def add(
    name: str = typer.Option(..., "--name", "-n", help="Contact name"),
    phone: Optional[str] = typer.Option(None, "--phone", "-p", help="Phone number"),
    email: Optional[str] = typer.Option(None, "--email", "-e", help="Email address"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Create a new contact."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        req = ContactSaveRequest(name=name, phone_no=phone, email=email)
        with spinner(f"Creating contact '{name}'..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.ContactsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.create5(req))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            print_success(f"Contact '{name}' created successfully")
            print_json(result)
    except Exception as e:
        print_error(f"Failed to create contact: {e}")
        raise typer.Exit(1)


@app.command()
def get(
    id: int = typer.Argument(..., help="Contact ID"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get contact by ID."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner(f"Fetching contact {id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.ContactsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_one(id=id))
        
        if result is None:
            raise typer.Exit(1)
        
        print_json(result)
    except Exception as e:
        print_error(f"Failed to get contact: {e}")
        raise typer.Exit(1)


@app.command()
def update(
    id: int = typer.Argument(..., help="Contact ID"),
    name: str = typer.Option(..., "--name", "-n", help="Contact name"),
    phone: Optional[str] = typer.Option(None, "--phone", "-p", help="Phone number"),
    email: Optional[str] = typer.Option(None, "--email", "-e", help="Email address"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Update a contact."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        req = ContactSaveRequest(name=name, phone_no=phone, email=email)
        with spinner(f"Updating contact {id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.ContactsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.update3(id=id, contact_save_request=req))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            print_success(f"Contact {id} updated successfully")
            print_json(result)
    except Exception as e:
        print_error(f"Failed to update contact: {e}")
        raise typer.Exit(1)


@app.command()
def delete(
    id: int = typer.Argument(..., help="Contact ID"),
):
    """Delete a contact."""
    if not confirm(f"Delete contact {id}?", default=False):
        print_error("Cancelled")
        raise typer.Exit(1)
    
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner(f"Deleting contact {id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.ContactsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.delete5(id=id))
        
        print_success(f"Contact {id} deleted successfully")
    except Exception as e:
        print_error(f"Failed to delete contact: {e}")
        raise typer.Exit(1)
