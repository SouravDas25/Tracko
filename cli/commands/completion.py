"""Shell completion setup command."""
import typer
from ..core.output import console, print_success, print_info


app = typer.Typer(help="Shell completion setup")


@app.command()
def install(
    shell: str = typer.Argument(..., help="Shell type (bash/zsh/fish/powershell)"),
):
    """Install shell completion for the CLI."""
    shell = shell.lower()
    
    if shell == "bash":
        print_info("Add this to your ~/.bashrc:")
        console.print('[dim]eval "$(_TRACKO_COMPLETE=bash_source tracko)"[/dim]')
    elif shell == "zsh":
        print_info("Add this to your ~/.zshrc:")
        console.print('[dim]eval "$(_TRACKO_COMPLETE=zsh_source tracko)"[/dim]')
    elif shell == "fish":
        print_info("Add this to ~/.config/fish/completions/tracko.fish:")
        console.print('[dim]eval (env _TRACKO_COMPLETE=fish_source tracko)[/dim]')
    elif shell == "powershell":
        print_info("Add this to your PowerShell profile:")
        console.print('[dim]Invoke-Expression (& tracko --show-completion powershell)[/dim]')
    else:
        console.print(f"[red]Unsupported shell: {shell}[/red]")
        console.print("Supported shells: bash, zsh, fish, powershell")
        raise typer.Exit(1)
    
    print_success(f"\nRestart your shell or source the config file to enable completion")


@app.command()
def show(
    shell: str = typer.Argument(..., help="Shell type (bash/zsh/fish/powershell)"),
):
    """Show completion script for the specified shell."""
    # Typer handles this automatically with --show-completion
    print_info(f"Run: tracko --show-completion {shell}")
