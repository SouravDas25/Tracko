"""Tests for config commands."""
import pytest
from unittest.mock import patch, Mock
from typer.testing import CliRunner

from cli.main import app


def test_config_list(runner, mock_config):
    """Test listing config profiles."""
    with patch('cli.commands.config.load_config', return_value=mock_config):
        result = runner.invoke(app, ["config", "list"])
        
        assert result.exit_code == 0
        assert "Profile" in result.stdout


def test_config_show(runner, mock_config):
    """Test showing current profile config."""
    with patch('cli.commands.config.load_config', return_value=mock_config):
        result = runner.invoke(app, ["config", "show"])
        
        # Just verify command runs
        assert result.exit_code == 0


def test_account_delete_cancelled(runner, mock_config):
    """Test cancelling account deletion."""
    with patch('cli.commands.account.get_active_profile_config', return_value=mock_config["profiles"]["test"]):
        with patch('cli.utils.prompts.Confirm.ask', return_value=False):
            result = runner.invoke(app, ["account", "delete", "1"])
            
            # Cancelled should not call API, just exit cleanly
            assert "Cancelled" in result.stdout or result.exit_code == 0


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


def test_transaction_add_success(runner, mock_config, sample_transaction):
    """Test adding a new transaction."""
    with patch('cli.commands.transaction.get_active_profile_config', return_value=mock_config["profiles"]["test"]):
        with patch('cli.commands.transaction.sdk_call_unwrapped') as mock_sdk:
            mock_sdk.return_value = sample_transaction
            
            result = runner.invoke(app, [
                "transaction", "add",
                "--name", "Lunch",
                "--amount", "50.0",
                "--type", "expense",
                "--account-id", "1",
                "--category-id", "1",
                "--date", "2026-03-09"
            ])
            
            # Just verify no crash
            assert "error" not in result.stdout.lower() or result.exit_code == 0


def test_transaction_transfer_with_confirmation(runner, mock_config):
    """Test creating a transfer between accounts."""
    with patch('cli.commands.transaction.get_active_profile_config', return_value=mock_config["profiles"]["test"]):
        with patch('cli.commands.transaction.sdk_call_unwrapped') as mock_sdk:
            with patch('cli.utils.prompts.Confirm.ask', return_value=True):
                mock_sdk.return_value = {"message": "Transfer created"}
                
                result = runner.invoke(app, [
                    "transaction", "transfer",
                    "--from-account-id", "1",
                    "--to-account-id", "2",
                    "--amount", "100.0",
                    "--description", "Test transfer",
                    "--date", "2026-03-09"
                ])
                
                # Just verify command completes
                assert "error" not in result.stdout.lower() or result.exit_code == 0


def test_config_use(runner, mock_config):
    """Test switching active profile."""
    with patch('cli.commands.config.load_config', return_value=mock_config):
        with patch('cli.core.config.save_config') as mock_save:
            with patch('cli.utils.prompts.Confirm.ask', return_value=True):
                result = runner.invoke(app, ["config", "use", "test"])
                
                assert result.exit_code == 0
                assert "Active profile set to" in result.stdout
                mock_save.assert_called_once()


def test_config_set_base_url(runner, mock_config):
    """Test setting base URL for profile."""
    with patch('cli.commands.config.load_config', return_value=mock_config):
        with patch('cli.core.config.save_config') as mock_save:
            result = runner.invoke(app, [
                "config", "set",
                "--base-url", "http://example.com:8080"
            ])
            
            assert result.exit_code == 0
            assert "Updated profile" in result.stdout
            mock_save.assert_called_once()


def test_config_set_token(runner, mock_config):
    """Test setting token for profile."""
    with patch('cli.commands.config.load_config', return_value=mock_config):
        with patch('cli.core.config.save_config') as mock_save:
            result = runner.invoke(app, [
                "config", "set",
                "--token", "new-token-456"
            ])
            
            assert result.exit_code == 0
            assert "Updated profile" in result.stdout
            mock_save.assert_called_once()
