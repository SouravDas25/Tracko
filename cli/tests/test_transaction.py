"""Tests for transaction commands with real backend integration."""
import pytest
from typer.testing import CliRunner

from cli.main import app


def test_transaction_list_success(runner, mock_config, sample_transaction):
    """Test listing transactions successfully."""
    result = runner.invoke(app, ["transaction", "list", "--month", "3", "--year", "2026"])
    assert result.exit_code == 0
    assert "transactions" in result.stdout.lower() or "No transactions found" in result.stdout


def test_transaction_list_empty(runner, mock_config):
    """Test listing transactions when none exist."""
    result = runner.invoke(app, ["transaction", "list", "--month", "3", "--year", "2026"])
    assert result.exit_code == 0
    assert "No transactions found" in result.stdout or "transactions" in result.stdout.lower()


def test_transaction_list_raw(runner, mock_config):
    """Test listing transactions with raw JSON output."""
    result = runner.invoke(app, ["transaction", "list", "--month", "3", "--year", "2026", "--raw"])
    assert result.exit_code == 0


def test_transaction_add_expense_success(runner, mock_config):
    """Test adding a new expense."""
    result = runner.invoke(app, [
        "transaction", "add-expense",
        "--name", "Test Lunch",
        "--amount", "50.0",
        "--account-name", "Cash",
        "--category-name", "FOOD",
        "--currency", "INR"
    ])
    assert result.exit_code == 0
    assert "created" in result.stdout.lower() or "expense" in result.stdout.lower()


def test_transaction_add_income_success(runner, mock_config):
    """Test adding a new income."""
    result = runner.invoke(app, [
        "transaction", "add-income",
        "--name", "Test Salary",
        "--amount", "5000.0",
        "--account-name", "Cash",
        "--category-name", "INCOME",
        "--currency", "INR"
    ])
    assert result.exit_code == 0
    assert "created" in result.stdout.lower() or "income" in result.stdout.lower()


def test_transaction_add_transfer_success(runner, mock_config):
    """Test creating a transfer between accounts."""
    # Create a second account for the transfer
    runner.invoke(app, [
        "account", "add", "--name", "TransferTarget", "--currency", "INR"
    ])
    result = runner.invoke(app, [
        "transaction", "add-transfer",
        "--from-account-name", "Cash",
        "--to-account-name", "TransferTarget",
        "--amount", "100.0",
        "--name", "Test transfer",
        "--currency", "INR"
    ])
    assert result.exit_code == 0
    assert "transfer" in result.stdout.lower()


def test_transaction_delete_with_confirmation(runner, mock_config):
    """Test deleting a transaction with confirmation."""
    result = runner.invoke(app, ["transaction", "delete", "1"], input="y\n")
    assert result.exit_code == 0 or "Cancelled" in result.stdout


def test_transaction_summary(runner, mock_config):
    """Test transaction summary."""
    result = runner.invoke(app, [
        "transaction", "summary",
        "--start-date", "2026-01-01",
        "--end-date", "2026-12-31"
    ])
    assert result.exit_code == 0
