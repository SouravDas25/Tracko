"""Tests for health command."""
import pytest
from unittest.mock import patch, Mock
from typer.testing import CliRunner

from cli.main import app


def test_health_check_success(runner, mock_config):
    """Test successful health check."""
    with patch('cli.commands.health.get_active_profile_config', return_value=mock_config["profiles"]["test"]):
        with patch('cli.commands.health.sdk_call_unwrapped') as mock_sdk:
            mock_sdk.return_value = {"status": "UP"}
            
            result = runner.invoke(app, ["health", "check"])
            
            assert result.exit_code == 0
            assert "healthy" in result.stdout.lower()


def test_health_check_raw_output(runner, mock_config):
    """Test health check with raw JSON output."""
    with patch('cli.commands.health.get_active_profile_config', return_value=mock_config["profiles"]["test"]):
        with patch('cli.commands.health.sdk_call_unwrapped') as mock_sdk:
            mock_sdk.return_value = {"status": "UP", "version": "1.0.0"}
            
            result = runner.invoke(app, ["health", "check", "--raw"])
            
            assert result.exit_code == 0
            assert "status" in result.stdout
            assert "UP" in result.stdout


def test_health_check_failure(runner, mock_config):
    """Test health check when API is down."""
    with patch('cli.commands.health.get_active_profile_config', return_value=mock_config["profiles"]["test"]):
        with patch('cli.commands.health.sdk_call_unwrapped') as mock_sdk:
            mock_sdk.return_value = None
            
            result = runner.invoke(app, ["health", "check"])
            
            assert result.exit_code == 1
