# Trako CLI — Full Command Reference

All commands support `--raw` for JSON output. Always use `--raw` when calling from the agent.

---

## Setup & Health

```bash
trako health check --raw              # Check API connectivity
trako auth login --username "<user>"  # Interactive login (prompts for password)
trako config use <profile-name>       # Switch config profile
trako config list                     # List profiles
```

---

## Accounts

```bash
trako account list --raw              # List all accounts
trako account balances --raw          # Get all account balances (returns {id: balance} map)
trako account get <ID> --raw          # Get single account details
```

---

## Categories

```bash
trako category list --raw             # List all categories (id, name, categoryType)
```

categoryType values: `EXPENSE` or `INCOME`

---

## Transactions

### List

```bash
trako transaction list --raw --month <M> --year <Y> [--page N] [--size N]
```

`--month` and `--year` are required. Response is paginated.

### Search (available in next release)

```bash
trako transaction search "<query>" --raw \
  [--start-date YYYY-MM-DD] [--end-date YYYY-MM-DD] \
  [--min-amount <NUMBER>] [--max-amount <NUMBER>] \
  [--account-ids "1,2,3"] [--category-id <ID>] \
  [--page N] [--size N]
```

Full-text fuzzy search across transaction names, amounts and comments.

### Get

```bash
trako transaction get <ID> --raw
```

### History

```bash
trako transaction history <ID> --raw
```

Show the full change history for a transaction (CREATE, UPDATE, DELETE, REVERT operations).

### Revert

```bash
trako transaction revert <HISTORY_ID>
```

Revert a transaction to a previous history snapshot — undo an edit or restore a deleted transaction.

### Recycle Bin

```bash
trako transaction trash --raw
```

List all deleted transactions for the current user.

### Add Expense

```bash
trako transaction add-expense \
  --amount <NUMBER> \
  --name "<description>" \
  --currency <CODE> \
  --account-id <ID>    OR  --account-name "<name>" \
  --category-id <ID>   OR  --category-name "<name>" \
  [--comments "<text>"] \
  [--date YYYY-MM-DD] \
  [--exchange-rate <NUMBER>] \
  --raw
```

Required: `--amount`, `--name`, `--currency`, account (id or name), category (id or name).
If `--date` omitted → defaults to today.

### Add Income

```bash
trako transaction add-income \
  --amount <NUMBER> \
  --name "<description>" \
  --currency <CODE> \
  --account-id <ID>    OR  --account-name "<name>" \
  --category-id <ID>   OR  --category-name "<name>" \
  [--comments "<text>"] \
  [--date YYYY-MM-DD] \
  [--exchange-rate <NUMBER>] \
  --raw
```

The same required fields as add-expense.

### Add Transfer

Transfers move money between the user's own accounts. NO category.

```bash
trako transaction add-transfer \
  --amount <NUMBER> \
  --currency <CODE> \
  --from-account-id <ID>   OR  --from-account-name "<name>" \
  --to-account-id <ID>     OR  --to-account-name "<name>" \
  [--name "<description>"] \
  [--comments "<text>"] \
  [--date YYYY-MM-DD] \
  --raw
```

Required: `--amount`, `--currency`, from-account, to-account.
Never pass `--category-id` or `--category-name` to transfers.

### Update Expense / Income

```bash
trako transaction update-expense <ID> [--amount N] [--name "..."] [--account-id N] [--category-id N] [--currency CODE] [--comments "..."] [--date YYYY-MM-DD] [--exchange-rate N] --raw
trako transaction update-income <ID>  [same flags as update-expense] --raw
```

Only pass fields you want to change.

### Update Transfer

```bash
trako transaction update-transfer <ID> [--amount N] [--name "..."] [--from-account-id N] [--to-account-id N] [--comments "..."] [--date YYYY-MM-DD] --raw
```

### Delete

```bash
trako transaction delete <ID>
```

⚠️ Requires interactive terminal confirmation. No `--yes` flag exists.

### Aggregations

```bash
trako transaction summary --raw --start-date YYYY-MM-DD --end-date YYYY-MM-DD [--account-ids "1,2,3"]
trako transaction total-income --start-date YYYY-MM-DD --end-date YYYY-MM-DD --raw
trako transaction total-expense --start-date YYYY-MM-DD --end-date YYYY-MM-DD --raw
```

---

## Budgets

```bash
trako budget view --raw [--month M] [--year Y]     # View budget (defaults to current month)
trako budget current --raw                          # Shortcut for current month
trako budget allocate --category-id <ID> --amount <NUMBER> --raw [--month M] [--year Y]
trako budget available --raw [--month M] [--year Y] # Unassigned amount
```

---

## Contacts

```bash
trako contact list --raw
```

---

## Splits

```bash
trako split list --raw                              # All splits
trako split unsettled --raw                         # Unsettled only
trako split create --transaction-id <ID> --user-id "<uid>" --amount <NUMBER> [--contact-id <ID>] --raw
trako split settle <ID>                             # ⚠️ Interactive confirmation
trako split for-transaction <TRANSACTION_ID> --raw
trako split for-contact <CONTACT_ID> --raw
```

---

## Statistics

```bash
trako stats summary --range <RANGE> --type <TYPE> --raw [--start-date YYYY-MM-DD] [--end-date YYYY-MM-DD]
trako stats category-summary --category-id <ID> --range <RANGE> --type <TYPE> --raw [--start-date YYYY-MM-DD] [--end-date YYYY-MM-DD]
```

**Range values (camelCase):** `weekly`, `monthly`, `yearly`, `fiveYearly`, `tenYearly`, `custom`
- `custom` requires `--start-date` and `--end-date`

**Type values (UPPERCASE):** `DEBIT` (expenses), `CREDIT` (income), `TRANSFER`

---

## CSV Import

```bash
trako transaction csv-template    # Print expected CSV format
trako transaction import-csv <FILE> --account-id <ID> --raw
```

---

## Self-Update

```bash
trako update                        # Show current vs latest version
trako update check                  # Check if a newer version is available
trako update apply                  # Download and install the latest version (packaged binary only)
```

The CLI checks for updates automatically on startup (once per 24 hours) and prints a notice (on stderr) if a new version is available. Toggle the check with:

```bash
trako update disable                # Turn off the automatic startup check
trako update enable                 # Turn it back on
```
