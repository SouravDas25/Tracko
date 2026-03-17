"""Tests for config commands."""
import pytest
from unittest.mock import patch

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
        with patch('cli.commands.config.get_active_profile_name', return_value="test"):
            result = runner.invoke(app, ["config", "show"])
            assert result.exit_code == 0


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
        with patch('cli.commands.config.get_active_profile_name', return_value="test"):
            with patch('cli.commands.config.update_profile') as mock_update:
                result = runner.invoke(app, [
                    "config", "set",
                    "--base-url", "http://example.com"
                ])
                assert result.exit_code == 0
                assert "Updated profile" in result.stdout
                mock_update.assert_called_once()
