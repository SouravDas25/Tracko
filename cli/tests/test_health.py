"""Tests for health command with real backend integration."""
import pytest

from cli.main import app


def test_health_check_success(runner):
    """Health check against running backend succeeds."""
    result = runner.invoke(app, ["health", "check"])
    assert result.exit_code == 0
    assert "healthy" in result.stdout.lower()


def test_health_check_raw_output(runner):
    """Health check with --raw returns JSON with status."""
    result = runner.invoke(app, ["health", "check", "--raw"])
    assert result.exit_code == 0
    assert "status" in result.stdout
