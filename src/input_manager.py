import csv
from pathlib import Path
from typing import List

class InputManager:
    """Parses CSV files and extracts a unique list of IDs."""

    def load_ids(self, file_path: Path) -> List[str]:
        """Reads the CSV and returns a list of unique strings from the first column."""
        if not file_path.exists():
            raise FileNotFoundError(f"Input file not found: {file_path}")

        ids = set()
        with open(file_path, mode='r', newline='', encoding='utf-8') as csvfile:
            # Detect header presence
            sample = csvfile.read(2048)
            csvfile.seek(0)
            has_header = False
            if sample:
                try:
                    has_header = csv.Sniffer().has_header(sample)
                except Exception:
                    has_header = False

            reader = csv.reader(csvfile)

            # Use a flag to ensure we only skip one header
            header_skipped = False
            if has_header:
                next(reader, None)
                header_skipped = True

            for row in reader:
                if row and row[0]:
                    val = row[0].strip()
                    if val:
                        # Fallback: if Sniffer failed but first row is "ID", skip it
                        if not header_skipped and val.upper() == "ID":
                            header_skipped = True
                            continue
                        ids.add(val)

        return sorted(list(ids))

    def validate_format(self, file_path: Path) -> bool:
        """Ensures the CSV exists and contains at least one column."""
        if not file_path.exists():
            return False

        try:
            with open(file_path, mode='r', newline='', encoding='utf-8') as csvfile:
                reader = csv.reader(csvfile)
                row = next(reader, None)
                return row is not None and len(row) >= 1
        except Exception:
            return False
