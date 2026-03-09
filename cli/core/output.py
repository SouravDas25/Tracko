"""Rich output helpers for CLI."""
import json
from typing import Any
from contextlib import contextmanager

from rich.console import Console
from rich.table import Table
from rich.progress import Progress, SpinnerColumn, TextColumn

console = Console()


def print_json(data: Any) -> None:
    """Print data as formatted JSON."""
    if hasattr(data, "to_dict"):
        console.print_json(json.dumps(data.to_dict(), default=str))
    else:
        console.print_json(json.dumps(data, default=str))


def print_success(message: str) -> None:
    """Print success message in green."""
    console.print(f"[green]✓[/green] {message}")


def print_error(message: str) -> None:
    """Print error message in red."""
    console.print(f"[red]✗[/red] {message}", style="bold red")


def print_warning(message: str) -> None:
    """Print warning message in yellow."""
    console.print(f"[yellow]⚠[/yellow] {message}")


def print_info(message: str) -> None:
    """Print info message in blue."""
    console.print(f"[blue]ℹ[/blue] {message}")


def create_table(title: str = None, **kwargs) -> Table:
    """Create a Rich table with default styling."""
    return Table(title=title, show_header=True, header_style="bold cyan", **kwargs)


@contextmanager
def spinner(text: str):
    """Context manager for spinner during operations."""
    with Progress(
        SpinnerColumn(),
        TextColumn("[progress.description]{task.description}"),
        console=console,
    ) as progress:
        progress.add_task(description=text, total=None)
        yield
