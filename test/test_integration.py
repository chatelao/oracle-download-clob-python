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

def test_large_clob_integration(connector):
    # Create a large string (> 64KB)
    large_content = "A" * (70 * 1024)
    target_id = "1"
    connector.update_clob(target_id, large_content)
    connector.commit()

    connector.create_gtt([target_id])
    results = list(connector.fetch_clobs_join())
    assert len(results) == 1

    content = results[0][1]
    if hasattr(content, 'read'):
        content = content.read()
    assert str(content) == large_content

def test_empty_clob_integration(connector):
    target_id = "2"
    connector.update_clob(target_id, "")
    connector.commit()

    connector.create_gtt([target_id])
    results = list(connector.fetch_clobs_join())
    assert len(results) == 1

    content = results[0][1]
    if hasattr(content, 'read'):
        content = content.read()
    # Oracle treats empty string as NULL in some contexts, but let's see how it behaves here
    assert content is None or str(content) == ""

def test_unicode_clob_integration(connector):
    unicode_content = "Hello 🌍, Special characters: ñ, á, é, í, ó, ú, ⚡"
    target_id = "3"
    connector.update_clob(target_id, unicode_content)
    connector.commit()

    connector.create_gtt([target_id])
    results = list(connector.fetch_clobs_join())
    assert len(results) == 1

    content = results[0][1]
    if hasattr(content, 'read'):
        content = content.read()
    assert str(content) == unicode_content

def test_non_existent_id_integration(connector):
    # GTT has an ID that doesn't exist in CLOB_DATA
    connector.create_gtt(["non-existent-999"])
    results = list(connector.fetch_clobs_join())
    assert len(results) == 0

def test_multiple_ids_integration(connector):
    ids = ["1", "2", "3"]
    connector.create_gtt(ids)
    results = list(connector.fetch_clobs_join())
    assert len(results) == 3

    result_ids = {row[0] for row in results}
    assert result_ids == set(ids)
