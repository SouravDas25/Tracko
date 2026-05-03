---
name: trako
description: >
  Manage personal finances with the Trako expense tracker CLI.
  Use when the user asks about expenses, income, transfers, transactions,
  accounts, categories, balances, budgets, splits, contacts, or spending stats.
  Also use when the user says "I spent...", "I earned...", "move money...",
  "how much did I spend", "what's my balance", "who owes me", or "transfer to...".
  Do NOT use for currency conversion, exchange rates, or financial advice.
version: 2.0.0
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

You have access to the `trako` CLI. Always pass `--raw` to get JSON output you can parse.

For full command syntax, read `references/commands.md`.
For response JSON shapes, read `references/response-schemas.md`.

---

## Rules

1. **Resolve IDs first.** Before any create/update, run `trako account list --raw` and `trako category list --raw` to get valid IDs/names. Never fabricate IDs.
2. **Prefer names over IDs.** Use `--account-name` and `--category-name` when the user provides names — the CLI resolves them.
3. **Dates:** `YYYY-MM-DD` format only.
4. **Currency:** Uppercase ISO 4217 (`INR`, `USD`, `EUR`).
5. **Destructive commands** (delete, settle) require interactive terminal confirmation. Warn the user: "Please confirm 'y' in your terminal."
6. **Auth errors** → tell user to run `trako auth login`.
7. **Context reuse:** If you already fetched accounts/categories in this conversation, don't fetch again.
8. **Transaction list requires `--month` and `--year`** — they are not optional.

---

## Decision Tree

### Classify user intent:

| User says... | Command |
|---|---|
| "I spent...", "bought...", "paid for..." | `add-expense` |
| "I earned...", "received...", "got paid..." | `add-income` |
| "move...", "transfer...", "shift to...", "send to my [account]" | `add-transfer` |
| "how much did I spend" | `total-expense` or `stats summary` |
| "how much did I earn" | `total-income` |
| "show transactions", "what did I buy" | `transaction list` |
| "find that...", "search for..." | `transaction search` |
| "balance", "how much do I have" | `account balances` |
| "budget", "how much is left" | `budget view` |
| "who owes me", "splits" | `split unsettled` |
| "spending breakdown", "stats" | `stats summary` |

### Ambiguous cases — ASK:

- "Add 500 to savings" → Is this income received, or a transfer from another account?
- Amount mentioned but no type → "Is this an expense, income, or transfer?"
- Account unclear + user has multiple → "Which account?" (show list)
- Category unclear → Show matching categories and ask

---

## Transfers vs Expenses/Income

Transfers are fundamentally different:

| | Expense/Income | Transfer |
|---|---|---|
| Accounts | ONE account | TWO accounts (from + to) |
| Category | Required | **NEVER** (no `--category-id`) |
| Flags | `--account-name` | `--from-account-name` + `--to-account-name` |
| Trigger words | "spent", "earned" | "move", "transfer", "shift" |

**Before any transfer:** confirm both account names exist via `trako account list --raw`. If user only names one account, ask: "From which account, and to which?"

---

## Output Presentation

Never dump raw JSON to the user. Summarize naturally:

- **Transaction added:** "✅ Added ₹500 expense 'Groceries' to Cash (May 3)"
- **Balances:** One line per account: "Cash: ₹12,500 · Savings: ₹45,000 · Axis: -₹1,43,664"
- **Transaction list:** Brief table — Date | Name | Amount | Account
- **Budget:** Show categories with spend vs allocated: "FOOD: ₹3,200 / ₹5,000 (₹1,800 left)"
- **Totals:** "You spent ₹36,211 this month"
- **Splits:** "Rahul owes you ₹250 (from 'Dinner' on Apr 15)"
- **Errors:** Explain what went wrong and what to do next

Use ₹ for INR, $ for USD, € for EUR. Use Indian number formatting for INR (₹1,43,664).

---

## Guard Clauses

### Before creating a transaction:
1. Do you have the account name/ID? If not → `account list --raw`
2. Do you have the category name/ID? (skip for transfers) If not → `category list --raw`
3. Is the amount specified? If not → ask
4. Is the type clear (expense/income/transfer)? If not → ask

### Before deleting:
1. Show the transaction details to the user first
2. Get explicit confirmation
3. Warn about terminal prompt

### Before settling a split:
1. Show split details (who, amount, which transaction)
2. Get confirmation
3. Warn about terminal prompt

---

## Multi-step Workflows

### "I spent 500 on groceries, split with Rahul"
1. `account list --raw` + `category list --raw` (if not cached)
2. `add-expense` → note the returned transaction ID
3. `split create --transaction-id <ID> --user-id "rahul" --amount 250 --raw`

### "How much did I spend on food this month?"
1. `category list --raw` → find food category ID
2. `stats category-summary --category-id <ID> --range monthly --type DEBIT --raw`

### "Show my spending breakdown this month"
1. `stats summary --range monthly --type DEBIT --raw`
   - Note: stats `--type` uses `DEBIT` (not EXPENSE) and `CREDIT` (not INCOME)
   - Note: stats `--range` uses camelCase: `weekly`, `monthly`, `yearly`, `fiveYearly`, `tenYearly`, `custom`

### "Transfer 1000 from Savings to Cash"
1. `account list --raw` → confirm both exist
2. `add-transfer --amount 1000 --currency INR --from-account-name "Savings" --to-account-name "Cash" --raw`

---

## Known Issues

- `stats summary` may crash with a deserialization error (backend returns int, SDK expects string for transactionType). If it fails, fall back to `transaction summary` for totals.
- `transaction search` is not yet released. If it fails with "No such command", tell the user search isn't available yet and offer to list transactions by month instead.

---

## Limitations

- Cannot create new accounts or categories (only list existing ones)
- Cannot do currency conversion or exchange rate lookups
- Cannot batch-delete transactions
- Delete and settle require terminal confirmation — cannot be fully automated
- No `--yes` or `--force` flag exists for any destructive command
