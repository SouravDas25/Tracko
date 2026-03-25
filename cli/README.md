# Trako CLI

Command-line interface for Trako expense management, built with [Typer](https://typer.tiangolo.com/) and [Rich](https://rich.readthedocs.io/).

## Installation

### Download Binary (no Python needed)

Grab the latest binary for your OS from [GitHub Releases](https://github.com/SouravDas25/Tracko/releases/latest).

**Windows:**
```powershell
# Move the downloaded binary to a folder in your PATH
mkdir $env:USERPROFILE\trako
move $env:USERPROFILE\Downloads\trako-windows.exe $env:USERPROFILE\trako\trako.exe
# Add to PATH (run once)
[Environment]::SetEnvironmentVariable("Path", $env:Path + ";$env:USERPROFILE\trako", "User")
# Restart terminal, then:
trako --help
```

**Linux:**
```bash
curl -L https://github.com/SouravDas25/Tracko/releases/latest/download/trako-linux -o trako
chmod +x trako
sudo mv trako /usr/local/bin/
trako --help
```

**macOS:**
```bash
curl -L https://github.com/SouravDas25/Tracko/releases/latest/download/trako-macos -o trako
chmod +x trako
sudo mv trako /usr/local/bin/
trako --help
```

### Install from Source (requires Python 3.10+)

```bash
pip install ./sdk
pip install ./cli
trako --help
```

## Quick Start

```bash
trako auth login                          # Login
trako account list                        # List accounts
trako transaction add --amount 50 \
  --type expense --name "Lunch"           # Add expense
trako budget view                         # View budget
trako db seed                             # Seed sample data
```

All commands follow the pattern: `trako <group> <command> [options]`

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

Use `trako <group> --help` for full details on any group.

## Common Workflows

### Daily Usage

```bash
trako budget view                         # Check budget
trako transaction add --amount 50 \
  --type expense --name "Coffee"          # Log expense
trako account balances                    # Check balances
trako transaction list                    # Review transactions
```

### Monthly Review

```bash
trako budget view --month 3 --year 2026
trako stats summary --range MONTH --type EXPENSE
trako stats category-summary --category-id 1 --range MONTH --type EXPENSE
```

### Transfers & Splits

```bash
trako transaction transfer \
  --from-account-id 1 --to-account-id 2 \
  --amount 100 --name "Savings transfer"

trako split create \
  --transaction-id 1 --contact-id 1 --amount 50
trako split unsettled                     # View unsettled splits
trako split settle 1                      # Mark as settled
```

### CSV Import

```bash
trako transaction csv-template            # Print expected format
trako transaction import-csv \
  --file data.csv --account-id 1          # Import with progress bar
```

### Setup New Environment

```bash
trako auth login
trako db seed                             # Seed sample data
trako db seed --dry-run                   # Preview without creating
trako account list                        # Verify
```

## Global Options

- `--raw` on any command for JSON output: `trako account list --raw`
- `--help` on any command for usage details: `trako transaction add --help`

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
trako config list                         # List profiles
trako config use production               # Switch profile
trako config set --base-url <url>         # Update URL
```

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Not logged in | `trako auth login` |
| Wrong backend URL | `trako config set --base-url http://localhost:8080` |
| Need different environment | `trako config use <profile>` |
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
