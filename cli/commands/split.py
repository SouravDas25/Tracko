"""Split transaction management commands."""
import typer
from typing import Optional

from ..core.config import get_active_profile_config
from ..core.api import make_api_client, sdk_call_unwrapped
from ..core.output import console, create_table, print_json, print_success, print_error, spinner
from ..utils.prompts import confirm

import tracko_sdk
from tracko_sdk.models.split import Split


app = typer.Typer(help="Split transaction management")


@app.command()
def list(raw: bool = typer.Option(False, "--raw", help="Output raw JSON")):
    """List all splits."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner("Fetching splits..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.SplitsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_all2())
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            splits = result if result else []
            if splits:
                table = create_table(title="Splits")
                table.add_column("ID", justify="right", style="cyan")
                table.add_column("Transaction", justify="right", style="yellow")
                table.add_column("Amount", justify="right", style="green")
                table.add_column("Contact", style="blue")
                table.add_column("Settled", justify="center", style="magenta")
                
                for split in splits:
                    split_dict = split.to_dict() if hasattr(split, "to_dict") else split
                    table.add_row(
                        str(split_dict.get("id", "")),
                        str(split_dict.get("transactionId", "")),
                        f"{float(split_dict.get('amount', 0)):.2f}",
                        str(split_dict.get("contactId", "")),
                        "✓" if split_dict.get("isSettled") else "✗"
                    )
                console.print(table)
            else:
                print_error("No splits found")
    except Exception as e:
        print_error(f"Failed to list splits: {e}")
        raise typer.Exit(1)


@app.command()
def get(
    id: int = typer.Argument(..., help="Split ID"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get split by ID."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner(f"Fetching split {id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.SplitsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_by_id1(id=id))
        
        if result is None:
            raise typer.Exit(1)
        
        print_json(result)
    except Exception as e:
        print_error(f"Failed to get split: {e}")
        raise typer.Exit(1)


@app.command()
def create(
    transaction_id: int = typer.Option(..., "--transaction-id", help="Transaction ID"),
    user_id: str = typer.Option(..., "--user-id", help="User ID"),
    amount: float = typer.Option(..., "--amount", "-a", help="Split amount"),
    contact_id: Optional[int] = typer.Option(None, "--contact-id", help="Contact ID"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Create a new split."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        split = Split(
            transaction_id=transaction_id,
            user_id=user_id,
            amount=amount,
            contact_id=contact_id
        )
        
        with spinner(f"Creating split for transaction {transaction_id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.SplitsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.create2(split))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            print_success(f"Split created for transaction {transaction_id}")
            print_json(result)
    except Exception as e:
        print_error(f"Failed to create split: {e}")
        raise typer.Exit(1)


@app.command()
def delete(
    id: int = typer.Argument(..., help="Split ID"),
):
    """Delete a split."""
    if not confirm(f"Delete split {id}?", default=False):
        print_error("Cancelled")
        raise typer.Exit(1)
    
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner(f"Deleting split {id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.SplitsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.delete2(id=id))
        
        print_success(f"Split {id} deleted successfully")
    except Exception as e:
        print_error(f"Failed to delete split: {e}")
        raise typer.Exit(1)


@app.command()
def settle(
    id: int = typer.Argument(..., help="Split ID"),
):
    """Mark a split as settled."""
    if not confirm(f"Mark split {id} as settled?", default=True):
        print_error("Cancelled")
        raise typer.Exit(1)
    
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner(f"Settling split {id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.SplitsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.settle(id=id))
        
        print_success(f"Split {id} marked as settled")
    except Exception as e:
        print_error(f"Failed to settle split: {e}")
        raise typer.Exit(1)


@app.command()
def unsettle(
    id: int = typer.Argument(..., help="Split ID"),
):
    """Mark a split as unsettled."""
    if not confirm(f"Mark split {id} as unsettled?", default=True):
        print_error("Cancelled")
        raise typer.Exit(1)
    
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner(f"Unsettling split {id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.SplitsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.unsettle(id=id))
        
        print_success(f"Split {id} marked as unsettled")
    except Exception as e:
        print_error(f"Failed to unsettle split: {e}")
        raise typer.Exit(1)


@app.command()
def for_transaction(
    transaction_id: int = typer.Argument(..., help="Transaction ID"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get splits for a transaction."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner(f"Fetching splits for transaction {transaction_id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.SplitsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_by_transaction_id(transaction_id=transaction_id))
        
        if result is None:
            raise typer.Exit(1)
        
        print_json(result)
    except Exception as e:
        print_error(f"Failed to get splits: {e}")
        raise typer.Exit(1)


@app.command()
def for_contact(
    contact_id: int = typer.Argument(..., help="Contact ID"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get splits for a contact."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner(f"Fetching splits for contact {contact_id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.SplitsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_by_contact_id(contact_id=contact_id))
        
        if result is None:
            raise typer.Exit(1)
        
        print_json(result)
    except Exception as e:
        print_error(f"Failed to get splits: {e}")
        raise typer.Exit(1)


@app.command()
def unsettled(raw: bool = typer.Option(False, "--raw", help="Output raw JSON")):
    """Get all unsettled splits for current user."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner("Fetching unsettled splits..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.SplitsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_my_unsettled())
        
        if result is None:
            raise typer.Exit(1)
        
        print_json(result)
    except Exception as e:
        print_error(f"Failed to get unsettled splits: {e}")
        raise typer.Exit(1)


@app.command()
def unsettled_contact(
    contact_id: int = typer.Argument(..., help="Contact ID"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get unsettled splits for a contact."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner(f"Fetching unsettled splits for contact {contact_id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.SplitsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_unsettled_by_contact_id(contact_id=contact_id))
        
        if result is None:
            raise typer.Exit(1)
        
        print_json(result)
    except Exception as e:
        print_error(f"Failed to get unsettled splits: {e}")
        raise typer.Exit(1)
