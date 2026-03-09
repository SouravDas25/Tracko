"""Test configuration and fixtures for CLI tests."""
import pytest
from typer.testing import CliRunner
from unittest.mock import Mock, patch
import json

from cli.main import app


@pytest.fixture
def runner():
    """Provide a Typer CLI test runner."""
    return CliRunner()


@pytest.fixture
def mock_config():
    """Mock configuration with test profile."""
    return {
        "active_profile": "test",
        "profiles": {
            "test": {
                "base_url": "http://localhost:8080",
                "token": "test-token-123"
            }
        }
    }


@pytest.fixture
def mock_api_client():
    """Mock API client for testing."""
    with patch('cli.core.api.make_api_client') as mock:
        client = Mock()
        mock.return_value.__enter__ = Mock(return_value=client)
        mock.return_value.__exit__ = Mock(return_value=False)
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
        "categoryType": "EXPENSE",
        "userId": "test-user-id"
    }


@pytest.fixture
def sample_transaction():
    """Sample transaction data."""
    return {
        "id": 1,
        "name": "Lunch",
        "amount": 50.0,
        "transactionType": "EXPENSE",
        "date": "2026-03-09T12:00:00Z",
        "accountId": 1,
        "categoryId": 1
    }


@pytest.fixture
def sample_contact():
    """Sample contact data."""
    return {
        "id": 1,
        "name": "John Doe",
        "phoneNo": "1234567890",
        "email": "john@example.com"
    }
