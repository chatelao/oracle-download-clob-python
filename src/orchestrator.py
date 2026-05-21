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

        if reporter:
            reporter.set_total(len(ids))

        self.db_connector.connect(db_config)
        try:
            self.fs_manager.ensure_directory(output_dir)

            if len(ids) < 1000:
                logger.info(f"Using IN clause strategy for {len(ids)} IDs")
                clob_iterator = self.db_connector.fetch_clobs_in(ids)
            else:
                logger.info(f"Using GTT Join strategy for {len(ids)} IDs")
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
            self.db_connector.close()

    def upload_mode(self, csv_path: Path, input_dir: Path, db_config: DBConfig):
        """Orchestrates UC-2."""
        ids = self.input_manager.load_ids(csv_path)
        if not ids:
            return

        self.db_connector.connect(db_config)
        try:
            for id_val in ids:
                file_path = input_dir / f"{id_val}.txt"
                if file_path.exists():
                    with self.clob_processor.open_file(file_path) as f:
                        self.db_connector.update_clob(id_val, f)
                else:
                    logger.warning(f"File not found for ID {id_val}: {file_path}")
            self.db_connector.commit()
        finally:
            self.db_connector.close()
