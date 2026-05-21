import pytest
from pathlib import Path
from src.input_manager import InputManager

def test_load_ids_valid_csv(tmp_path):
    csv_content = "ID\n101\n102\n103\n101\n"
    csv_file = tmp_path / "test.csv"
    csv_file.write_text(csv_content)

    manager = InputManager()
    ids = manager.load_ids(csv_file)

    assert ids == ["101", "102", "103"]

def test_load_ids_different_header(tmp_path):
    csv_content = "IDENTIFIER,COMMENT\n101,test\n102,test2\n"
    csv_file = tmp_path / "test_header.csv"
    csv_file.write_text(csv_content)

    manager = InputManager()
    ids = manager.load_ids(csv_file)

    assert ids == ["101", "102"]

def test_load_ids_no_header_skips_first_row(tmp_path):
    # Now it ALWAYS skips the first line, so 201 will be skipped
    csv_content = "201\n202\n203\n"
    csv_file = tmp_path / "test.csv"
    csv_file.write_text(csv_content)

    manager = InputManager()
    ids = manager.load_ids(csv_file)

    assert ids == ["202", "203"]

def test_load_ids_file_not_found():
    manager = InputManager()
    with pytest.raises(FileNotFoundError):
        manager.load_ids(Path("non_existent.csv"))

def test_validate_format_valid(tmp_path):
    csv_file = tmp_path / "test.csv"
    csv_file.write_text("ID,Name\n1,Alpha")

    manager = InputManager()
    assert manager.validate_format(csv_file) is True

def test_validate_format_invalid_empty(tmp_path):
    csv_file = tmp_path / "empty.csv"
    csv_file.write_text("")

    manager = InputManager()
    assert manager.validate_format(csv_file) is False

def test_validate_format_not_exists():
    manager = InputManager()
    assert manager.validate_format(Path("ghost.csv")) is False
