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
        return "1"
    if type_str.lower() in {"income", "credit", "cr", "c"}:
        return "2"
    if type_str.lower() in {"transfer", "tr", "t"}:
        return "3"
    return type_str


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
def add_expense(
    amount: float = typer.Option(..., "--amount", "-a", help="Amount"),
    name: str = typer.Option(..., "--name", "-n", help="Expense name"),
    currency: str = typer.Option(..., "--currency", help="Currency code"),
    account_id: Optional[int] = typer.Option(None, "--account-id", help="Account ID"),
    account_name: Optional[str] = typer.Option(None, "--account-name", help="Account name"),
    category_id: Optional[int] = typer.Option(None, "--category-id", help="Category ID"),
    category_name: Optional[str] = typer.Option(None, "--category-name", help="Category name"),
    comments: Optional[str] = typer.Option(None, "--comments", "-c", help="Comments"),
    date: Optional[str] = typer.Option(None, "--date", help="Date (YYYY-MM-DD)"),
    exchange_rate: Optional[float] = typer.Option(None, "--exchange-rate", help="Exchange rate"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Create a new expense."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with make_api_client(base_url, token) as api_client:
            # Resolve account
            if not account_id and not account_name:
                print_error("Either --account-id or --account-name is required")
                raise typer.Exit(1)
            if account_name:
                accounts_api = tracko_sdk.AccountsApi(api_client)
                accounts = sdk_call_unwrapped(lambda: accounts_api.get_all6()) or []
                account = next((a for a in accounts if a.name.lower() == account_name.lower()), None)
                if not account:
                    print_error(f"Account '{account_name}' not found")
                    raise typer.Exit(1)
                account_id = account.id
            
            # Resolve category
            if not category_id and not category_name:
                print_error("Either --category-id or --category-name is required")
                raise typer.Exit(1)
            if category_name:
                categories_api = tracko_sdk.CategoriesApi(api_client)
                categories = sdk_call_unwrapped(lambda: categories_api.get_all_categories())
                category = next((c for c in categories if c.name.lower() == category_name.lower()), None)
                if not category:
                    print_error(f"Category '{category_name}' not found")
                    raise typer.Exit(1)
                category_id = category.id
            
            txn_date = datetime.strptime(date, "%Y-%m-%d") if date else datetime.now()
            
            req = TransactionRequest(
                account_id=account_id,
                category_id=category_id,
                original_amount=amount,
                transaction_type="1",
                name=name,
                comments=comments,
                date=txn_date,
                original_currency=currency,
                exchange_rate=exchange_rate
            )
        
        with spinner(f"Creating expense '{name}'..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.TransactionsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.create1(req))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            print_success(f"Expense '{name}' created successfully")
            print_json(result)
    except Exception as e:
        print_error(f"Failed to create expense: {e}")
        raise typer.Exit(1)


@app.command()
def add_income(
    amount: float = typer.Option(..., "--amount", "-a", help="Amount"),
    name: str = typer.Option(..., "--name", "-n", help="Income name"),
    currency: str = typer.Option(..., "--currency", help="Currency code"),
    account_id: Optional[int] = typer.Option(None, "--account-id", help="Account ID"),
    account_name: Optional[str] = typer.Option(None, "--account-name", help="Account name"),
    category_id: Optional[int] = typer.Option(None, "--category-id", help="Category ID"),
    category_name: Optional[str] = typer.Option(None, "--category-name", help="Category name"),
    comments: Optional[str] = typer.Option(None, "--comments", "-c", help="Comments"),
    date: Optional[str] = typer.Option(None, "--date", help="Date (YYYY-MM-DD)"),
    exchange_rate: Optional[float] = typer.Option(None, "--exchange-rate", help="Exchange rate"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Create a new income."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with make_api_client(base_url, token) as api_client:
            # Resolve account
            if not account_id and not account_name:
                print_error("Either --account-id or --account-name is required")
                raise typer.Exit(1)
            if account_name:
                accounts_api = tracko_sdk.AccountsApi(api_client)
                accounts = sdk_call_unwrapped(lambda: accounts_api.get_all6()) or []
                account = next((a for a in accounts if a.name.lower() == account_name.lower()), None)
                if not account:
                    print_error(f"Account '{account_name}' not found")
                    raise typer.Exit(1)
                account_id = account.id
            
            # Resolve category
            if not category_id and not category_name:
                print_error("Either --category-id or --category-name is required")
                raise typer.Exit(1)
            if category_name:
                categories_api = tracko_sdk.CategoriesApi(api_client)
                categories = sdk_call_unwrapped(lambda: categories_api.get_all_categories())
                category = next((c for c in categories if c.name.lower() == category_name.lower()), None)
                if not category:
                    print_error(f"Category '{category_name}' not found")
                    raise typer.Exit(1)
                category_id = category.id
            
            txn_date = datetime.strptime(date, "%Y-%m-%d") if date else datetime.now()
            
            req = TransactionRequest(
                account_id=account_id,
                category_id=category_id,
                original_amount=amount,
                transaction_type="2",
                name=name,
                comments=comments,
                date=txn_date,
                original_currency=currency,
                exchange_rate=exchange_rate
            )
        
        with spinner(f"Creating income '{name}'..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.TransactionsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.create1(req))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            print_success(f"Income '{name}' created successfully")
            print_json(result)
    except Exception as e:
        print_error(f"Failed to create income: {e}")
        raise typer.Exit(1)


@app.command()
def add_transfer(
    amount: float = typer.Option(..., "--amount", "-a", help="Transfer amount"),
    currency: str = typer.Option(..., "--currency", help="Currency code"),
    from_account_id: Optional[int] = typer.Option(None, "--from-account-id", help="Source account ID"),
    from_account_name: Optional[str] = typer.Option(None, "--from-account-name", help="Source account name"),
    to_account_id: Optional[int] = typer.Option(None, "--to-account-id", help="Destination account ID"),
    to_account_name: Optional[str] = typer.Option(None, "--to-account-name", help="Destination account name"),
    name: Optional[str] = typer.Option(None, "--name", "-n", help="Transfer description"),
    comments: Optional[str] = typer.Option(None, "--comments", "-c", help="Comments"),
    date: Optional[str] = typer.Option(None, "--date", help="Date (YYYY-MM-DD)"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Create a transfer between accounts."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        with make_api_client(base_url, token) as api_client:
            # Resolve from account
            if not from_account_id and not from_account_name:
                print_error("Either --from-account-id or --from-account-name is required")
                raise typer.Exit(1)
            if from_account_name:
                accounts_api = tracko_sdk.AccountsApi(api_client)
                accounts = sdk_call_unwrapped(lambda: accounts_api.get_all6()) or []
                account = next((a for a in accounts if a.name.lower() == from_account_name.lower()), None)
                if not account:
                    print_error(f"Account '{from_account_name}' not found")
                    raise typer.Exit(1)
                from_account_id = account.id
            
            # Resolve to account
            if not to_account_id and not to_account_name:
                print_error("Either --to-account-id or --to-account-name is required")
                raise typer.Exit(1)
            if to_account_name:
                accounts_api = tracko_sdk.AccountsApi(api_client)
                accounts = sdk_call_unwrapped(lambda: accounts_api.get_all6()) or []
                account = next((a for a in accounts if a.name.lower() == to_account_name.lower()), None)
                if not account:
                    print_error(f"Account '{to_account_name}' not found")
                    raise typer.Exit(1)
                to_account_id = account.id
            
            if from_account_id == to_account_id:
                print_error("Source and destination accounts must be different")
                raise typer.Exit(1)
            
            txn_date = datetime.strptime(date, "%Y-%m-%d") if date else datetime.now()
            
            req = TransactionRequest(
                account_id=from_account_id,
                to_account_id=to_account_id,
                original_amount=amount,
                transaction_type="3",
                name=name or f"Transfer to account {to_account_id}",
                comments=comments,
                date=txn_date,
                original_currency=currency
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
def update_expense(
    id: int = typer.Argument(..., help="Transaction ID"),
    account_id: Optional[int] = typer.Option(None, "--account-id", help="Account ID"),
    category_id: Optional[int] = typer.Option(None, "--category-id", help="Category ID"),
    amount: Optional[float] = typer.Option(None, "--amount", "-a", help="Amount"),
    name: Optional[str] = typer.Option(None, "--name", "-n", help="Expense name"),
    comments: Optional[str] = typer.Option(None, "--comments", "-c", help="Comments"),
    date: Optional[str] = typer.Option(None, "--date", help="Date (YYYY-MM-DD)"),
    currency: Optional[str] = typer.Option(None, "--currency", help="Currency code"),
    exchange_rate: Optional[float] = typer.Option(None, "--exchange-rate", help="Exchange rate"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Update an expense."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        txn_date = datetime.strptime(date, "%Y-%m-%d") if date else None
        
        req = TransactionRequest(
            account_id=account_id,
            category_id=category_id,
            amount=amount,
            transaction_type="1",
            name=name,
            comments=comments,
            date=txn_date,
            original_currency=currency,
            exchange_rate=exchange_rate
        )
        
        with spinner(f"Updating expense {id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.TransactionsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.update(id=id, transaction_request=req))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            print_success(f"Expense {id} updated successfully")
            print_json(result)
    except Exception as e:
        print_error(f"Failed to update expense: {e}")
        raise typer.Exit(1)


@app.command()
def update_income(
    id: int = typer.Argument(..., help="Transaction ID"),
    account_id: Optional[int] = typer.Option(None, "--account-id", help="Account ID"),
    category_id: Optional[int] = typer.Option(None, "--category-id", help="Category ID"),
    amount: Optional[float] = typer.Option(None, "--amount", "-a", help="Amount"),
    name: Optional[str] = typer.Option(None, "--name", "-n", help="Income name"),
    comments: Optional[str] = typer.Option(None, "--comments", "-c", help="Comments"),
    date: Optional[str] = typer.Option(None, "--date", help="Date (YYYY-MM-DD)"),
    currency: Optional[str] = typer.Option(None, "--currency", help="Currency code"),
    exchange_rate: Optional[float] = typer.Option(None, "--exchange-rate", help="Exchange rate"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Update an income."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        txn_date = datetime.strptime(date, "%Y-%m-%d") if date else None
        
        req = TransactionRequest(
            account_id=account_id,
            category_id=category_id,
            amount=amount,
            transaction_type="2",
            name=name,
            comments=comments,
            date=txn_date,
            original_currency=currency,
            exchange_rate=exchange_rate
        )
        
        with spinner(f"Updating income {id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.TransactionsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.update(id=id, transaction_request=req))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            print_success(f"Income {id} updated successfully")
            print_json(result)
    except Exception as e:
        print_error(f"Failed to update income: {e}")
        raise typer.Exit(1)


@app.command()
def update_transfer(
    id: int = typer.Argument(..., help="Transaction ID"),
    from_account_id: Optional[int] = typer.Option(None, "--from-account-id", help="Source account ID"),
    to_account_id: Optional[int] = typer.Option(None, "--to-account-id", help="Destination account ID"),
    amount: Optional[float] = typer.Option(None, "--amount", "-a", help="Transfer amount"),
    name: Optional[str] = typer.Option(None, "--name", "-n", help="Transfer description"),
    comments: Optional[str] = typer.Option(None, "--comments", "-c", help="Comments"),
    date: Optional[str] = typer.Option(None, "--date", help="Date (YYYY-MM-DD)"),
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Update a transfer."""
    config = get_active_profile_config()
    token, base_url = config.get("token"), config.get("base_url", "http://localhost:8080")
    
    try:
        txn_date = datetime.strptime(date, "%Y-%m-%d") if date else None
        
        req = TransactionRequest(
            account_id=from_account_id,
            to_account_id=to_account_id,
            amount=amount,
            transaction_type="3",
            name=name,
            comments=comments,
            date=txn_date
        )
        
        with spinner(f"Updating transfer {id}..."):
            with make_api_client(base_url, token) as api_client:
                api = tracko_sdk.TransactionsApi(api_client)
                result = sdk_call_unwrapped(lambda: api.update(id=id, transaction_request=req))
        
        if result is None:
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            print_success(f"Transfer {id} updated successfully")
            print_json(result)
    except Exception as e:
        print_error(f"Failed to update transfer: {e}")
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



