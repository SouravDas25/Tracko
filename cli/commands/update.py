"""Self-update commands for Trako CLI."""
import os
import platform
import sys
import tempfile
import typer

from ..core.output import console, print_success, print_info, print_error
from ..core.updater import (
    check_for_update,
    download_binary,
    get_current_version,
    get_download_url,
    get_latest_version,
    get_platform_asset_name,
    replace_and_exit,
)
from ..core.config import set_update_setting

app = typer.Typer(help="Self-update management")


@app.callback(invoke_without_command=True)
def update(
    ctx: typer.Context,
):
    if ctx.invoked_subcommand is None:
        _show_status()


def _show_status():
    current = get_current_version() or "dev"
    latest = get_latest_version()

    if latest:
        if latest == current:
            print_success(f"You are on the latest version ({current})")
        else:
            print_info(f"Current: {current}")
            print_info(f"Latest:  {latest}")
            console.print("\nRun [bold]trako update apply[/bold] to upgrade.")
    else:
        console.print(f"Current version: [bold]{current}[/bold]")
        print_error("Could not check for updates (network error)")


@app.command()
def check():
    """Check if a newer version is available."""
    current = get_current_version()
    if not current:
        print_info("Dev build — update checks are skipped.")
        return

    is_available, latest = check_for_update()
    if is_available:
        print_info(f"Update available: {current} → {latest}")
        console.print("Run [bold]trako update apply[/bold] to upgrade.")
        raise typer.Exit(1)
    elif latest:
        print_success(f"You are on the latest version ({latest})")
    else:
        print_error("Could not check for updates (network error)")


@app.command()
def enable():
    """Enable the daily automatic update check on startup."""
    set_update_setting("auto_update_check", True)
    print_success("Automatic update checks enabled.")


@app.command()
def disable():
    """Disable the daily automatic update check on startup."""
    set_update_setting("auto_update_check", False)
    print_success("Automatic update checks disabled.")


@app.command()
def apply():
    """Download and install the latest version."""
    if not getattr(sys, "frozen", False):
        print_error(
            "Self-update is only available for the packaged binary. "
            "For a source or pip install, update via your package manager instead."
        )
        raise typer.Exit(1)

    asset_name = get_platform_asset_name()
    if not asset_name:
        print_error(f"Unsupported platform: {sys.platform} {platform.machine()}")
        raise typer.Exit(1)

    latest = get_latest_version()
    current = get_current_version()

    if latest and latest == current:
        print_success("Already on the latest version.")
        raise typer.Exit(0)

    url = get_download_url(asset_name)
    if not url:
        print_error("Could not find download URL for this platform.")
        raise typer.Exit(1)

    print_info(f"Downloading {asset_name} ({latest or 'latest'})...")

    fd, tmp_path = tempfile.mkstemp(prefix='trako-update-')
    os.close(fd)

    if not download_binary(url, tmp_path):
        print_error("Download failed.")
        if os.path.exists(tmp_path):
            os.remove(tmp_path)
        raise typer.Exit(1)

    print_success("Download complete. Installing...")
    replace_and_exit(tmp_path)
