import pytest
from pathlib import Path
from src.fs_manager import FSManager

def test_ensure_directory(tmp_path):
    target_dir = tmp_path / "new_dir" / "sub_dir"
    manager = FSManager()

    assert not target_dir.exists()
    manager.ensure_directory(target_dir)
    assert target_dir.is_dir()

def test_list_files(tmp_path):
    (tmp_path / "file1.txt").write_text("content1")
    (tmp_path / "file2.txt").write_text("content2")
    (tmp_path / "data.csv").write_text("content3")

    manager = FSManager()

    txt_files = manager.list_files(tmp_path, "*.txt")
    assert len(txt_files) == 2
    assert sorted([f.name for f in txt_files]) == ["file1.txt", "file2.txt"]

    all_files = manager.list_files(tmp_path, "*")
    assert len(all_files) == 3

def test_list_files_invalid_dir():
    manager = FSManager()
    assert manager.list_files(Path("non_existent_dir_12345"), "*") == []
