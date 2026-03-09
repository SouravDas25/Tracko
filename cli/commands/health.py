"""Health check command."""
import typer
from typing import Optional

from ..core.config import get_active_profile_config
from ..core.api import make_api_client, sdk_call_unwrapped
from ..core.output import console, print_json, print_success, print_error, spinner

import tracko_sdk


app = typer.Typer(help="Health check commands")


@app.command()
def check(
    raw: bool = typer.Option(False, "--raw", help="Output raw JSON"),
):
    """Check API health status."""
    config = get_active_profile_config()
    base_url = config.get("base_url", "http://localhost:8080")
    
    try:
        with spinner("Checking API health..."):
            with make_api_client(base_url) as api_client:
                api = tracko_sdk.HealthApi(api_client)
                result = sdk_call_unwrapped(lambda: api.health())
        
        if result is None:
            print_error("Health check failed")
            raise typer.Exit(1)
        
        if raw:
            print_json(result)
        else:
            print_success(f"API is healthy at {base_url}")
            if hasattr(result, "to_dict"):
                data = result.to_dict()
                for key, value in data.items():
                    console.print(f"  {key}: [cyan]{value}[/cyan]")
    
    except Exception as e:
        print_error(f"Health check failed: {e}")
        raise typer.Exit(1)
