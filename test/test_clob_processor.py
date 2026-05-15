import pytest
from pathlib import Path
from unittest.mock import MagicMock
from src.clob_processor import CLOBProcessor

def test_stream_to_file(tmp_path):
    target_path = tmp_path / "output.txt"
    mock_lob = MagicMock()

    # Simulate chunked reading: 2 chunks and then empty
    mock_lob.read.side_effect = ["Hello ", "World", ""]

    processor = CLOBProcessor(chunk_size=6)
    processor.stream_to_file(mock_lob, target_path)

    assert target_path.read_text(encoding='utf-8') == "Hello World"
    # Verify read calls: oracledb LOB.read(offset, length)
    assert mock_lob.read.call_count == 3
    mock_lob.read.assert_any_call(1, 6)
    mock_lob.read.assert_any_call(7, 6)
    mock_lob.read.assert_any_call(12, 6)

def test_read_from_file(tmp_path):
    source_path = tmp_path / "input.txt"
    content = "Sample CLOB content"
    source_path.write_text(content, encoding='utf-8')

    processor = CLOBProcessor()
    result = processor.read_from_file(source_path)

    assert result == content
