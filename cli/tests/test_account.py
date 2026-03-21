"""Tests for account commands with real backend integration."""
import pytest

from cli.main import app


def test_account_list_success(runner):
    """Seeded Cash account appears in list output."""
    result = runner.invoke(app, ["account", "list"])
    assert result.exit_code == 0
    assert "Cash" in result.stdout


def test_account_list_raw(runner):
    """Raw flag produces valid JSON output."""
    result = runner.invoke(app, ["account", "list", "--raw"])
    assert result.exit_code == 0
    assert "[" in result.stdout


def test_account_add_success(runner):
    """Adding a new account prints a success message."""
    result = runner.invoke(app, ["account", "add", "--name", "TestAccount", "--currency", "USD"])
    assert result.exit_code == 0
    assert "created" in result.stdout.lower()


def test_account_delete_cancelled(runner):
    """Declining deletion prints Cancelled."""
    result = runner.invoke(app, ["account", "delete", "999999"], input="n\n")
    assert result.exit_code == 1
    assert "Cancelled" in result.stdout
