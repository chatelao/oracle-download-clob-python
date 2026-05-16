import os
import pytest
from src.oracle_connector import OracleConnector, DBConfig

# Integration tests are marked and can be skipped if DB is not available
pytestmark = pytest.mark.integration

@pytest.fixture
def db_config():
    return DBConfig(
        user=os.getenv("DB_USER", "system"),
        password=os.getenv("DB_PASSWORD", "password"),
        dsn=os.getenv("DB_DSN", "127.0.0.1:1521/FREEPDB1"),
        target_table="CLOB_DATA",
        id_column="ID",
        clob_column="CONTENT",
        gtt_name="GTT_IDS_INTEGRATION"
    )

@pytest.fixture
def connector(db_config):
    conn = OracleConnector()
    conn.connect(db_config)
    yield conn
    conn.close()

def test_oracle_connection(connector):
    assert connector.conn is not None

def test_fetch_clobs_join_integration(connector):
    ids = ["1", "2"]
    connector.create_gtt(ids)

    results = list(connector.fetch_clobs_join())
    assert len(results) == 2

    # Sort results by ID for verification
    results.sort(key=lambda x: x[0])

    assert results[0][0] == "1"
    # LOB objects might need to be read
    content1 = results[0][1]
    if hasattr(content1, 'read'):
        content1 = content1.read()
    assert "Initial content for ID 1" in str(content1)

def test_update_clob_integration(connector):
    new_content = "Updated content via integration test"
    connector.update_clob("3", new_content)

    # Verify the update
    connector.create_gtt(["3"])
    results = list(connector.fetch_clobs_join())
    assert len(results) == 1

    content = results[0][1]
    if hasattr(content, 'read'):
        content = content.read()
    assert str(content) == new_content
