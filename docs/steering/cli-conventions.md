---
inclusion: fileMatch
fileMatchPattern: "cli/**/*.py"
---

# CLI Conventions — Python / Typer

## Project Structure
```
cli/
├── main.py          # Typer app with all command group registrations
├── entry.py         # Alternative entry point
├── __main__.py      # Module entry point (python -m cli)
├── commands/        # One file per command group (auth.py, account.py, transaction.py, etc.)
├── core/            # Shared infrastructure
│   ├── api.py       # API client wrapper (uses tracko-sdk)
│   ├── config.py    # Profile/config management
│   └── output.py    # Rich console output helpers
├── utils/           # Utility modules
│   ├── prompts.py   # Interactive prompts
│   └── dates.py     # Date parsing utilities
└── tests/           # Test suite
```

## Coding Standards
- Python 3.10+ — use modern type hints (`str | None` instead of `Optional[str]`)
- Use Typer for all CLI commands and options
- Use Rich for all terminal output (tables, panels, progress bars)
- Use the `tracko-sdk` package for all API calls — do not make raw HTTP requests
- Each command group is a separate `typer.Typer()` instance registered in `main.py`
- Use `Annotated[type, typer.Option(...)]` syntax for command parameters
- Format with Black (configured in workspace settings)

## Testing
- pytest with coverage (`--cov=cli`)
- Tests in `cli/tests/`
- Use `pytest-mock` for mocking API calls
- Coverage target: meaningful coverage on command logic and core modules

## Dependencies
- CLI depends on `tracko-sdk` — install SDK first: `pip install -e ../sdk`
- Install CLI in editable mode: `pip install -e .`
- Run via: `trako <command> <subcommand>`

## Output Patterns
- Use `core/output.py` helpers for consistent formatting
- Tables: Rich Table with consistent column styling
- Errors: Rich Panel with red border
- Success: Rich Panel with green border
- Use `typer.echo()` for simple text, Rich console for formatted output
