"""Tests for prompt utilities."""
import pytest
from unittest.mock import patch

from cli.utils.prompts import confirm, prompt


def test_confirm_yes():
    """Test confirmation with yes response."""
    with patch('cli.utils.prompts.Confirm.ask', return_value=True):
        result = confirm("Are you sure?")
        assert result is True


def test_confirm_no():
    """Test confirmation with no response."""
    with patch('cli.utils.prompts.Confirm.ask', return_value=False):
        result = confirm("Are you sure?")
        assert result is False


def test_confirm_with_default():
    """Test confirmation with default value."""
    with patch('cli.utils.prompts.Confirm.ask', return_value=True):
        result = confirm("Continue?", default=True)
        assert result is True


def test_prompt():
    """Test prompting for input."""
    with patch('cli.utils.prompts.Prompt.ask', return_value="test input"):
        result = prompt("Enter value")
        assert result == "test input"


def test_prompt_password():
    """Test password prompting."""
    with patch('cli.utils.prompts.Prompt.ask', return_value="secret123"):
        password = prompt("Enter password", password=True)
        assert password == "secret123"


def test_prompt_with_default():
    """Test prompt with default value."""
    with patch('cli.utils.prompts.Prompt.ask', return_value="default"):
        result = prompt("Enter value", default="default")
        assert result == "default"

