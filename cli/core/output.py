"""Rich output helpers for CLI."""
import sys
import json
from typing import Any
from contextlib import contextmanager

from rich.console import Console
from rich.table import Table
from rich.progress import Progress, SpinnerColumn, TextColumn

# Force UTF-8 to prevent encoding errors on Windows
console = Console()

# Use ASCII icons on Windows to prevent encoding errors
IS_WINDOWS = sys.platform == "win32"
ICONS = {
    "success": "✓" if not IS_WINDOWS else "[OK]",
    "error": "✗" if not IS_WINDOWS else "[ERROR]",
    "warning": "⚠" if not IS_WINDOWS else "[WARN]",
    "info": "ℹ" if not IS_WINDOWS else "[INFO]",
}


def print_json(data: Any) -> None:
    """Print data as formatted JSON."""
    if hasattr(data, "model_dump"):
        console.print_json(json.dumps(data.model_dump(by_alias=True), default=str))
    else:
        console.print_json(json.dumps(data, default=str))


def print_success(message: str) -> None:
    """Print success message in green."""
    console.print(f"[green]{ICONS['success']}[/green] {message}")


def print_error(message: str) -> None:
    """Print error message in red."""
    console.print(f"[red]{ICONS['error']}[/red] {message}", style="bold red")


def print_warning(message: str) -> None:
    """Print warning message in yellow."""
    console.print(f"[yellow]{ICONS['warning']}[/yellow] {message}")


def print_info(message: str) -> None:
    """Print info message in blue."""
    console.print(f"[blue]{ICONS['info']}[/blue] {message}")


def create_table(title: str = None, **kwargs) -> Table:
    """Create a Rich table with default styling."""
    return Table(title=title, show_header=True, header_style="bold cyan", **kwargs)


@contextmanager
def spinner(text: str):
    """Context manager for spinner during operations."""
    # Use ASCII spinner on Windows
    spinner_style = "dots" if not IS_WINDOWS else "line"
    with Progress(
        SpinnerColumn(spinner_name=spinner_style),
        TextColumn("[progress.description]{task.description}"),
        console=console,
    ) as progress:
        progress.add_task(description=text, total=None)
        yield
