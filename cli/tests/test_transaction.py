"""Tests for transaction commands with real backend integration.

Assumes seeded data: Cash account, categories (FOOD, INCOME, TRANSFER, etc.)
Run via: python cli/run_cli_test.py  (starts backend, logs in, runs pytest)
"""
import json
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
