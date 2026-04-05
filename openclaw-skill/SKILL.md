---
name: trako
description: >
  Manage personal finances with the Trako expense tracker.
  Use when the user asks about expenses, income, transfers, transactions,
  accounts, categories, balances, budgets, splits, or contacts.
  Do NOT use for currency conversion or exchange rates.
version: 1.0.0
user-invocable: true
metadata:
  openclaw:
    requires:
      bins:
        - trako
    emoji: "💰"
    homepage: https://github.com/SouravDas25/Tracko
---

# Trako — Personal Finance Manager

You have access to the `trako` CLI for managing personal finances.
You can always pass `--raw` to every command so you receive JSON output you can parse.

## Important Rules

1. Always run `trako account list --raw` and `trako category list --raw` first to resolve IDs before creating or updating transactions.
2. The CLI supports `--account-name` and `--category-name` as alternatives to IDs for transaction commands — prefer names when the user provides them, as the CLI resolves them automatically.
3. Dates must be in `YYYY-MM-DD` format.
4. Currency codes are uppercase ISO 4217 (e.g. `INR`, `USD`, `EUR`).
5. Commands that delete or settle require interactive confirmation in the terminal. There is no `--yes` flag. Warn the user they will see a prompt.
6. When the user says "expense" they mean `add-expense`. When they say "income" they mean `add-income`. When they say "transfer" they mean `add-transfer`.
7. If a command fails with an auth error, tell the user to run `trako auth login`.

---

## Setup & Health

### Check if the API is reachable
```
trako health check --raw
```

### Login (interactive — prompts for password)
```
trako auth login --username "<username>"
```

### Switch config profile
```
trako config use <profile-name>
```

---

## Accounts

### List all accounts
```
trako account list --raw
```

### Get account balances
```
trako account balances --raw
```

### Get a single account
```
trako account get <ID> --raw
```

---

## Categories

### List all categories
```
trako category list --raw
```
Returns ID, name, and type (INCOME / EXPENSE) for each category.

---

## Transactions

### List transactions
```
trako transaction list --raw [--month M] [--year Y] [--page N] [--size N]
```
Defaults to current month if no month/year given. Response is paginated.

### Get a single transaction
```
trako transaction get <ID> --raw
```

### Add an expense
```
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
Required: `--amount`, `--name`, `--currency`, one of account id/name, one of category id/name.
If `--date` is omitted the CLI defaults to today.

### Add an income
```
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
Same required fields as add-expense.

### Add a transfer
```
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
Required: `--amount`, `--currency`, source account, destination account.
Source and destination must be different accounts.

### Update an expense
```
trako transaction update-expense <ID> \
  [--amount <NUMBER>] \
  [--name "<description>"] \
  [--account-id <ID>] \
  [--category-id <ID>] \
  [--currency <CODE>] \
  [--comments "<text>"] \
  [--date YYYY-MM-DD] \
  [--exchange-rate <NUMBER>] \
  --raw
```
Only pass the fields you want to change.

### Update an income
```
trako transaction update-income <ID> \
  [--amount <NUMBER>] \
  [--name "<description>"] \
  [--account-id <ID>] \
  [--category-id <ID>] \
  [--currency <CODE>] \
  [--comments "<text>"] \
  [--date YYYY-MM-DD] \
  [--exchange-rate <NUMBER>] \
  --raw
```

### Update a transfer
```
trako transaction update-transfer <ID> \
  [--amount <NUMBER>] \
  [--name "<description>"] \
  [--from-account-id <ID>] \
  [--to-account-id <ID>] \
  [--comments "<text>"] \
  [--date YYYY-MM-DD] \
  --raw
```

### Delete a transaction
```
trako transaction delete <ID>
```
Requires interactive confirmation. Tell the user: "Please confirm 'y' in your terminal."

### Transaction summary
```
trako transaction summary --raw [--start-date YYYY-MM-DD] [--end-date YYYY-MM-DD] [--account-ids "1,2,3"]
```

### Total income in a date range
```
trako transaction total-income --start-date YYYY-MM-DD --end-date YYYY-MM-DD --raw
```

### Total expense in a date range
```
trako transaction total-expense --start-date YYYY-MM-DD --end-date YYYY-MM-DD --raw
```

---

## Budgets

### View budget for a month
```
trako budget view --raw [--month M] [--year Y]
```
Defaults to current month/year. Shows total budget, income, spent, available, and per-category allocations.

### View current month's budget
```
trako budget current --raw
```

### Allocate budget to a category
```
trako budget allocate --category-id <ID> --amount <NUMBER> --raw [--month M] [--year Y]
```

### Get available amount to assign
```
trako budget available --raw [--month M] [--year Y]
```

---

## Contacts

### List contacts
```
trako contact list --raw
```

---

## Splits

### List all splits
```
trako split list --raw
```

### Create a split
```
trako split create --transaction-id <ID> --user-id "<uid>" --amount <NUMBER> [--contact-id <ID>] --raw
```

### View unsettled splits
```
trako split unsettled --raw
```

### Settle a split (interactive confirmation)
```
trako split settle <ID>
```

### Get splits for a transaction
```
trako split for-transaction <TRANSACTION_ID> --raw
```

### Get splits for a contact
```
trako split for-contact <CONTACT_ID> --raw
```

---

## Statistics

### Spending/income summary
```
trako stats summary --range <RANGE> --type <TYPE> --raw [--start-date YYYY-MM-DD] [--end-date YYYY-MM-DD]
```
Ranges: `WEEK`, `MONTH`, `YEAR`, `FIVE_YEAR`, `TEN_YEAR`, `CUSTOM` (requires start/end dates).
Types: `INCOME`, `EXPENSE`.

### Category-level stats
```
trako stats category-summary --category-id <ID> --range <RANGE> --type <TYPE> --raw
```

---

## Common Workflows

### "I spent 500 on groceries"
1. `trako account list --raw` → find the account
2. `trako category list --raw` → find the food/grocery category
3. `trako transaction add-expense --amount 500 --name "Groceries" --currency INR --category-name "FOOD" --account-name "Cash" --raw`

### "How much did I spend this month?"
```
trako transaction total-expense --start-date 2026-03-01 --end-date 2026-03-31 --raw
```
Or for a breakdown: `trako stats summary --range MONTH --type EXPENSE --raw`

### "Show my balances"
```
trako account balances --raw
```

### "Move 1000 from Savings to Cash"
```
trako transaction add-transfer --amount 1000 --currency INR --from-account-name "Savings" --to-account-name "Cash" --raw
```

### "What's my budget looking like?"
```
trako budget view --raw
```

### "Who owes me money?"
```
trako split unsettled --raw
```
