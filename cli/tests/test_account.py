"""Tests for account commands."""
import pytest
from unittest.mock import patch, Mock
from typer.testing import CliRunner

from cli.main import app


def test_account_list_success(runner, mock_config, sample_account):
    """Test listing accounts successfully."""
    with patch('cli.commands.account.get_active_profile_config', return_value=mock_config["profiles"]["test"]):
        with patch('cli.commands.account.sdk_call_unwrapped') as mock_sdk:
            mock_sdk.return_value = [sample_account]
            
            result = runner.invoke(app, ["account", "list"])
            
            assert result.exit_code == 0
            assert "Test Account" in result.stdout
            assert "USD" in result.stdout


def test_account_list_empty(runner, mock_config):
    """Test listing accounts when none exist."""
    with patch('cli.commands.account.get_active_profile_config', return_value=mock_config["profiles"]["test"]):
        with patch('cli.commands.account.sdk_call_unwrapped') as mock_sdk:
            mock_sdk.return_value = []
            
            result = runner.invoke(app, ["account", "list"])
            
            assert result.exit_code == 0
            assert "No accounts found" in result.stdout


def test_account_list_raw(runner, mock_config, sample_account):
    """Test listing accounts with raw JSON output."""
    with patch('cli.commands.account.get_active_profile_config', return_value=mock_config["profiles"]["test"]):
        with patch('cli.commands.account.sdk_call_unwrapped') as mock_sdk:
            mock_sdk.return_value = [sample_account]
            
            result = runner.invoke(app, ["account", "list", "--raw"])
            
            assert result.exit_code == 0
            assert "Test Account" in result.stdout


def test_account_get_success(runner, mock_config, sample_account):
    """Test getting a specific account."""
    with patch('cli.commands.account.get_active_profile_config', return_value=mock_config["profiles"]["test"]):
        with patch('cli.commands.account.sdk_call_unwrapped') as mock_sdk:
            mock_sdk.return_value = sample_account
            
            result = runner.invoke(app, ["account", "get", "1"])
            
            assert result.exit_code == 0
            assert "Test Account" in result.stdout


def test_account_add_success(runner, mock_config, sample_account):
    """Test adding a new account."""
    with patch('cli.commands.account.get_active_profile_config', return_value=mock_config["profiles"]["test"]):
        with patch('cli.commands.account.sdk_call_unwrapped') as mock_sdk:
            mock_sdk.return_value = sample_account
            
            result = runner.invoke(app, ["account", "add", "--name", "Test Account", "--currency", "USD"])
            
            assert result.exit_code == 0
            assert "created successfully" in result.stdout


def test_account_delete_with_confirmation(runner, mock_config):
    """Test deleting an account with confirmation."""
    with patch('cli.commands.account.get_active_profile_config', return_value=mock_config["profiles"]["test"]):
        with patch('cli.commands.account.sdk_call_unwrapped') as mock_sdk:
            with patch('cli.commands.account.confirm', return_value=True):
                mock_sdk.return_value = {"message": "Deleted"}
                
                result = runner.invoke(app, ["account", "delete", "1"])
                
                assert result.exit_code == 0
                assert "deleted successfully" in result.stdout


def test_account_delete_cancelled(runner, mock_config):
    """Test cancelling account deletion."""
    with patch('cli.commands.account.get_active_profile_config', return_value=mock_config["profiles"]["test"]):
        with patch('cli.utils.prompts.Confirm.ask', return_value=False):
            result = runner.invoke(app, ["account", "delete", "1"])
            
            # When cancelled, should exit cleanly without calling API
            assert result.exit_code == 0
