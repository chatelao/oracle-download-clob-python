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

    def download_mode(self, csv_path: Optional[Path], output_dir: Path, db_config: DBConfig,
                      reporter: Optional[ProgressReporter] = None):
        """Orchestrates UC-1."""
        self.db_connector.connect(db_config)
        try:
            if db_config.id_query:
                logger.info(f"Fetching IDs using query: {db_config.id_query}")
                ids = self.db_connector.fetch_ids(db_config.id_query)
            elif csv_path:
                ids = self.input_manager.load_ids(csv_path)
            else:
                raise ValueError("Either csv_path or id_query must be provided.")

            if not ids:
                logger.info("No IDs found.")
                return

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

    def upload_mode(self, csv_path: Optional[Path], input_dir: Path, db_config: DBConfig,
                    id_as_regex: bool = False, batch_size: int = 100):
        """Orchestrates UC-2."""
        import oracledb
        self.db_connector.connect(db_config)
        try:
            if db_config.id_query:
                logger.info(f"Fetching IDs using query: {db_config.id_query}")
                patterns_or_ids = self.db_connector.fetch_ids(db_config.id_query)
            elif csv_path:
                patterns_or_ids = self.input_manager.load_ids(csv_path)
            else:
                raise ValueError("Either csv_path or id_query must be provided.")

            if not patterns_or_ids:
                logger.info("No IDs found.")
                return

            col_type = self.db_connector.get_lob_column_type()
            is_binary = (col_type == oracledb.DB_TYPE_BLOB)
            mode = 'rb' if is_binary else 'r'

            upload_attempted = 0
            upload_success = 0

            def _perform_update(db_id_val, f_obj):
                nonlocal upload_success
                affected = self.db_connector.update_lob(db_id_val, f_obj)
                if affected > 0:
                    upload_success += 1
                else:
                    logger.warning(f"No rows updated for ID {db_id_val}. Record may not exist.")

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
                            with self.clob_processor.open_file(file_path, mode=mode) as f:
                                _perform_update(db_id, f)
                            upload_attempted += 1
                            if upload_attempted % batch_size == 0:
                                self.db_connector.commit()
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
                        with self.clob_processor.open_file(file_path, mode=mode) as f:
                            _perform_update(id_val, f)
                        upload_attempted += 1
                        if upload_attempted % batch_size == 0:
                            self.db_connector.commit()
                    else:
                        logger.warning(f"File not found for ID {id_val} in {input_dir}")

            self.db_connector.commit()
            logger.info(f"Total files attempted: {upload_attempted}, Successfully updated: {upload_success}")
        finally:
            self.db_connector.close()
