"""Test configuration and fixtures for CLI tests."""
import pytest
from typer.testing import CliRunner
from unittest.mock import Mock, patch
import json

@pytest.fixture
def runner():
    """Provide a Typer CLI test runner."""
    return CliRunner()


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
def mock_api_client():
    """Mock API client for testing."""
    with patch('cli.core.api.make_api_client') as mock:
        client = Mock()
        mock.return_value.__enter__.return_value = client
        mock.return_value.__exit__.return_value = None
        yield client


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
