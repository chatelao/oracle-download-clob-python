import pytest
import os
import pathlib
import csv
from src.oracle_connector import OracleConnector, DBConfig
from src.orchestrator import Orchestrator
from src.input_manager import InputManager
from src.clob_processor import CLOBProcessor
from src.fs_manager import FSManager

# Skip these tests if not running in a context where Oracle is available (e.g., local dev without docker)
ORACLE_DSN = os.getenv("ORACLE_DSN", "localhost:1521/FREEPDB1")
ORACLE_USER = os.getenv("ORACLE_USER", "TEST_USER")
ORACLE_PWD = os.getenv("ORACLE_PWD", "test_password")

@pytest.fixture
def db_config():
    return DBConfig(
        user=ORACLE_USER,
        password=ORACLE_PWD,
        dsn=ORACLE_DSN,
        target_table="CLOB_DATA",
        id_column="ID",
        clob_column="DATA_CONTENT"
    )

@pytest.fixture
def orchestrator(db_config):
    connector = OracleConnector()
    input_mgr = InputManager()
    processor = CLOBProcessor()
    fs_mgr = FSManager()
    return Orchestrator(input_mgr, connector, processor, fs_mgr), connector

def test_full_download_upload_cycle(orchestrator, db_config, tmp_path):
    orch, connector = orchestrator

    # 1. Create a CSV with IDs to download
    csv_path = tmp_path / "ids.csv"
    with open(csv_path, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerow(['ID'])
        writer.writerow(['1'])
        writer.writerow(['2'])

    output_dir = tmp_path / "downloaded"
    output_dir.mkdir()

    # 2. Run Download Mode
    # We need to connect first because Orchestrator expects it or handles it?
    # Looking at orchestrator.py might be needed, but usually it should handle connection.
    # In our current implementation, we might need to pass config.

    # Mocking or setting environment for DBConfig if needed,
    # but Orchestrator.download_mode takes csv_path and output_dir.
    # It probably needs to be initialized with the connector and config.

    # Let's assume Orchestrator needs to be told about the config.
    # For this test, we'll manually connect the connector.
    connector.connect(db_config)
    try:
        orch.download_mode(csv_path, output_dir)

        # Verify files were downloaded
        assert (output_dir / "1.txt").exists()
        assert (output_dir / "2.txt").exists()
        assert (output_dir / "1.txt").read_text() == "Initial content for ID 1"
        assert (output_dir / "2.txt").read_text() == "Initial content for ID 2"

        # 3. Modify a file and Upload it back
        (output_dir / "1.txt").write_text("Updated content for ID 1")

        orch.upload_mode(csv_path, output_dir)

        # 4. Verify the update in the database
        connector.create_gtt(["1"])
        results = list(connector.fetch_clobs_join())
        assert len(results) >= 1
        # find the one with ID 1
        row1 = next(r for r in results if r[0] == "1")
        # In real oracledb, the second element might be a LOB object
        lob_content = row1[1]
        if hasattr(lob_content, 'read'):
            lob_content = lob_content.read()
        assert lob_content == "Updated content for ID 1"

    finally:
        connector.close()
