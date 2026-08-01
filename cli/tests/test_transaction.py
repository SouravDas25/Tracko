"""Tests for transaction commands with real backend integration.

Assumes seeded data: Cash account, categories (FOOD, INCOME, TRANSFER, etc.)
Run via: python cli/run_cli_test.py  (starts backend, logs in, runs pytest)
"""
import json
import re
import tempfile

import pytest

from cli.main import app


# ---------------------------------------------------------------------------
# list
# ---------------------------------------------------------------------------

def test_transaction_list_success(runner, created_expense):
    """After creating a transaction, its name appears in the list output."""
    result = runner.invoke(app, ["transaction", "list", "--month", "3", "--year", "2026"])
    assert result.exit_code == 0
    assert "Transactions" in result.stdout
    assert "FixtureExpense" in result.stdout


def test_transaction_list_empty(runner):
    """Listing a month with no data returns a valid response."""
    result = runner.invoke(app, ["transaction", "list", "--month", "1", "--year", "2000"])
    assert result.exit_code == 0
    assert "No transactions found" in result.stdout


def test_transaction_list_raw(runner, created_expense):
    """Raw flag produces valid JSON output."""
    result = runner.invoke(app, ["transaction", "list", "--month", "3", "--year", "2026", "--raw"])
    assert result.exit_code == 0
    # Output should contain JSON with transactions key
    assert "transactions" in result.stdout.lower()


# ---------------------------------------------------------------------------
# add-expense / add-income / add-transfer
# ---------------------------------------------------------------------------

def test_transaction_add_expense_success(runner):
    """Adding an expense prints the name and a success message."""
    result = runner.invoke(app, [
        "transaction", "add-expense",
        "--name", "Test Lunch",
        "--amount", "50.0",
        "--account-name", "Cash",
        "--category-name", "FOOD",
        "--currency", "INR",
    ])
    assert result.exit_code == 0
    assert "Test Lunch" in result.stdout
    assert "created" in result.stdout.lower()


def test_transaction_add_income_success(runner):
    """Adding an income prints the name and a success message."""
    result = runner.invoke(app, [
        "transaction", "add-income",
        "--name", "Test Salary",
        "--amount", "5000.0",
        "--account-name", "Cash",
        "--category-name", "INCOME",
        "--currency", "INR",
    ])
    assert result.exit_code == 0
    assert "Test Salary" in result.stdout
    assert "created" in result.stdout.lower()


def test_transaction_add_transfer_success(runner, transfer_target_account):
    """Creating a transfer prints a success message with the amount."""
    result = runner.invoke(app, [
        "transaction", "add-transfer",
        "--from-account-name", "Cash",
        "--to-account-name", transfer_target_account,
        "--amount", "100.0",
        "--name", "Test transfer",
        "--currency", "INR",
    ])
    assert result.exit_code == 0
    assert "100" in result.stdout
    assert "transfer" in result.stdout.lower()


def test_transaction_add_transfer_honors_explicit_exchange_rate(runner, transfer_target_account):
    """An explicit --exchange-rate is stored verbatim instead of a live provider quote.

    Mirrors a remittance booked at a contracted rate: without the flag the backend falls back
    to the live mid-market quote, which will not match what the transfer actually settled at.
    """
    result = runner.invoke(app, [
        "transaction", "add-transfer",
        "--from-account-name", "Cash",
        "--to-account-name", transfer_target_account,
        "--amount", "3000.0",
        "--name", "Instarem",
        "--currency", "EUR",
        "--exchange-rate", "109.78",
        "--raw",
    ])
    assert result.exit_code == 0
    match = re.search(r'"exchangeRate"\s*:\s*([\d.]+)', result.output)
    assert match, f"exchangeRate not found in output:\n{result.output}"
    assert float(match.group(1)) == pytest.approx(109.78)


# ---------------------------------------------------------------------------
# get
# ---------------------------------------------------------------------------

def test_transaction_get(runner, created_expense):
    """Fetching a transaction by ID returns its details."""
    result = runner.invoke(app, ["transaction", "get", str(created_expense)])
    assert result.exit_code == 0
    assert "FixtureExpense" in result.stdout


def test_transaction_get_nonexistent(runner):
    """Fetching a non-existent transaction returns an error."""
    result = runner.invoke(app, ["transaction", "get", "999999"])
    assert result.exit_code != 0


# ---------------------------------------------------------------------------
# update-expense / update-income / update-transfer
# ---------------------------------------------------------------------------

def test_transaction_update_expense(runner, created_expense):
    """Updating an expense name prints a success message."""
    result = runner.invoke(app, [
        "transaction", "update-expense", str(created_expense),
        "--name", "Updated Lunch",
    ])
    assert result.exit_code == 0
    assert "updated" in result.stdout.lower()
    assert "Updated Lunch" in result.stdout


def test_transaction_update_income(runner, created_income):
    """Updating an income amount prints a success message."""
    result = runner.invoke(app, [
        "transaction", "update-income", str(created_income),
        "--amount", "2000.0",
    ])
    assert result.exit_code == 0
    assert "updated" in result.stdout.lower()


def test_transaction_update_transfer(runner, created_transfer):
    """Updating a transfer name prints a success message."""
    result = runner.invoke(app, [
        "transaction", "update-transfer", str(created_transfer),
        "--name", "Updated Transfer",
    ])
    assert result.exit_code == 0
    assert "updated" in result.stdout.lower()


def test_transaction_update_transfer_re_rates_without_touching_currency(runner, created_transfer):
    """--exchange-rate alone re-rates an existing transfer to the given value.

    This is the repair path for transfers written with a wrong rate: omitting --currency
    keeps the backend off the live provider, so the supplied rate is stored verbatim.
    """
    result = runner.invoke(app, [
        "transaction", "update-transfer", str(created_transfer),
        "--exchange-rate", "109.78",
        "--raw",
    ])
    assert result.exit_code == 0
    match = re.search(r'"exchangeRate"\s*:\s*([\d.]+)', result.output)
    assert match, f"exchangeRate not found in output:\n{result.output}"
    assert float(match.group(1)) == pytest.approx(109.78)


def test_transaction_update_transfer_applies_amount(runner, created_transfer):
    """--amount actually changes the stored amount.

    Regression test: the request was previously built with an `amount=` keyword, which is not
    a field on the SDK model and was silently dropped, so the command reported success while
    changing nothing.
    """
    result = runner.invoke(app, [
        "transaction", "update-transfer", str(created_transfer),
        "--amount", "500.0",
        "--raw",
    ])
    assert result.exit_code == 0
    match = re.search(r'"originalAmount"\s*:\s*([\d.]+)', result.output)
    assert match, f"originalAmount not found in output:\n{result.output}"
    assert float(match.group(1)) == pytest.approx(500.0)


# ---------------------------------------------------------------------------
# delete
# ---------------------------------------------------------------------------

def test_transaction_delete_confirmed(runner, created_expense):
    """Deleting a real transaction with 'y' confirmation succeeds."""
    result = runner.invoke(app, ["transaction", "delete", str(created_expense)], input="y\n")
    assert result.exit_code == 0
    assert "deleted" in result.stdout.lower()


def test_transaction_delete_cancelled(runner, created_expense):
    """Declining deletion prints 'Cancelled' and exits with code 1."""
    result = runner.invoke(app, ["transaction", "delete", str(created_expense)], input="n\n")
    assert result.exit_code == 1
    assert "Cancelled" in result.stdout


# ---------------------------------------------------------------------------
# summary / total-income / total-expense
# ---------------------------------------------------------------------------

def test_transaction_summary(runner):
    """Summary returns valid JSON output for a date range."""
    result = runner.invoke(app, [
        "transaction", "summary",
        "--start-date", "2026-01-01",
        "--end-date", "2026-12-31",
    ])
    assert result.exit_code == 0
    assert "totalIncome" in result.stdout


def test_transaction_total_income(runner):
    """Total income returns valid JSON output."""
    result = runner.invoke(app, [
        "transaction", "total-income",
        "--start-date", "2026-01-01",
        "--end-date", "2026-12-31",
    ])
    assert result.exit_code == 0
    assert result.stdout.strip()


def test_transaction_total_expense(runner):
    """Total expense returns valid JSON output."""
    result = runner.invoke(app, [
        "transaction", "total-expense",
        "--start-date", "2026-01-01",
        "--end-date", "2026-12-31",
    ])
    assert result.exit_code == 0
    assert result.stdout.strip()


# ---------------------------------------------------------------------------
# csv-template / import-csv
# ---------------------------------------------------------------------------

def test_transaction_csv_template(runner):
    """CSV template prints the expected header columns."""
    result = runner.invoke(app, ["transaction", "csv-template"])
    assert result.exit_code == 0
    assert "date" in result.stdout
    assert "amount" in result.stdout
    assert "category" in result.stdout
    assert "account" in result.stdout
    assert "currency" in result.stdout
    assert "name" in result.stdout


def test_transaction_import_csv(runner, tmp_path):
    """Importing a valid CSV file succeeds."""
    csv_file = tmp_path / "import.csv"
    csv_file.write_text("date,amount,type,category,account,currency,name,comments\n2026-03-01,25.0,expense,FOOD,Cash,INR,CSVLunch,test\n")

    result = runner.invoke(app, [
        "transaction", "import-csv",
        "--file", str(csv_file),
    ])
    assert result.exit_code == 0
    assert "imported" in result.stdout.lower()


# ---------------------------------------------------------------------------
# Error paths
# ---------------------------------------------------------------------------

def test_transaction_add_expense_missing_account(runner):
    """Omitting both account-id and account-name produces an error."""
    result = runner.invoke(app, [
        "transaction", "add-expense",
        "--name", "Bad",
        "--amount", "10.0",
        "--category-name", "FOOD",
        "--currency", "INR",
    ])
    assert result.exit_code != 0
    assert "account" in result.stdout.lower()


def test_transaction_add_expense_invalid_category(runner):
    """Using a non-existent category name produces an error."""
    result = runner.invoke(app, [
        "transaction", "add-expense",
        "--name", "Bad",
        "--amount", "10.0",
        "--account-name", "Cash",
        "--category-name", "NONEXISTENT_CATEGORY",
        "--currency", "INR",
    ])
    assert result.exit_code != 0
    assert "not found" in result.stdout.lower()


# ---------------------------------------------------------------------------
# search
# ---------------------------------------------------------------------------

def test_transaction_search_success(runner):
    """Search returns a table with matching results."""
    result = runner.invoke(app, ["transaction", "search", "Lunch"])
    assert result.exit_code == 0
    assert "Search Results" in result.stdout or "No results found" in result.stdout


def test_transaction_search_empty(runner):
    """Search with no matches shows 'No results found'."""
    result = runner.invoke(app, ["transaction", "search", "xyznonexistent999abc"])
    assert result.exit_code == 0
    assert "No results found" in result.stdout


def test_transaction_search_raw(runner):
    """--raw flag outputs JSON containing 'results' key."""
    result = runner.invoke(app, ["transaction", "search", "Lunch", "--raw"])
    assert result.exit_code == 0
    assert "results" in result.stdout.lower()


def test_transaction_search_with_filters(runner):
    """Search with date and amount filters returns exit code 0."""
    result = runner.invoke(app, [
        "transaction", "search", "Lunch",
        "--start-date", "2026-01-01",
        "--end-date", "2026-12-31",
        "--min-amount", "1",
    ])
    assert result.exit_code == 0


def test_transaction_search_pagination(runner):
    """Search with --page and --size options returns exit code 0."""
    result = runner.invoke(app, [
        "transaction", "search", "Lunch",
        "--page", "0",
        "--size", "5",
    ])
    assert result.exit_code == 0


# ---------------------------------------------------------------------------
# history
# ---------------------------------------------------------------------------

def test_transaction_history_renders(runner, created_expense):
    """A transaction's history table renders after it has been edited."""
    runner.invoke(app, [
        "transaction", "update-expense", str(created_expense),
        "--name", "HistoryEdited",
    ])
    result = runner.invoke(app, ["transaction", "history", str(created_expense)])
    assert result.exit_code == 0
    assert f"History for transaction {created_expense}" in result.stdout


def test_transaction_history_raw(runner, created_expense):
    """--raw history output is produced without error."""
    result = runner.invoke(app, ["transaction", "history", str(created_expense), "--raw"])
    assert result.exit_code == 0


def test_transaction_history_none(runner):
    """History for a non-existent transaction reports no history (exit 0)."""
    result = runner.invoke(app, ["transaction", "history", "999999"])
    assert result.exit_code == 0
    assert "No history found" in result.stdout


# ---------------------------------------------------------------------------
# trash (recycle bin)
# ---------------------------------------------------------------------------

def test_transaction_trash_lists_deleted(runner, created_expense):
    """A deleted transaction shows up in the recycle-bin listing."""
    runner.invoke(app, ["transaction", "delete", str(created_expense)], input="y\n")
    result = runner.invoke(app, ["transaction", "trash"])
    assert result.exit_code == 0
    assert "Recycle Bin" in result.stdout


def test_transaction_trash_raw(runner):
    """--raw recycle-bin output is produced without error."""
    result = runner.invoke(app, ["transaction", "trash", "--raw"])
    assert result.exit_code == 0


# ---------------------------------------------------------------------------
# revert
# ---------------------------------------------------------------------------

def test_transaction_revert_cancelled(runner):
    """Declining the revert confirmation prints 'Cancelled' and exits 1."""
    result = runner.invoke(app, ["transaction", "revert", "1"], input="n\n")
    assert result.exit_code == 1
    assert "Cancelled" in result.stdout


def test_transaction_revert_nonexistent(runner):
    """Reverting a non-existent history entry returns an error."""
    result = runner.invoke(app, ["transaction", "revert", "999999"], input="y\n")
    assert result.exit_code != 0


def test_transaction_revert_restores_deleted(runner, created_expense):
    """Full round-trip: delete a transaction, then revert its DELETE entry to restore it."""
    txn_id = created_expense

    # Delete → moves to the recycle bin (hard-deletes the row).
    deleted = runner.invoke(app, ["transaction", "delete", str(txn_id)], input="y\n")
    assert deleted.exit_code == 0
    # The transaction row is gone.
    assert runner.invoke(app, ["transaction", "get", str(txn_id)]).exit_code != 0

    # The history is still addressable by transaction id; grab the most recent
    # entry (the DELETE) so we can restore it.
    hist = runner.invoke(app, ["transaction", "history", str(txn_id), "--raw"])
    assert hist.exit_code == 0
    match = re.search(r'(?<!\w)id["\s]*[=:]\s*(\d+)', hist.stdout)
    assert match, f"Could not find a history id in output:\n{hist.stdout}"
    history_id = match.group(1)

    # Revert the delete → restores the transaction with its original id.
    reverted = runner.invoke(app, ["transaction", "revert", history_id], input="y\n")
    assert reverted.exit_code == 0
    assert "Reverted" in reverted.stdout and "successfully" in reverted.stdout

    # It is back, under the same id.
    restored = runner.invoke(app, ["transaction", "get", str(txn_id)])
    assert restored.exit_code == 0
    assert "FixtureExpense" in restored.stdout
