"""Tests for health command."""
import pytest
from unittest.mock import patch, MagicMock
from typer.testing import CliRunner
from urllib3.exceptions import MaxRetryError, NewConnectionError

from cli.main import app


def test_health_check_success(runner, mock_config):
    """Test successful health check."""
    mock_client = MagicMock()
    mock_health_api = MagicMock()
    mock_health_api.health.return_value = {"status": "UP"}

    with patch('cli.commands.health.get_config_for_api', return_value=("http://localhost:8080", None)):
        with patch('cli.commands.health.get_api_client') as mock_get_client:
            mock_get_client.return_value.__enter__ = MagicMock(return_value=mock_client)
            mock_get_client.return_value.__exit__ = MagicMock(return_value=False)
            with patch('cli.commands.health.tracko_sdk.HealthApi', return_value=mock_health_api):
                result = runner.invoke(app, ["health", "check"])

                assert result.exit_code == 0
                assert "healthy" in result.stdout.lower()


def test_health_check_raw_output(runner, mock_config):
    """Test health check with raw JSON output."""
    mock_client = MagicMock()
    mock_health_api = MagicMock()
    mock_health_api.health.return_value = {"status": "UP", "version": "1.0.0"}

    with patch('cli.commands.health.get_config_for_api', return_value=("http://localhost:8080", None)):
        with patch('cli.commands.health.get_api_client') as mock_get_client:
            mock_get_client.return_value.__enter__ = MagicMock(return_value=mock_client)
            mock_get_client.return_value.__exit__ = MagicMock(return_value=False)
            with patch('cli.commands.health.tracko_sdk.HealthApi', return_value=mock_health_api):
                result = runner.invoke(app, ["health", "check", "--raw"])

                assert result.exit_code == 0
                assert "status" in result.stdout
                assert "UP" in result.stdout


def test_health_check_failure(runner, mock_config):
    """Test health check when API returns None."""
    mock_client = MagicMock()
    mock_health_api = MagicMock()
    mock_health_api.health.return_value = None

    with patch('cli.commands.health.get_config_for_api', return_value=("http://localhost:8080", None)):
        with patch('cli.commands.health.get_api_client') as mock_get_client:
            mock_get_client.return_value.__enter__ = MagicMock(return_value=mock_client)
            mock_get_client.return_value.__exit__ = MagicMock(return_value=False)
            with patch('cli.commands.health.tracko_sdk.HealthApi', return_value=mock_health_api):
                result = runner.invoke(app, ["health", "check"])

                assert result.exit_code == 1


def test_health_check_connection_error(runner, mock_config):
    """Test health check when API is unreachable."""
    with patch('cli.commands.health.get_config_for_api', return_value=("http://localhost:8080", None)):
        with patch('cli.commands.health.get_api_client') as mock_get_client:
            mock_get_client.return_value.__enter__ = MagicMock(side_effect=ConnectionError("Connection refused"))
            mock_get_client.return_value.__exit__ = MagicMock(return_value=False)

            result = runner.invoke(app, ["health", "check"])

            assert result.exit_code == 1
            assert "could not connect" in result.stdout.lower() or "could not connect" in (result.stderr or "").lower()
