"""Configuration management commands."""
import typer
from rich.table import Table

from ..core.config import (
    load_config,
    get_active_profile_name,
    set_active_profile,
    update_profile,
    list_profiles,
)
from ..core.output import console, create_table, print_success, print_info
from ..utils.prompts import confirm


app = typer.Typer(help="Configuration management")


@app.command()
def list():
    """List all configuration profiles."""
    data = list_profiles()
    active = data["active"]
    profiles = data["profiles"]
    
    table = create_table(title="Configuration Profiles")
    table.add_column("Profile", style="cyan")
    table.add_column("Active", justify="center")
    
    for p in profiles:
        is_active = "✓" if p == active else ""
        table.add_row(p, is_active)
    
    console.print(table)


@app.command()
def use(
    profile: str = typer.Argument(..., help="Profile name to activate"),
):
    """Set the active profile."""
    if confirm(f"Switch to profile '{profile}'?", default=True):
        set_active_profile(profile)
        print_success(f"Active profile set to '{profile}'")


@app.command()
def set(
    base_url: str = typer.Option(None, "--base-url", help="Set base URL"),
    profile: str = typer.Option(None, "--profile", help="Profile to update (defaults to active)"),
):
    """Set configuration values for a profile."""
    profile_name = profile or get_active_profile_name()
    updates = {}
    
    if base_url:
        updates["base_url"] = base_url
    
    if not updates:
        print_info("No updates provided. Use --base-url to update configuration.")
        raise typer.Exit(1)
    
    update_profile(profile_name, updates)
    print_success(f"Updated profile '{profile_name}': {updates}")


@app.command()
def show(
    profile: str = typer.Option(None, "--profile", help="Profile to show (defaults to active)"),
):
    """Show configuration for a profile."""
    profile_name = profile or get_active_profile_name()
    
    cfg = load_config()
    profiles = cfg.get("profiles", {})
    
    if profile_name not in profiles:
        console.print(f"[red]Profile '{profile_name}' not found.[/red]")
        raise typer.Exit(1)
    
    profile_data = profiles[profile_name]
    
    table = create_table(title=f"Profile: {profile_name}")
    table.add_column("Setting", style="cyan")
    table.add_column("Value", style="green")
    
    for k, v in profile_data.items():
        # Mask token for security
        if k == "token" and v:
            v = v[:10] + "..." + v[-5:]
        table.add_row(k, str(v))
    
    console.print(table)
