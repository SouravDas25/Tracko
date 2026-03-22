# Tracko CLI

Command-line interface for Tracko expense management, built with [Typer](https://typer.tiangolo.com/) and [Rich](https://rich.readthedocs.io/).

## Installation

```bash
cd cli && pip install -r requirements.txt
```

Optional — enable tab completion:
```bash
python -m cli --install-completion bash   # or zsh / fish
```

## Quick Start

```bash
python -m cli auth login                          # Login
python -m cli account list                        # List accounts
python -m cli transaction add --amount 50 \
  --type expense --name "Lunch"                   # Add expense
python -m cli budget view                         # View budget
python -m cli db seed                             # Seed sample data
```

All commands follow the pattern: `python -m cli <group> <command> [options]`

## Command Groups

| Group | Description | Key Commands |
|-------|-------------|--------------|
| `auth` | Authentication | `login`, `logout` |
| `account` | Bank accounts & balances | `list`, `add`, `balances`, `summary`, `transactions` |
| `transaction` | Expenses, income & transfers | `add`, `list`, `transfer`, `import-csv`, `summary` |
| `budget` | Zero-based budgeting | `view`, `current`, `allocate`, `available` |
| `category` | Expense/income categories | `list`, `add`, `update`, `delete` |
| `contact` | People for splits | `list`, `add`, `update`, `delete` |
| `split` | Split expenses with contacts | `create`, `settle`, `unsettle`, `unsettled` |
| `stats` | Spending analytics | `summary`, `category-summary` |
| `currency` | Multi-currency management | `list`, `add`, `update`, `delete` |
| `exchange` | Live exchange rates | `get` |
| `user` | User management | `me`, `list`, `find-phone` |
| `config` | CLI profiles | `list`, `show`, `use`, `set` |
| `store` | Key-value JSON storage | `list`, `get`, `create`, `update`, `delete` |
| `db` | Database operations | `seed` |
| `health` | API health check | `check` |

Use `python -m cli <group> --help` for full details on any group.

## Common Workflows

### Daily Usage

```bash
python -m cli budget view                         # Check budget
python -m cli transaction add --amount 50 \
  --type expense --name "Coffee"                  # Log expense
python -m cli account balances                    # Check balances
python -m cli transaction list                    # Review transactions
```

### Monthly Review

```bash
python -m cli budget view --month 3 --year 2026
python -m cli stats summary --range MONTH --type EXPENSE
python -m cli stats category-summary --category-id 1 --range MONTH --type EXPENSE
```

### Transfers & Splits

```bash
python -m cli transaction transfer \
  --from-account-id 1 --to-account-id 2 \
  --amount 100 --name "Savings transfer"

python -m cli split create \
  --transaction-id 1 --contact-id 1 --amount 50
python -m cli split unsettled                     # View unsettled splits
python -m cli split settle 1                      # Mark as settled
```

### CSV Import

```bash
python -m cli transaction csv-template            # Print expected format
python -m cli transaction import-csv \
  --file data.csv --account-id 1                  # Import with progress bar
```

### Setup New Environment

```bash
python -m cli auth login
python -m cli db seed                             # Seed sample data
python -m cli db seed --dry-run                   # Preview without creating
python -m cli account list                        # Verify
```

## Global Options

- `--raw` on any command for JSON output: `python -m cli account list --raw`
- `--help` on any command for usage details: `python -m cli transaction add --help`

## Configuration

Profiles are stored in `~/.tracko-cli.json`:

```json
{
  "active_profile": "default",
  "profiles": {
    "default": { "base_url": "http://localhost:8080" },
    "production": { "base_url": "https://api.tracko.com" }
  }
}
```

```bash
python -m cli config list                         # List profiles
python -m cli config use production               # Switch profile
python -m cli config set --base-url <url>         # Update URL
```

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Not logged in | `python -m cli auth login` |
| Wrong backend URL | `python -m cli config set --base-url http://localhost:8080` |
| Need different environment | `python -m cli config use <profile>` |
| Backend not running | Start with `task start` from project root |

## Development

```
cli/
├── main.py              # Typer app with command groups
├── commands/            # Command modules (*_new.py = Typer, *.py = deprecated argparse)
├── core/
│   ├── api.py          # API client wrapper
│   ├── config.py       # Profile management
│   └── output.py       # Rich output helpers
└── utils/
    ├── prompts.py      # Interactive prompts
    └── dates.py        # Date parsing
```

### Adding a New Command Group

1. Create `commands/mycommand_new.py`
2. Define Typer app: `app = typer.Typer(help="...")`
3. Add commands with `@app.command()`
4. Register in `main.py`: `app.add_typer(mycommand_new.app, name="mycommand")`
