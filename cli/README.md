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

All commands follow the pattern: `python -m cli <group> <command> [options]`

### Authentication

```bash
python -m cli auth login              # Interactive login with password prompt
python -m cli auth logout             # Clear saved token
```

### Configuration

```bash
python -m cli config list             # List all profiles
python -m cli config show             # Show current profile
python -m cli config use <profile>    # Switch profile
python -m cli config set --base-url <url>  # Update base URL
```

### Accounts

```bash
python -m cli account list                    # List all accounts
python -m cli account add --name "HDFC"       # Create account
python -m cli account get 1                   # Get account by ID
python -m cli account update 1 --name "HDFC Savings"
python -m cli account delete 1                # Delete (with confirmation)
python -m cli account balances                # Get all balances
python -m cli account summary 1 --start-date 2026-01-01 --end-date 2026-12-31
python -m cli account transactions 1          # Get account transactions
```

### Categories

```bash
python -m cli category list                   # List all categories
python -m cli category add --name "Food" --type EXPENSE
python -m cli category get 1
python -m cli category update 1 --name "Food & Dining"
python -m cli category delete 1               # Delete (with confirmation)
```

### Contacts

```bash
python -m cli contact list
python -m cli contact add --name "Alice" --phone "9876543210" --email "alice@example.com"
python -m cli contact get 1
python -m cli contact update 1 --name "Alice Smith"
python -m cli contact delete 1                # Delete (with confirmation)
```

### Users

```bash
python -m cli user list                       # List all users (admin)
python -m cli user me                         # Get current user info
python -m cli user get 1
python -m cli user find-phone 9876543210
python -m cli user upsert --name "John" --email "john@example.com"
```

### Transactions

```bash
# List and query
python -m cli transaction list --month 3 --year 2026
python -m cli transaction get 1

# Create
python -m cli transaction add \
  --account-id 1 \
  --category-id 2 \
  --amount 50 \
  --type expense \
  --name "Lunch" \
  --comments "Team lunch"

# Update
python -m cli transaction update 1 --amount 60 --name "Lunch (updated)"

# Delete
python -m cli transaction delete 1            # Delete (with confirmation)

# Summaries
python -m cli transaction summary --start-date 2026-01-01 --end-date 2026-12-31
python -m cli transaction total-income --start-date 2026-01-01 --end-date 2026-12-31
python -m cli transaction total-expense --start-date 2026-01-01 --end-date 2026-12-31

# Transfer
python -m cli transaction transfer \
  --from-account-id 1 \
  --to-account-id 2 \
  --amount 100 \
  --name "Savings transfer"

# Import CSV (with progress bar)
python -m cli transaction import-csv --file data.csv --account-id 1

# Print CSV template (header + sample row)
python -m cli transaction csv-template
```

### Budget

```bash
# View budget with usage percentages
python -m cli budget view --month 3 --year 2026
python -m cli budget current              # Current month

# Allocate funds
python -m cli budget allocate \
  --category-id 1 \
  --amount 5000 \
  --month 3 \
  --year 2026

# Check available
python -m cli budget available --month 3 --year 2026
```

### Currency

```bash
python -m cli currency list
python -m cli currency add --code USD --rate 0.85
python -m cli currency update --code USD --rate 0.86
python -m cli currency delete USD             # Delete (with confirmation)
```

### Splits

```bash
python -m cli split list
python -m cli split get 1
python -m cli split create --transaction-id 1 --user-id <uuid> --amount 50 --contact-id 1
python -m cli split delete 1                  # Delete (with confirmation)
python -m cli split settle 1                  # Mark as settled (with confirmation)
python -m cli split unsettle 1                # Mark as unsettled (with confirmation)

# Query splits
python -m cli split for-transaction 1
python -m cli split for-contact 1
python -m cli split unsettled                 # All unsettled for current user
python -m cli split unsettled-contact 1
```

### Statistics

```bash
# Overall stats with Rich tables
python -m cli stats summary \
  --range MONTH \
  --type EXPENSE \
  --start-date 2026-01-01 \
  --end-date 2026-12-31

# Category-specific stats
python -m cli stats category-summary \
  --category-id 1 \
  --range MONTH \
  --type EXPENSE
```

### Exchange Rates

```bash
python -m cli exchange get --base USD         # Get current rates
```

### JSON Store

```bash
python -m cli store list
python -m cli store get <name>
python -m cli store create --name "config" --value '{"key":"value"}'
python -m cli store update <name> --value '{"key":"new_value"}'
python -m cli store delete <name>             # Delete (with confirmation)
```

### Database Operations

```bash
# Seed database with sample data (with progress bars)
python -m cli db seed

# Preview without creating
python -m cli db seed --dry-run

# Skip transaction creation
python -m cli db seed --skip-transactions
```

### Health Check

```bash
python -m cli health check                    # With spinner
python -m cli health check --raw              # Raw JSON output
```

## Global Options

### Raw Output

Add `--raw` to any command for JSON output:

```bash
python -m cli account list --raw
python -m cli transaction get 1 --raw
```

### Help

Get help for any command:

```bash
python -m cli --help
python -m cli account --help
python -m cli transaction add --help
```

## Interactive Features

### Password Prompts

Login prompts for password securely (hidden input):

```bash
python -m cli auth login
# Username: user@example.com
# Password: ********
```

### Confirmations

Destructive operations require confirmation:

```bash
python -m cli account delete 1
# Delete account 1? [y/N]: y
```

### Progress Indicators

- **Spinners** for API calls
- **Progress bars** for bulk operations (CSV import, database seeding)

## Shell Completion

Enable tab completion for your shell:

```bash
# Bash
python -m cli --install-completion bash

# Zsh
python -m cli --install-completion zsh

# Fish
python -m cli --install-completion fish
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
python -m cli budget view

# Add expense
python -m cli transaction add --amount 50 --type expense --name "Coffee"

# Check balance
python -m cli account balances

# Evening: Review today's transactions
python -m cli transaction list
```

### Monthly Review

```bash
# View monthly budget with usage
python -m cli budget view --month 3 --year 2026

# Get statistics
python -m cli stats summary --range MONTH --type EXPENSE

# Check category spending
python -m cli stats category-summary --category-id 1 --range MONTH --type EXPENSE
```

### Setup New Environment

```bash
# Login
python -m cli auth login

# Seed with sample data
python -m cli db seed

# Verify
python -m cli account list
python -m cli category list
python -m cli transaction list
```


## Troubleshooting

### Not logged in

```bash
python -m cli auth login
```

### Wrong base URL

```bash
python -m cli config set --base-url http://localhost:8080
```

### Switch profile

```bash
python -m cli config use production
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
