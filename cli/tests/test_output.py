"""Tests for output helpers."""
import pytest
from io import StringIO
from unittest.mock import patch

from cli.core.output import (
    print_success,
    print_error,
    print_warning,
    print_info,
    create_table
)


def test_print_success():
    """Test success message printing."""
    with patch('cli.core.output.console.print') as mock_print:
        print_success("Operation completed")
        mock_print.assert_called_once()
        args = mock_print.call_args[0][0]
        assert "✓" in args
        assert "Operation completed" in args


def test_print_error():
    """Test error message printing."""
    with patch('cli.core.output.console.print') as mock_print:
        print_error("Something went wrong")
        mock_print.assert_called_once()
        args = mock_print.call_args[0][0]
        assert "✗" in args
        assert "Something went wrong" in args


def test_print_warning():
    """Test warning message printing."""
    with patch('cli.core.output.console.print') as mock_print:
        print_warning("Be careful")
        mock_print.assert_called_once()
        args = mock_print.call_args[0][0]
        assert "⚠" in args
        assert "Be careful" in args


def test_print_info():
    """Test info message printing."""
    with patch('cli.core.output.console.print') as mock_print:
        print_info("FYI")
        mock_print.assert_called_once()
        args = mock_print.call_args[0][0]
        assert "ℹ" in args
        assert "FYI" in args


def test_create_table():
    """Test table creation."""
    table = create_table(title="Test Table")
    assert table.title == "Test Table"
    assert table.show_header is True


def test_create_table_with_columns():
    """Test table with columns and rows."""
    table = create_table(title="Users")
    table.add_column("ID", justify="right")
    table.add_column("Name")
    table.add_row("1", "John Doe")
    
    assert len(table.columns) == 2
    assert len(table.rows) == 1
