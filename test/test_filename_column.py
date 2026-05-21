import pytest
from unittest.mock import MagicMock, patch
from pathlib import Path
from src.orchestrator import Orchestrator
from src.oracle_connector import DBConfig

def test_download_mode_with_filename_column():
    # Arrange
    input_manager = MagicMock()
    db_connector = MagicMock()
    clob_processor = MagicMock()
    fs_manager = MagicMock()

    orchestrator = Orchestrator(input_manager, db_connector, clob_processor, fs_manager)

    csv_path = Path("test.csv")
    output_dir = Path("output")
    db_config = DBConfig(
        user="user",
        password="password",
        dsn="dsn",
        target_table="table",
        id_column="id",
        clob_column="clob",
        filename_column="filename_col"
    )

    input_manager.load_ids.return_value = ["1", "2"]
    # Mock return value for fetch_clobs_in: (ID, LOB, Filename)
    db_connector.fetch_clobs_in.return_value = [
        ("1", "content1", "file1.txt"),
        ("2", "content2", "file2.pdf")
    ]

    # Act
    orchestrator.download_mode(csv_path, output_dir, db_config)

    # Assert
    assert clob_processor.stream_to_file.call_count == 2
    clob_processor.stream_to_file.assert_any_call("content1", output_dir / "file1.txt")
    clob_processor.stream_to_file.assert_any_call("content2", output_dir / "file2.pdf")

def test_download_mode_without_filename_column_fallback():
    # Arrange
    input_manager = MagicMock()
    db_connector = MagicMock()
    clob_processor = MagicMock()
    fs_manager = MagicMock()

    orchestrator = Orchestrator(input_manager, db_connector, clob_processor, fs_manager)

    csv_path = Path("test.csv")
    output_dir = Path("output")
    db_config = DBConfig(
        user="user",
        password="password",
        dsn="dsn",
        target_table="table",
        id_column="id",
        clob_column="clob",
        filename_column=None
    )

    input_manager.load_ids.return_value = ["1"]
    # Mock return value for fetch_clobs_in: (ID, LOB)
    db_connector.fetch_clobs_in.return_value = [
        ("1", "content1")
    ]

    # Act
    orchestrator.download_mode(csv_path, output_dir, db_config)

    # Assert
    clob_processor.stream_to_file.assert_called_once_with("content1", output_dir / "1.txt")
