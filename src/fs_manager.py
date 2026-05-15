from pathlib import Path
from typing import List

class FSManager:
    """Abstracts disk I/O and directory management."""

    def ensure_directory(self, path: Path):
        """Creates directory if it doesn't exist."""
        path.mkdir(parents=True, exist_ok=True)

    def list_files(self, directory: Path, pattern: str) -> List[Path]:
        """Matches local files with IDs using a glob pattern."""
        if not directory.is_dir():
            return []
        return list(directory.glob(pattern))
