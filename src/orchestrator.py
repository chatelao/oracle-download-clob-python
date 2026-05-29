from pathlib import Path
from src.input_manager import InputManager
from src.oracle_connector import OracleConnector, DBConfig
from src.clob_processor import CLOBProcessor
import logging
from src.fs_manager import FSManager
from typing import Optional

logger = logging.getLogger(__name__)

class ProgressReporter:
    """Base class/interface for progress reporting."""
    def set_total(self, total: int):
        pass

    def update(self, n: int):
        pass

    def finish(self):
        pass

class Orchestrator:
    """High-level execution flow for Download and Upload modes."""

    def __init__(self,
                 input_manager: InputManager,
                 db_connector: OracleConnector,
                 clob_processor: CLOBProcessor,
                 fs_manager: FSManager):
        self.input_manager = input_manager
        self.db_connector = db_connector
        self.clob_processor = clob_processor
        self.fs_manager = fs_manager

    def download_mode(self, csv_path: Path, output_dir: Path, db_config: DBConfig,
                      reporter: Optional[ProgressReporter] = None):
        """Orchestrates UC-1."""
        ids = self.input_manager.load_ids(csv_path)
        if not ids:
            return

        self.db_connector.connect(db_config)
        try:
            self.fs_manager.ensure_directory(output_dir)

            if len(ids) < 1000:
                logger.info(f"Using IN clause strategy for {len(ids)} IDs")
                if reporter:
                    reporter.set_total(len(ids))
                clob_iterator = self.db_connector.fetch_clobs_in(ids)
            else:
                logger.info(f"Using GTT Join strategy for {len(ids)} IDs")
                if reporter:
                    reporter.set_total(len(ids))
                self.db_connector.create_gtt(ids)
                clob_iterator = self.db_connector.fetch_clobs_join()

            for row in clob_iterator:
                id_val, clob_lob = row[0], row[1]
                filename = row[2] if len(row) > 2 else None

                if filename:
                    target_path = output_dir / filename
                else:
                    target_path = output_dir / f"{id_val}.txt"

                self.clob_processor.stream_to_file(clob_lob, target_path)
                if reporter:
                    reporter.update(1)
        finally:
            if reporter:
                reporter.finish()
            self.db_connector.close()

    def upload_mode(self, csv_path: Path, input_dir: Path, db_config: DBConfig,
                    id_as_regex: bool = False, batch_size: int = 100):
        """Orchestrates UC-2."""
        patterns_or_ids = self.input_manager.load_ids(csv_path)
        if not patterns_or_ids:
            return

        import oracledb
        self.db_connector.connect(db_config)
        upload_attempted = 0
        upload_success = 0
        batch = []
        file_handles = []

        def _flush_batch():
            nonlocal upload_success, batch, file_handles
            if not batch:
                return
            try:
                affected = self.db_connector.update_lobs_batch(batch)
                upload_success += affected
                self.db_connector.commit()
            finally:
                for fh in file_handles:
                    fh.close()
                batch = []
                file_handles = []

        try:
            col_type = self.db_connector.get_lob_column_type()
            is_binary = (col_type == oracledb.DB_TYPE_BLOB)
            mode = 'rb' if is_binary else 'r'

            if id_as_regex:
                import re
                compiled_patterns = []
                for p in patterns_or_ids:
                    try:
                        compiled_patterns.append(re.compile(p))
                    except re.error as e:
                        logger.error(f"Invalid regex pattern '{p}': {e}")

                for file_path in input_dir.iterdir():
                    if not file_path.is_file():
                        continue
                    filename = file_path.name
                    for cp in compiled_patterns:
                        match = cp.search(filename)
                        if match:
                            db_id = match.group(1) if match.groups() else match.group(0)
                            logger.info(f"Matched file {filename} with pattern {cp.pattern} -> ID: {db_id}")
                            f = self.clob_processor.open_file(file_path, mode=mode)
                            file_handles.append(f)
                            batch.append((f, db_id))
                            upload_attempted += 1
                            if len(batch) >= batch_size:
                                _flush_batch()
                            break
            else:
                # Try with .txt, then without extension if not found
                for id_val in patterns_or_ids:
                    file_path = input_dir / f"{id_val}.txt"
                    if not file_path.exists():
                        file_path = input_dir / id_val

                    if not file_path.exists():
                        # Try globbing
                        matches = list(input_dir.glob(f"{id_val}.*"))
                        if matches:
                            file_path = matches[0]

                    if file_path.exists():
                        logger.info(f"Uploading file {file_path.name} for ID {id_val}")
                        f = self.clob_processor.open_file(file_path, mode=mode)
                        file_handles.append(f)
                        batch.append((f, id_val))
                        upload_attempted += 1
                        if len(batch) >= batch_size:
                            _flush_batch()
                    else:
                        logger.warning(f"File not found for ID {id_val} in {input_dir}")

            _flush_batch()
            logger.info(f"Total files attempted: {upload_attempted}, Successfully updated: {upload_success}")
        finally:
            for fh in file_handles:
                fh.close()
            self.db_connector.close()
