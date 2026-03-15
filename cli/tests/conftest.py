"""Test configuration and fixtures for CLI tests."""
import pytest
from typer.testing import CliRunner
import json

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
    """Load real token from 'test' profile saved by run_cli_test.py."""
    import json, os
    config_file = os.path.join(os.getcwd(), ".tracko-cli.json")
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


@pytest.fixture
def sample_account():
    """Sample account data."""
    return {
        "id": 1,
        "name": "Test Account",
        "currency": "USD",
        "balance": 1000.0
    }


@pytest.fixture
def sample_category():
    """Sample category data."""
    return {
        "id": 1,
        "name": "Food",
        "categoryType": "EXPENSE"
    }


@pytest.fixture
def sample_transaction():
    """Sample transaction data."""
    return {
        "id": 1,
        "name": "Lunch",
        "amount": 25.50,
        "transactionType": "EXPENSE",
        "date": "2024-01-15T12:00:00Z",
        "accountId": 1,
        "categoryId": 1
    }


@pytest.fixture
def sample_contact():
    """Sample contact data."""
    return {
        "id": 1,
        "name": "John Doe",
        "phone": "1234567890",
        "email": "john@example.com"
    }
