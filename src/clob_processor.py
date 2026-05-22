from pathlib import Path
from typing import Any

class CLOBProcessor:
    """Handles streaming of large text data."""

    def __init__(self, chunk_size: int = 65536):
        self.chunk_size = chunk_size

    def stream_to_file(self, clob_lob: Any, target_path: Path):
        """Reads from database LOB and writes to disk in chunks."""
        # Detect if it's a BLOB by checking the first read result
        first_chunk = clob_lob.read(1, self.chunk_size)
        if not first_chunk:
            # Empty LOB, create empty file
            target_path.write_text("", encoding='utf-8')
            return

        mode = 'wb' if isinstance(first_chunk, bytes) else 'w'
        encoding = None if mode == 'wb' else 'utf-8'

        with target_path.open(mode, encoding=encoding) as f:
            f.write(first_chunk)
            offset = 1 + len(first_chunk)
            while True:
                data = clob_lob.read(offset, self.chunk_size)
                if not data:
                    break
                f.write(data)
                offset += len(data)

    def read_from_file(self, source_path: Path) -> str:
        """Reads file content for upload. Note: Reads entire file into memory."""
        return source_path.read_text(encoding='utf-8')

    def open_file(self, source_path: Path):
        """Opens a file for reading, providing a handle for streaming."""
        return source_path.open('r', encoding='utf-8')

    def open_file_binary(self, source_path: Path):
        """Opens a file for binary reading."""
        return source_path.open('rb')
