from pathlib import Path
from typing import List

class InputManager:
    """Parses CSV files and extracts a unique list of IDs."""

    def load_ids(self, file_path: Path) -> List[str]:
        """Reads the CSV and returns a list of strings."""
        pass

    def validate_format(self, file_path: Path) -> bool:
        """Ensures the CSV contains the expected column."""
        pass
