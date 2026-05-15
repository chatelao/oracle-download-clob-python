from pathlib import Path
from typing import Any

class CLOBProcessor:
    """Handles streaming of large text data."""

    def __init__(self, chunk_size: int = 65536):
        self.chunk_size = chunk_size

    def stream_to_file(self, clob_lob: Any, target_path: Path):
        """Reads from database LOB and writes to disk in chunks."""
        with target_path.open('w', encoding='utf-8') as f:
            offset = 1
            while True:
                data = clob_lob.read(offset, self.chunk_size)
                if not data:
                    break
                f.write(data)
                offset += len(data)

    def read_from_file(self, source_path: Path) -> str:
        """Reads file content for upload."""
        return source_path.read_text(encoding='utf-8')
