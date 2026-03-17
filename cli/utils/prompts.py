"""Interactive prompts and confirmations."""
import typer
from rich.prompt import Prompt, Confirm


def confirm(message: str, default: bool = False) -> bool:
    """Ask for confirmation."""
    return Confirm.ask(message, default=default)


def prompt(message: str, password: bool = False, default: str = None) -> str:
    """Prompt for input."""
    return Prompt.ask(message, password=password, default=default)


def prompt_or_exit(message: str, password: bool = False) -> str:
    """Prompt for input or exit if empty."""
    value = Prompt.ask(message, password=password)
    if not value:
        raise typer.Exit(1)
    return value
