# Tracko CLI - Typer Edition

Modern command-line interface for Tracko expense management, built with Typer and Rich.

## Features

- 🎨 **Beautiful output** with Rich tables, colors, and spinners
- 🔐 **Interactive prompts** for passwords and confirmations
- 📊 **Progress bars** for bulk operations
- ⚡ **Fast and intuitive** grouped commands
- 🛡️ **Type-safe** with automatic validation
- 🎯 **Tab completion** support (bash/zsh/fish)

## Installation

```bash
cd cli
pip install -r requirements.txt
```

## Quick Start

```bash
# Login
python -m cli auth login

# Check health
python -m cli health check

# List accounts
python -m cli account list

# Create transaction
python -m cli transaction add --amount 50 --type expense --name "Lunch"

# View budget
python -m cli budget view

# Seed database with sample data
python -m cli db seed
```

## Command Structure

All commands follow the pattern: `tracko <group> <command> [options]`

### Authentication

```bash
tracko auth login              # Interactive login with password prompt
tracko auth logout             # Clear saved token
```

### Configuration

```bash
tracko config list             # List all profiles
tracko config show             # Show current profile
tracko config use <profile>    # Switch profile
tracko config set --base-url <url>  # Update base URL
```

### Accounts

```bash
tracko account list                    # List all accounts
tracko account add --name "HDFC"       # Create account
tracko account get 1                   # Get account by ID
tracko account update 1 --name "HDFC Savings"
tracko account delete 1                # Delete (with confirmation)
tracko account balances                # Get all balances
tracko account summary 1 --start-date 2026-01-01 --end-date 2026-12-31
tracko account transactions 1          # Get account transactions
```

### Categories

```bash
tracko category list                   # List all categories
tracko category add --name "Food" --type EXPENSE
tracko category get 1
tracko category update 1 --name "Food & Dining"
tracko category delete 1               # Delete (with confirmation)
```

### Contacts

```bash
tracko contact list
tracko contact add --name "Alice" --phone "9876543210" --email "alice@example.com"
tracko contact get 1
tracko contact update 1 --name "Alice Smith"
tracko contact delete 1                # Delete (with confirmation)
```

### Users

```bash
tracko user list                       # List all users (admin)
tracko user me                         # Get current user info
tracko user get 1
tracko user find-phone 9876543210
tracko user upsert --name "John" --email "john@example.com"
```

### Transactions

```bash
# List and query
tracko transaction list --month 3 --year 2026
tracko transaction get 1

# Create
tracko transaction add \
  --account-id 1 \
  --category-id 2 \
  --amount 50 \
  --type expense \
  --name "Lunch" \
  --comments "Team lunch"

# Update
tracko transaction update 1 --amount 60 --name "Lunch (updated)"

# Delete
tracko transaction delete 1            # Delete (with confirmation)

# Summaries
tracko transaction summary --start-date 2026-01-01 --end-date 2026-12-31
tracko transaction total-income --start-date 2026-01-01 --end-date 2026-12-31
tracko transaction total-expense --start-date 2026-01-01 --end-date 2026-12-31

# Transfer
tracko transaction transfer \
  --from-account-id 1 \
  --to-account-id 2 \
  --amount 100 \
  --name "Savings transfer"

# Import CSV (with progress bar)
tracko transaction import-csv --file data.csv --account-id 1
```

### Budget

```bash
# View budget with usage percentages
tracko budget view --month 3 --year 2026
tracko budget current              # Current month

# Allocate funds
tracko budget allocate \
  --category-id 1 \
  --amount 5000 \
  --month 3 \
  --year 2026

# Check available
tracko budget available --month 3 --year 2026
```

### Currency

```bash
tracko currency list
tracko currency add --code USD --rate 0.85
tracko currency update --code USD --rate 0.86
tracko currency delete USD             # Delete (with confirmation)
```

### Splits

```bash
tracko split list
tracko split get 1
tracko split create --transaction-id 1 --user-id <uuid> --amount 50 --contact-id 1
tracko split delete 1                  # Delete (with confirmation)
tracko split settle 1                  # Mark as settled (with confirmation)
tracko split unsettle 1                # Mark as unsettled (with confirmation)

# Query splits
tracko split for-transaction 1
tracko split for-contact 1
tracko split unsettled                 # All unsettled for current user
tracko split unsettled-contact 1
```

### Statistics

```bash
# Overall stats with Rich tables
tracko stats summary \
  --range MONTH \
  --type EXPENSE \
  --start-date 2026-01-01 \
  --end-date 2026-12-31

# Category-specific stats
tracko stats category-summary \
  --category-id 1 \
  --range MONTH \
  --type EXPENSE
```

### Exchange Rates

```bash
tracko exchange get --base USD         # Get current rates
```

### JSON Store

```bash
tracko store list
tracko store get <name>
tracko store create --name "config" --value '{"key":"value"}'
tracko store update <name> --value '{"key":"new_value"}'
tracko store delete <name>             # Delete (with confirmation)
```

### Database Operations

```bash
# Seed database with sample data (with progress bars)
tracko db seed

# Preview without creating
tracko db seed --dry-run

# Skip transaction creation
tracko db seed --skip-transactions
```

### Health Check

```bash
tracko health check                    # With spinner
tracko health check --raw              # Raw JSON output
```

## Global Options

### Raw Output

Add `--raw` to any command for JSON output:

```bash
tracko account list --raw
tracko transaction get 1 --raw
```

### Help

Get help for any command:

```bash
tracko --help
tracko account --help
tracko transaction add --help
```

## Interactive Features

### Password Prompts

Login prompts for password securely (hidden input):

```bash
tracko auth login
# Username: user@example.com
# Password: ********
```

### Confirmations

Destructive operations require confirmation:

```bash
tracko account delete 1
# Delete account 1? [y/N]: y
```

### Progress Indicators

- **Spinners** for API calls
- **Progress bars** for bulk operations (CSV import, database seeding)

## Shell Completion

Enable tab completion for your shell:

```bash
# Bash
tracko --install-completion bash

# Zsh
tracko --install-completion zsh

# Fish
tracko --install-completion fish
```

## Configuration

Configuration is stored in `~/.tracko-cli.json`:

```json
{
  "active_profile": "default",
  "profiles": {
    "default": {
      "base_url": "http://localhost:8080",
      "token": "eyJ..."
    },
    "production": {
      "base_url": "https://api.tracko.com",
      "token": "eyJ..."
    }
  }
}
```

## Examples

### Daily Workflow

```bash
# Morning: Check budget
tracko budget view

# Add expense
tracko transaction add --amount 50 --type expense --name "Coffee"

# Check balance
tracko account balances

# Evening: Review today's transactions
tracko transaction list
```

### Monthly Review

```bash
# View monthly budget with usage
tracko budget view --month 3 --year 2026

# Get statistics
tracko stats summary --range MONTH --type EXPENSE

# Check category spending
tracko stats category-summary --category-id 1 --range MONTH --type EXPENSE
```

### Setup New Environment

```bash
# Login
tracko auth login

# Seed with sample data
tracko db seed

# Verify
tracko account list
tracko category list
tracko transaction list
```

## Migration from Old CLI

The new CLI uses grouped commands instead of flat structure:

| Old Command | New Command |
|-------------|-------------|
| `cli accounts list` | `tracko account list` |
| `cli transactions add` | `tracko transaction add` |
| `cli budget view` | `tracko budget view` |

Global options `--base-url` and `--token` are removed. Use `tracko config` instead.

## Troubleshooting

### Not logged in

```bash
tracko auth login
```

### Wrong base URL

```bash
tracko config set --base-url http://localhost:8080
```

### Switch profile

```bash
tracko config use production
```

## Development

### Project Structure

```
cli/
├── main.py              # Typer app with command groups
├── commands/            # Command modules
│   ├── *_new.py        # New Typer commands
│   └── *.py            # Old argparse commands (deprecated)
├── core/
│   ├── api.py          # SDK client wrapper
│   ├── config.py       # Profile management
│   └── output.py       # Rich output helpers
└── utils/
    ├── prompts.py      # Interactive prompts
    └── dates.py        # Date parsing
```

### Adding New Commands

1. Create `commands/mycommand_new.py`
2. Define Typer app: `app = typer.Typer(help="...")`
3. Add commands with `@app.command()`
4. Register in `main.py`: `app.add_typer(mycommand_new.app, name="mycommand")`

## License

See main project LICENSE.
