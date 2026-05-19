import pytest
from pathlib import Path
from unittest.mock import MagicMock
from src.orchestrator import Orchestrator
from src.oracle_connector import DBConfig

@pytest.fixture
def mock_managers():
    return {
        "input": MagicMock(),
        "db": MagicMock(),
        "processor": MagicMock(),
        "fs": MagicMock()
    }

@pytest.fixture
def orchestrator(mock_managers):
    return Orchestrator(
        input_manager=mock_managers["input"],
        db_connector=mock_managers["db"],
        clob_processor=mock_managers["processor"],
        fs_manager=mock_managers["fs"]
    )

@pytest.fixture
def db_config():
    return DBConfig(
        user="u", password="p", dsn="d",
        target_table="T", id_column="I", clob_column="C"
    )


def test_download_mode_large_dataset(orchestrator, mock_managers, db_config, tmp_path):
    csv_path = tmp_path / "ids.csv"
    output_dir = tmp_path / "output"

    ids = [str(i) for i in range(1001)]
    mock_managers["input"].load_ids.return_value = ids
    mock_managers["db"].fetch_clobs_join.return_value = []

    orchestrator.download_mode(csv_path, output_dir, db_config)

    mock_managers["db"].create_gtt.assert_called_once_with(ids)
    mock_managers["db"].fetch_clobs_join.assert_called_once()
    mock_managers["db"].fetch_clobs_in.assert_not_called()

def test_download_mode_small_dataset(orchestrator, mock_managers, db_config, tmp_path):
    csv_path = tmp_path / "ids.csv"
    output_dir = tmp_path / "output"

    ids = ["1", "2"]
    mock_managers["input"].load_ids.return_value = ids
    mock_managers["db"].fetch_clobs_in.return_value = []

    orchestrator.download_mode(csv_path, output_dir, db_config)

    mock_managers["db"].create_gtt.assert_not_called()
    mock_managers["db"].fetch_clobs_join.assert_not_called()
    mock_managers["db"].fetch_clobs_in.assert_called_once_with(ids)

def test_upload_mode(orchestrator, mock_managers, db_config, tmp_path, caplog):
    csv_path = tmp_path / "ids.csv"
    input_dir = tmp_path / "input"
    input_dir.mkdir()
    (input_dir / "1.txt").write_text("content1")
    # 2.txt does not exist

    mock_managers["input"].load_ids.return_value = ["1", "2"]
    mock_managers["processor"].read_from_file.return_value = "content1"

    orchestrator.upload_mode(csv_path, input_dir, db_config)

    mock_managers["input"].load_ids.assert_called_once_with(csv_path)
    mock_managers["db"].connect.assert_called_once_with(db_config)
    mock_managers["processor"].open_file.assert_called_once()
    mock_managers["db"].update_clob.assert_called_once()
    mock_managers["db"].commit.assert_called_once()
    mock_managers["db"].close.assert_called_once()

    assert "File not found for ID 2" in caplog.text
