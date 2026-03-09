"""Transaction management commands."""
import typer
from typing import Optional
from datetime import datetime
import csv
from rich.progress import Progress, BarColumn, TextColumn, TimeRemainingColumn

from ..core.config import get_active_profile_config
from ..core.api import make_api_client, sdk_call_unwrapped
from ..core.output import console, create_table, print_json, print_success, print_error, spinner
from ..utils.prompts import confirm

import tracko_sdk
from tracko_sdk.models.transaction_request import TransactionRequest


app = typer.Typer(help="Transaction management")


def _parse_type(type_str: str) -> str:
    """Map type to API enum."""
    if type_str.lower() in {"expense", "debit", "dr", "d"}:
        return "EXPENSE"
    if type_str.lower() in {"income", "credit", "cr", "c"}:
        return "INCOME"
    return type_str.upper()


@app.command()
def list(
    month: Optional[int] = typer.Option(None, "--month", help="Month (1-12)"),
    year: Optional[int] = typer.Option(None, "--year", help="Year"),
    page: Optional[int] = typer.Option(None, "--page", help="Page number"),
    size: Optional[int] = typer.Option(None, "--size", help="Page size"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """List transactions."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner("Fetching transactions..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.TransactionsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_all1(
                    month=month, year=year, page=page, size=size
                ))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            result_dict = result.to_dict() if hasattr(result, "to_dict") else result
            transactions = result_dict.get("transactions", [])
            
            if transactions:
                table = create_table(title="Transactions")
                table.add_column("ID", justify="right", style="cyan")
                table.add_column("Date", style="yellow")
                table.add_column("Name", style="green")
                table.add_column("Amount", justify="right", style="magenta")
                table.add_column("Type", style="blue")
                
                for txn in transactions:
                    table.add_row(
                        str(txn.get("id", "")),
                        str(txn.get("date", ""))[:10] if txn.get("date") else "",
                        str(txn.get("name", "")),
                        f"{float(txn.get('amount', 0)):.2f}",
                        str(txn.get("transactionType", ""))
                    )
                console.print(table)
                page = result_dict.get('page', 0)
                total_pages = result_dict.get('totalPages', 0)
                console.print(f"\n[dim]Page {page} of {total_pages}[/dim]")
            else:
                print_error("No transactions found")
    except Exception as e:
        print_error(f"Failed to list transactions: {e}")
        raise typer.Exit(1)


@app.command()
def add(
    account_id: Optional[int] = typer.Option(None, "--account-id", help="Account ID"),
    category_id: Optional[int] = typer.Option(None, "--category-id", help="Category ID"),
    amount: float = typer.Option(..., "--amount", "-a", help="Amount"),
    type: str = typer.Option(..., "--type", "-t", help="Type (income/expense)"),
    name: str = typer.Option(..., "--name", "-n", help="Transaction name"),
    comments: Optional[str] = typer.Option(None, "--comments", "-c", help="Comments"),
    date: Optional[str] = typer.Option(None, "--date", help="Date (YYYY-MM-DD)"),
    currency: Optional[str] = typer.Option(None, "--currency", help="Currency code"),
    exchange_rate: Optional[float] = typer.Option(None, "--exchange-rate", help="Exchange rate"),
    countable: Optional[bool] = typer.Option(None, "--countable/--not-countable", help="Is countable"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Create a new transaction."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        txn_date = datetime.strptime(date, "%Y-%m-%d") if date else datetime.now()
        txn_type = _parse_type(type)
        
        req = TransactionRequest(
            account_id=account_id,
            category_id=category_id,
            amount=amount,
            transaction_type=txn_type,
            name=name,
            comments=comments,
            date=txn_date,
            original_currency=currency,
            exchange_rate=exchange_rate,
            is_countable=countable
        )
        
        with spinner(f"Creating transaction '{name}'..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.TransactionsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.create1(req))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            print_success(f"Transaction '{name}' created successfully")
            print_json(result)
    except Exception as e:
        print_error(f"Failed to create transaction: {e}")
        raise typer.Exit(1)


@app.command()
def get(
    id: int = typer.Argument(..., help="Transaction ID"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get transaction by ID."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner(f"Fetching transaction {id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.TransactionsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_by_id(id=id))
        
        if result is None:
            raise typer.Exit(1)
        
        print_json(result)
    except Exception as e:
        print_error(f"Failed to get transaction: {e}")
        raise typer.Exit(1)


@app.command()
def update(
    id: int = typer.Argument(..., help="Transaction ID"),
    account_id: Optional[int] = typer.Option(None, "--account-id", help="Account ID"),
    category_id: Optional[int] = typer.Option(None, "--category-id", help="Category ID"),
    amount: Optional[float] = typer.Option(None, "--amount", "-a", help="Amount"),
    type: Optional[str] = typer.Option(None, "--type", "-t", help="Type (income/expense)"),
    name: Optional[str] = typer.Option(None, "--name", "-n", help="Transaction name"),
    comments: Optional[str] = typer.Option(None, "--comments", "-c", help="Comments"),
    date: Optional[str] = typer.Option(None, "--date", help="Date (YYYY-MM-DD)"),
    currency: Optional[str] = typer.Option(None, "--currency", help="Currency code"),
    exchange_rate: Optional[float] = typer.Option(None, "--exchange-rate", help="Exchange rate"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Update a transaction."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        txn_date = datetime.strptime(date, "%Y-%m-%d") if date else None
        txn_type = _parse_type(type) if type else None
        
        req = TransactionRequest(
            account_id=account_id,
            category_id=category_id,
            amount=amount,
            transaction_type=txn_type,
            name=name,
            comments=comments,
            date=txn_date,
            original_currency=currency,
            exchange_rate=exchange_rate
        )
        
        with spinner(f"Updating transaction {id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.TransactionsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.update(id=id, transaction_request=req))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            print_success(f"Transaction {id} updated successfully")
            print_json(result)
    except Exception as e:
        print_error(f"Failed to update transaction: {e}")
        raise typer.Exit(1)


@app.command()
def delete(
    id: int = typer.Argument(..., help="Transaction ID"),
):
    """Delete a transaction."""
    if not confirm(f"Delete transaction {id}?", default=False):
        print_error("Cancelled")
        raise typer.Exit(1)
    
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner(f"Deleting transaction {id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.TransactionsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.delete1(id=id))
        
        print_success(f"Transaction {id} deleted successfully")
    except Exception as e:
        print_error(f"Failed to delete transaction: {e}")
        raise typer.Exit(1)


@app.command()
def summary(
    start_date: Optional[str] = typer.Option(None, "--start-date", help="Start date (YYYY-MM-DD)"),
    end_date: Optional[str] = typer.Option(None, "--end-date", help="End date (YYYY-MM-DD)"),
    account_ids: Optional[str] = typer.Option(None, "--account-ids", help="Comma-separated account IDs"),
    include_rollover: bool = typer.Option(False, "--include-rollover", help="Include rollover"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get transaction summary."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        start = datetime.strptime(start_date, "%Y-%m-%d") if start_date else None
        end = datetime.strptime(end_date, "%Y-%m-%d") if end_date else None
        
        with spinner("Fetching summary..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.TransactionsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_my_summary(
                    start_date=start,
                    end_date=end,
                    account_ids=account_ids,
                    include_rollover=include_rollover if include_rollover else None
                ))
        
        if result is None:
            raise typer.Exit(1)
        
        print_json(result)
    except Exception as e:
        print_error(f"Failed to get summary: {e}")
        raise typer.Exit(1)


@app.command()
def total_income(
    start_date: str = typer.Option(..., "--start-date", help="Start date (YYYY-MM-DD)"),
    end_date: str = typer.Option(..., "--end-date", help="End date (YYYY-MM-DD)"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get total income."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")
        
        with spinner("Calculating total income..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.TransactionsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_my_total_income(
                    start_date=start, end_date=end
                ))
        
        if result is None:
            raise typer.Exit(1)
        
        print_json(result)
    except Exception as e:
        print_error(f"Failed to get total income: {e}")
        raise typer.Exit(1)


@app.command()
def total_expense(
    start_date: str = typer.Option(..., "--start-date", help="Start date (YYYY-MM-DD)"),
    end_date: str = typer.Option(..., "--end-date", help="End date (YYYY-MM-DD)"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Get total expense."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        start = datetime.strptime(start_date, "%Y-%m-%d")
        end = datetime.strptime(end_date, "%Y-%m-%d")
        
        with spinner("Calculating total expense..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.TransactionsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.get_my_total_expense(
                    start_date=start, end_date=end
                ))
        
        if result is None:
            raise typer.Exit(1)
        
        print_json(result)
    except Exception as e:
        print_error(f"Failed to get total expense: {e}")
        raise typer.Exit(1)


@app.command()
def import_csv(
    file: str = typer.Option(..., "--file", "-f", help="CSV file path"),
    account_id: int = typer.Option(..., "--account-id", help="Account ID"),
):
    """Import transactions from CSV file."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with open(file, 'r') as f:
            reader = csv.DictReader(f)
            rows = list(reader)
        
        with Progress(
            TextColumn("[progress.description]{task.description}"),
            BarColumn(),
            TextColumn("[progress.percentage]{task.percentage:>3.0f}%"),
            TimeRemainingColumn(),
            console=console
        ) as progress:
            task = progress.add_task(f"Importing {len(rows)} transactions...", total=len(rows))
            
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.TransactionsApi(api_client)
                
                for row in rows:
                    req = TransactionRequest(
                        account_id=account_id,
                        amount=float(row.get('amount', 0)),
                        transaction_type=_parse_type(row.get('type', 'expense')),
                        name=row.get('name', ''),
                        comments=row.get('comments'),
                        date=datetime.strptime(row['date'], "%Y-%m-%d") if row.get('date') else datetime.now()
                    )
                    sdk_call_unwrapped(lambda: api.create1(req))
                    progress.update(task, advance=1)
        
        print_success(f"Imported {len(rows)} transactions successfully")
    except Exception as e:
        print_error(f"Failed to import CSV: {e}")
        raise typer.Exit(1)


@app.command()
def transfer(
    from_account_id: int = typer.Option(..., "--from-account-id", help="Source account ID"),
    to_account_id: int = typer.Option(..., "--to-account-id", help="Destination account ID"),
    amount: float = typer.Option(..., "--amount", "-a", help="Transfer amount"),
    name: Optional[str] = typer.Option(None, "--name", "-n", help="Transfer description"),
    comments: Optional[str] = typer.Option(None, "--comments", "-c", help="Comments"),
    date: Optional[str] = typer.Option(None, "--date", help="Date (YYYY-MM-DD)"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Create a transfer between accounts."""
    if from_account_id == to_account_id:
        print_error("Source and destination accounts must be different")
        raise typer.Exit(1)
    
    if not confirm(f"Transfer {amount} from account {from_account_id} to {to_account_id}?", default=True):
        print_error("Cancelled")
        raise typer.Exit(1)
    
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        txn_date = datetime.strptime(date, "%Y-%m-%d") if date else datetime.now()
        
        req = TransactionRequest(
            account_id=from_account_id,
            to_account_id=to_account_id,
            amount=amount,
            transaction_type="TRANSFER",
            name=name or f"Transfer to account {to_account_id}",
            comments=comments,
            date=txn_date
        )
        
        with spinner(f"Creating transfer..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.TransactionsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.create1(req))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            print_success(f"Transfer of {amount} created successfully")
            print_json(result)
    except Exception as e:
        print_error(f"Failed to create transfer: {e}")
        raise typer.Exit(1)
