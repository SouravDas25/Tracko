"""Split transaction management commands."""
import sys

import typer
from typing import Optional
from tracko_sdk.rest import ApiException
from urllib3.exceptions import MaxRetryError, NewConnectionError

from ..core.api import get_config_for_api, get_api_client, unwrap_envelope, handle_api_error
from ..core.output import console, create_table, print_json, print_success, print_error, spinner
from ..utils.prompts import confirm

import tracko_sdk
from tracko_sdk.models.split import Split


app = typer.Typer(help="Split transaction management")


@app.command()
def list(raw: bool = typer.Option(False, "--raw", help="Output raw JSON")):
    """List all splits."""
    base_url, token = get_config_for_api()

    try:
        with spinner("Fetching splits..."):
            with get_api_client(base_url, token) as client:
                splits = unwrap_envelope(tracko_sdk.SplitsApi(client).get_all2()) or []

        if raw:
            print_json([s.model_dump(by_alias=True) for s in splits])
        elif splits:
            table = create_table(title="Splits")
            table.add_column("ID", justify="right", style="cyan")
            table.add_column("Transaction", justify="right", style="yellow")
            table.add_column("Amount", justify="right", style="green")
            table.add_column("Contact", style="blue")
            table.add_column("Settled", justify="center", style="magenta")

            for s in splits:
                table.add_row(
                    str(s.id or ""),
                    str(s.transaction_id or ""),
                    f"{float(s.amount or 0):.2f}",
                    str(s.contact_id or ""),
                    "✓" if s.is_settled else "✗"
                )
            console.print(table)
        else:
            print_error("No splits found")

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def get(
    id: int = typer.Argument(..., help="Split ID"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get split by ID."""
    base_url, token = get_config_for_api()

    try:
        with spinner(f"Fetching split {id}..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.SplitsApi(client).get_by_id1(id=id))

        print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def create(
    transaction_id: int = typer.Option(..., "--transaction-id", help="Transaction ID"),
    user_id: str = typer.Option(..., "--user-id", help="User ID"),
    amount: float = typer.Option(..., "--amount", "-a", help="Split amount"),
    contact_id: Optional[int] = typer.Option(None, "--contact-id", help="Contact ID"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Create a new split."""
    base_url, token = get_config_for_api()

    try:
        split = Split(
            transaction_id=transaction_id,
            user_id=user_id,
            amount=amount,
            contact_id=contact_id
        )

        with spinner(f"Creating split for transaction {transaction_id}..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.SplitsApi(client).create2(split))

        if raw:
            print_json(result)
        else:
            print_success(f"Split created for transaction {transaction_id}")
            print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def delete(
    id: int = typer.Argument(..., help="Split ID"),
):
    """Delete a split."""
    if not confirm(f"Delete split {id}?", default=False):
        print_error("Cancelled")
        raise typer.Exit(1)

    base_url, token = get_config_for_api()

    try:
        with spinner(f"Deleting split {id}..."):
            with get_api_client(base_url, token) as client:
                unwrap_envelope(tracko_sdk.SplitsApi(client).delete2(id=id))

        print_success(f"Split {id} deleted successfully")

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def settle(
    id: int = typer.Argument(..., help="Split ID"),
):
    """Mark a split as settled."""
    if not confirm(f"Mark split {id} as settled?", default=True):
        print_error("Cancelled")
        raise typer.Exit(1)

    base_url, token = get_config_for_api()

    try:
        with spinner(f"Settling split {id}..."):
            with get_api_client(base_url, token) as client:
                unwrap_envelope(tracko_sdk.SplitsApi(client).settle(split_id=id))

        print_success(f"Split {id} marked as settled")

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def unsettle(
    id: int = typer.Argument(..., help="Split ID"),
):
    """Mark a split as unsettled."""
    if not confirm(f"Mark split {id} as unsettled?", default=True):
        print_error("Cancelled")
        raise typer.Exit(1)

    base_url, token = get_config_for_api()

    try:
        with spinner(f"Unsettling split {id}..."):
            with get_api_client(base_url, token) as client:
                unwrap_envelope(tracko_sdk.SplitsApi(client).unsettle(split_id=id))

        print_success(f"Split {id} marked as unsettled")

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def for_transaction(
    transaction_id: int = typer.Argument(..., help="Transaction ID"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get splits for a transaction."""
    base_url, token = get_config_for_api()

    try:
        with spinner(f"Fetching splits for transaction {transaction_id}..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.SplitsApi(client).get_by_transaction_id(transaction_id=transaction_id))

        print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def for_contact(
    contact_id: int = typer.Argument(..., help="Contact ID"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get splits for a contact."""
    base_url, token = get_config_for_api()

    try:
        with spinner(f"Fetching splits for contact {contact_id}..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.SplitsApi(client).get_by_contact_id(contact_id=contact_id))

        print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def unsettled(raw: bool = typer.Option(False, "--raw", help="Output raw JSON")):
    """Get all unsettled splits for current user."""
    base_url, token = get_config_for_api()

    try:
        with spinner("Fetching unsettled splits..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.SplitsApi(client).get_my_unsettled())

        print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)


@app.command()
def unsettled_contact(
    contact_id: int = typer.Argument(..., help="Contact ID"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get unsettled splits for a contact."""
    base_url, token = get_config_for_api()

    try:
        with spinner(f"Fetching unsettled splits for contact {contact_id}..."):
            with get_api_client(base_url, token) as client:
                result = unwrap_envelope(tracko_sdk.SplitsApi(client).get_unsettled_by_contact_id(contact_id=contact_id))

        print_json(result)

    except ApiException as e:
        handle_api_error(e)
    except (ConnectionError, MaxRetryError, NewConnectionError, OSError):
        print_error("Could not connect to API. Is the server running?")
        sys.exit(1)
