"""Tracko CLI - Typer-based command-line interface."""
import typer
from datetime import datetime, timedelta, timezone
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
    update,
)

app = typer.Typer(
    name="trako",
    help="Trako CLI - Expense management tool",
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
app.add_typer(update.app, name="update")


def _check_for_updates_on_startup():
    """Check for updates once per day; print notice if available."""
    try:
        from .core.config import get_update_setting, set_update_setting

        if not get_update_setting("auto_update_check", True):
            return

        last_check = get_update_setting("last_update_check")
        if last_check:
            try:
                last_check_dt = datetime.fromisoformat(last_check)
                if datetime.now(timezone.utc) - last_check_dt < timedelta(hours=24):
                    return
            except (ValueError, TypeError):
                pass

        now_iso = datetime.now(timezone.utc).isoformat()
        set_update_setting("last_update_check", now_iso)

        from .core.updater import check_for_update, get_current_version

        is_available, latest_version = check_for_update()
        if is_available:
            current = get_current_version()
            # Write to stderr so the notice never corrupts piped/JSON/completion stdout.
            from rich.console import Console
            err_console = Console(stderr=True, log_time=False)
            err_console.print(
                f"\n[bold yellow]Update available:[/bold yellow] {current} → {latest_version}"
            )
            err_console.print("[dim]Run [bold]trako update apply[/bold] to upgrade.[/dim]\n")
    except Exception:
        pass


def main():
    """Entry point for the CLI."""
    _check_for_updates_on_startup()
    app()


if __name__ == "__main__":
    main()
