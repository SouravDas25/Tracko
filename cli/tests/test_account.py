"""Tests for account commands with real backend integration."""
import pytest

from cli.main import app


def test_account_list_success(runner, mock_config):
    """Test listing accounts successfully."""
    result = runner.invoke(app, ["account", "list"])
    assert result.exit_code == 0
    assert "Cash" in result.stdout or "accounts" in result.stdout.lower()


def test_account_list_empty_or_populated(runner, mock_config):
    """Test listing accounts returns valid output."""
    result = runner.invoke(app, ["account", "list"])
    assert result.exit_code == 0
    assert "No accounts found" in result.stdout or "Accounts" in result.stdout


def test_account_list_raw(runner, mock_config):
    """Test listing accounts with raw JSON output."""
    result = runner.invoke(app, ["account", "list", "--raw"])
    assert result.exit_code == 0


def test_account_add_success(runner, mock_config):
    """Test adding a new account."""
    result = runner.invoke(app, ["account", "add", "--name", "TestAccount", "--currency", "USD"])
    assert result.exit_code == 0
    assert "created" in result.stdout.lower()


def test_account_delete_cancelled(runner, mock_config):
    """Test cancelling account deletion."""
    result = runner.invoke(app, ["account", "delete", "999999"], input="n\n")
    assert result.exit_code == 1
    assert "Cancelled" in result.stdout
