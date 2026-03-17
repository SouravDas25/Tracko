"""Tracko CLI - Typer-based command-line interface."""
import typer
from .commands import (
    health,
    auth,
    config,
    account,
    category,
    contact,
    user,
    transaction,
    budget,
    currency,
    split,
    stats,
    misc,
    db,
    completion,
)

app = typer.Typer(
    name="tracko",
    help="Tracko CLI - Expense management tool",
    no_args_is_help=True,
    add_completion=True,
)

# Phase 1: Foundation & Core Commands
app.add_typer(health.app, name="health")
app.add_typer(auth.app, name="auth")
app.add_typer(config.app, name="config")

# Phase 2: Resource Management Commands
app.add_typer(account.app, name="account")
app.add_typer(category.app, name="category")
app.add_typer(contact.app, name="contact")
app.add_typer(user.app, name="user")

# Phase 3: Transaction & Financial Commands
app.add_typer(transaction.app, name="transaction")
app.add_typer(budget.app, name="budget")
app.add_typer(currency.app, name="currency")

# Phase 4: Advanced Features
app.add_typer(split.app, name="split")
app.add_typer(stats.app, name="stats")
app.add_typer(misc.exchange_app, name="exchange")
app.add_typer(misc.store_app, name="store")
app.add_typer(db.app, name="db")

# Phase 5: Utilities
app.add_typer(completion.app, name="completion")


def main():
    """Entry point for the CLI."""
    app()


if __name__ == "__main__":
    main()
