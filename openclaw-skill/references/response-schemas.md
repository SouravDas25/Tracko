# Trako CLI — Response Schemas

Example JSON outputs from `--raw` commands. Use these to parse and present results.

---

## account list

```json
[
  {"id": 1, "name": "Cash", "userId": "...", "currency": "INR", "balance": null},
  {"id": 4, "name": "Axis", "userId": "...", "currency": "INR", "balance": null}
]
```

Note: `balance` is always null here. Use `account balances` for actual balances.

---

## account balances

Returns a map of account ID → balance:

```json
{"1": -9054.42, "4": -143663.74, "6": 12601355.91}
```

Negative = net outflow. Cross-reference with `account list` to get names.

---

## category list

```json
[
  {"id": 2, "name": "FOOD", "userId": "...", "isRollOverEnabled": null, "parentCategoryId": null, "categoryType": "EXPENSE"},
  {"id": 24, "name": "salary", "userId": "...", "isRollOverEnabled": null, "parentCategoryId": null, "categoryType": "INCOME"}
]
```

`categoryType` is either `"EXPENSE"` or `"INCOME"`.

---

## transaction list

```json
{
  "month": 5,
  "year": 2026,
  "page": 0,
  "size": 20,
  "totalElements": 6,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false,
  "transactions": [
    {
      "id": 4335,
      "transactionType": 1,
      "name": "New phone",
      "comments": "New phone",
      "date": "2026-05-28 00:00:00+00:00",
      "amount": 4605.31,
      "originalCurrency": "INR",
      "originalAmount": 4605.31,
      "exchangeRate": 1.0,
      "accountId": 14,
      "categoryId": 41,
      "isCountable": 1,
      "category": null,
      "account": null,
      "splits": null
    }
  ]
}
```

`transactionType` values: `1` = DEBIT (expense), `2` = CREDIT (income), `3` = TRANSFER

---

## transaction summary

```json
{
  "totalIncome": 50000.0,
  "totalExpense": 36210.62,
  "netTotal": 13789.38,
  "rolloverNet": 4863008.35,
  "netTotalWithRollover": 4876797.73,
  "transactionCount": 6
}
```

---

## transaction total-expense / total-income

Returns a plain number (not JSON object):

```
36210.62
```

---

## budget view

```json
{
  "month": 5,
  "year": 2026,
  "totalBudget": 0.0,
  "totalIncome": 50000.0,
  "totalSpent": 36210.62,
  "rolloverAmount": 4863008.35,
  "availableToAssign": 4913008.35,
  "isClosed": false,
  "categories": [
    {
      "categoryId": 2,
      "categoryName": "FOOD",
      "allocatedAmount": 5000.0,
      "actualSpent": 3200.0,
      "remainingBalance": 1800.0
    }
  ]
}
```

`remainingBalance` = `allocatedAmount` - `actualSpent`. Negative means overspent.

---

## split unsettled

```json
[
  {
    "id": 12,
    "transactionId": 100,
    "userId": "user-uuid",
    "contactId": 3,
    "amount": 250.0,
    "settled": false,
    "createdAt": "2026-04-15T10:30:00"
  }
]
```

Empty array `[]` means no unsettled splits.

---

## stats summary

```json
{
  "range": "monthly",
  "transactionType": "DEBIT",
  "total": 36210.62,
  "series": [
    {"label": "May 2026", "value": 36210.62}
  ],
  "categories": [
    {"categoryName": "FOOD", "amount": 3200.0},
    {"categoryName": "RENT", "amount": 5000.0}
  ]
}
```

---

## Error responses

When a command fails, output looks like:

```
[ERROR] Bad request: Invalid input.
[ERROR] Details: <specific message>
```

Auth errors:
```
[ERROR] Unauthorized. Please run: trako auth login
```

Connection errors:
```
[ERROR] Could not connect to API. Is the server running?
```

---

## transaction search (upcoming)

```json
{
  "results": [
    {
      "id": 100,
      "name": "Restaurant dinner",
      "amount": 1500.0,
      "date": "2025-06-15",
      "relevanceScore": 0.92,
      "matchedFields": ["name"]
    }
  ],
  "totalResults": 3,
  "page": 0,
  "totalPages": 1,
  "searchTimeMs": 45
}
```
