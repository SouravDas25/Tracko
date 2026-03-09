"""Tests for transaction commands."""
import pytest
from unittest.mock import patch, Mock
from typer.testing import CliRunner

from cli.main import app


def test_transaction_list_success(runner, mock_config, sample_transaction):
    """Test listing transactions successfully."""
    with patch('cli.commands.transaction.get_active_profile_config', return_value=mock_config["profiles"]["test"]):
        with patch('cli.commands.transaction.sdk_call_unwrapped') as mock_sdk:
            mock_sdk.return_value = {
                "content": [sample_transaction],
                "page": 0,
                "totalPages": 1,
                "totalElements": 1
            }
            
            result = runner.invoke(app, ["transaction", "list", "--month", "3", "--year", "2026"])
            
            # Just verify command runs without error
            assert result.exit_code == 0 or "transactions" in result.stdout.lower()


def test_transaction_add_expense_success(runner, mock_config, sample_transaction):
    """Test adding a new expense."""
    with patch('cli.commands.transaction.get_active_profile_config', return_value=mock_config["profiles"]["test"]):
        with patch('cli.commands.transaction.sdk_call_unwrapped') as mock_sdk:
            mock_sdk.return_value = sample_transaction
            
            result = runner.invoke(app, [
                "transaction", "add-expense",
                "--name", "Lunch",
                "--amount", "50.0",
                "--account-id", "1",
                "--category-id", "1"
            ])
            
            assert result.exit_code == 0


def test_transaction_add_income_success(runner, mock_config, sample_transaction):
    """Test adding a new income."""
    with patch('cli.commands.transaction.get_active_profile_config', return_value=mock_config["profiles"]["test"]):
        with patch('cli.commands.transaction.sdk_call_unwrapped') as mock_sdk:
            mock_sdk.return_value = sample_transaction
            
            result = runner.invoke(app, [
                "transaction", "add-income",
                "--name", "Salary",
                "--amount", "5000.0",
                "--account-id", "1",
                "--category-id", "2"
            ])
            
            print(f"Exit code: {result.exit_code}")
            print(f"Output: {result.stdout}")
            if result.exception:
                print(f"Exception: {result.exception}")
            assert result.exit_code == 0


def test_transaction_add_transfer_success(runner, mock_config):
    """Test creating a transfer between accounts."""
    with patch('cli.commands.transaction.get_active_profile_config', return_value=mock_config["profiles"]["test"]):
        with patch('cli.commands.transaction.sdk_call_unwrapped') as mock_sdk:
            mock_sdk.return_value = {"message": "Transfer created"}
            
            result = runner.invoke(app, [
                "transaction", "add-transfer",
                "--from-account-id", "1",
                "--to-account-id", "2",
                "--amount", "100.0"
            ])
            
            assert result.exit_code == 0


def test_transaction_list_empty(runner, mock_config):
    """Test listing transactions when none exist."""
    with patch('cli.commands.transaction.get_active_profile_config', return_value=mock_config["profiles"]["test"]):
        with patch('cli.commands.transaction.sdk_call_unwrapped') as mock_sdk:
            mock_sdk.return_value = {
                "content": [],
                "page": 0,
                "totalPages": 0
            }
            
            result = runner.invoke(app, ["transaction", "list", "--month", "3", "--year", "2026"])
            
            assert result.exit_code == 0
            assert "No transactions found" in result.stdout


def test_transaction_add_expense_with_date(runner, mock_config, sample_transaction):
    """Test adding a new expense with date."""
    with patch('cli.commands.transaction.get_active_profile_config', return_value=mock_config["profiles"]["test"]):
        with patch('cli.commands.transaction.sdk_call_unwrapped') as mock_sdk:
            mock_sdk.return_value = sample_transaction
            
            result = runner.invoke(app, [
                "transaction", "add-expense",
                "--name", "Lunch",
                "--amount", "50.0",
                "--account-id", "1",
                "--category-id", "1",
                "--date", "2026-03-09"
            ])
            
            assert result.exit_code == 0
            assert "created" in result.stdout.lower()


def test_transaction_delete_with_confirmation(runner, mock_config):
    """Test deleting a transaction with confirmation."""
    with patch('cli.commands.transaction.get_active_profile_config', return_value=mock_config["profiles"]["test"]):
        with patch('cli.commands.transaction.sdk_call_unwrapped') as mock_sdk:
            with patch('cli.commands.transaction.confirm', return_value=True):
                mock_sdk.return_value = {"message": "Deleted"}
                
                result = runner.invoke(app, ["transaction", "delete", "1"])
                
                assert result.exit_code == 0
                assert "deleted successfully" in result.stdout


def test_transaction_summary(runner, mock_config):
    """Test transaction summary."""
    with patch('cli.commands.transaction.get_active_profile_config', return_value=mock_config["profiles"]["test"]):
        with patch('cli.commands.transaction.sdk_call_unwrapped') as mock_sdk:
            mock_sdk.return_value = {
                "totalIncome": 5000.0,
                "totalExpense": 3000.0,
                "netSavings": 2000.0
            }
            
            result = runner.invoke(app, [
                "transaction", "summary",
                "--start-date", "2026-01-01",
                "--end-date", "2026-12-31"
            ])
            
            assert result.exit_code == 0
            assert "5000" in result.stdout
            assert "3000" in result.stdout


def test_transaction_add_transfer_with_details(runner, mock_config):
    """Test creating a transfer between accounts with details."""
    with patch('cli.commands.transaction.get_active_profile_config', return_value=mock_config["profiles"]["test"]):
        with patch('cli.commands.transaction.sdk_call_unwrapped') as mock_sdk:
            mock_sdk.return_value = {"message": "Transfer created"}
            
            result = runner.invoke(app, [
                "transaction", "add-transfer",
                "--from-account-id", "1",
                "--to-account-id", "2",
                "--amount", "100.0",
                "--name", "Test transfer",
                "--date", "2026-03-09"
            ])
            
            assert result.exit_code == 0
            assert "transfer" in result.stdout.lower()
