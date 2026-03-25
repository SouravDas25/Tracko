"""Test configuration and fixtures for CLI tests."""
import json
import re

import pytest
from typer.testing import CliRunner

from cli.main import app


@pytest.fixture
def runner():
    """Provide a Typer CLI test runner that prints output for every invocation."""
    _runner = CliRunner()
    _original_invoke = _runner.invoke

    def _verbose_invoke(*args, **kwargs):
        result = _original_invoke(*args, **kwargs)
        print(f"Exit code: {result.exit_code}")
        print(f"Output: {result.output}")
        if result.exception:
            import traceback
            traceback.print_exception(type(result.exception), result.exception, result.exception.__traceback__)
        return result

    _runner.invoke = _verbose_invoke
    return _runner


@pytest.fixture
def mock_config():
    """Load real token from 'test' profile saved by run_cli_test.py.

    NOTE: This fixture is a no-op for CLI commands — the CLI reads config
    directly from disk via ~/.tracko-cli.json.  Kept for backward
    compatibility with other test files that reference it.
    """
    import os
    config_file = os.path.join(os.path.expanduser("~"), ".tracko-cli.json")
    if os.path.exists(config_file):
        with open(config_file) as f:
            cfg = json.load(f)
        token = cfg.get("profiles", {}).get("test", {}).get("token", "")
    else:
        token = ""
    return {
        "active_profile": "test",
        "profiles": {
            "test": {
                "base_url": "http://localhost:8080",
                "token": token
            }
        }
    }


def _extract_id_from_output(output: str) -> int:
    """Extract the 'id' field from JSON printed in CLI output."""
    match = re.search(r'"id"\s*:\s*(\d+)', output)
    if match:
        return int(match.group(1))
    raise ValueError(f"Could not extract id from output:\n{output}")


@pytest.fixture
def created_expense(runner):
    """Create an expense via CLI and return its ID."""
    result = runner.invoke(app, [
        "transaction", "add-expense",
        "--name", "FixtureExpense",
        "--amount", "42.0",
        "--account-name", "Cash",
        "--category-name", "FOOD",
        "--currency", "INR",
    ])
    assert result.exit_code == 0, f"Failed to create fixture expense: {result.output}"
    return _extract_id_from_output(result.output)


@pytest.fixture
def created_income(runner):
    """Create an income via CLI and return its ID."""
    result = runner.invoke(app, [
        "transaction", "add-income",
        "--name", "FixtureIncome",
        "--amount", "1000.0",
        "--account-name", "Cash",
        "--category-name", "INCOME",
        "--currency", "INR",
    ])
    assert result.exit_code == 0, f"Failed to create fixture income: {result.output}"
    return _extract_id_from_output(result.output)


@pytest.fixture
def transfer_target_account(runner):
    """Ensure a second account exists for transfer tests and return its name."""
    name = "TransferTarget"
    runner.invoke(app, ["account", "add", "--name", name, "--currency", "INR"])
    return name


@pytest.fixture
def created_transfer(runner, transfer_target_account):
    """Create a transfer via CLI and return its ID."""
    result = runner.invoke(app, [
        "transaction", "add-transfer",
        "--from-account-name", "Cash",
        "--to-account-name", transfer_target_account,
        "--amount", "75.0",
        "--name", "FixtureTransfer",
        "--currency", "INR",
    ])
    assert result.exit_code == 0, f"Failed to create fixture transfer: {result.output}"
    return _extract_id_from_output(result.output)


# --- Legacy sample data fixtures (used by other test files) ---

@pytest.fixture
def sample_account():
    """Sample account data."""
    return {"id": 1, "name": "Test Account", "currency": "USD", "balance": 1000.0}


@pytest.fixture
def sample_category():
    """Sample category data."""
    return {"id": 1, "name": "Food", "categoryType": "EXPENSE"}


@pytest.fixture
def sample_transaction():
    """Sample transaction data."""
    return {
        "id": 1, "name": "Lunch", "amount": 25.50,
        "transactionType": "EXPENSE", "date": "2024-01-15T12:00:00Z",
        "accountId": 1, "categoryId": 1,
    }


@pytest.fixture
def sample_contact():
    """Sample contact data."""
    return {"id": 1, "name": "John Doe", "phone": "1234567890", "email": "john@example.com"}
