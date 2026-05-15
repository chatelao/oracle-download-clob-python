import pytest
from unittest.mock import MagicMock, patch
from src.oracle_connector import OracleConnector, DBConfig

@pytest.fixture
def db_config():
    return DBConfig(
        user="test_user",
        password="test_password",
        dsn="test_dsn",
        target_table="TEST_TABLE",
        id_column="MY_ID",
        clob_column="MY_CLOB",
        gtt_name="MY_GTT"
    )

@pytest.fixture
def connector():
    return OracleConnector()

def test_connect(connector, db_config):
    with patch('oracledb.connect') as mock_connect:
        connector.connect(db_config)
        mock_connect.assert_called_once_with(
            user=db_config.user,
            password=db_config.password,
            dsn=db_config.dsn
        )
        assert connector.config == db_config

def test_close(connector, db_config):
    with patch('oracledb.connect') as mock_connect:
        mock_conn = MagicMock()
        mock_connect.return_value = mock_conn
        connector.connect(db_config)
        connector.close()
        mock_conn.close.assert_called_once()
        assert connector.conn is None

def test_create_gtt(connector, db_config):
    with patch('oracledb.connect') as mock_connect:
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_conn.cursor.return_value.__enter__.return_value = mock_cursor
        mock_connect.return_value = mock_conn

        connector.connect(db_config)
        ids = ["1", "2", "3"]
        connector.create_gtt(ids)

        # Verify CREATE table was called (at least once)
        create_call = any("CREATE GLOBAL TEMPORARY TABLE" in str(call) for call in mock_cursor.execute.call_args_list)
        assert create_call

        mock_cursor.execute.assert_called_with(f"DELETE FROM {db_config.gtt_name}")
        mock_cursor.executemany.assert_called_once()
        args, _ = mock_cursor.executemany.call_args
        assert args[0] == f"INSERT INTO {db_config.gtt_name} ({db_config.id_column}) VALUES (:1)"
        assert args[1] == [("1",), ("2",), ("3",)]

def test_create_gtt_already_exists(connector, db_config):
    with patch('oracledb.connect') as mock_connect:
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_conn.cursor.return_value.__enter__.return_value = mock_cursor
        mock_connect.return_value = mock_conn

        import oracledb
        error_obj = MagicMock()
        error_obj.code = 955
        mock_cursor.execute.side_effect = [oracledb.DatabaseError(error_obj), None, None]

        connector.connect(db_config)
        connector.create_gtt(["1"])

        mock_cursor.execute.assert_called_with(f"DELETE FROM {db_config.gtt_name}")

def test_fetch_clobs_join(connector, db_config):
    with patch('oracledb.connect') as mock_connect:
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_conn.cursor.return_value.__enter__.return_value = mock_cursor
        mock_connect.return_value = mock_conn

        connector.connect(db_config)

        mock_cursor.fetchmany.side_effect = [[("1", "content1"), ("2", "content2")], []]

        results = list(connector.fetch_clobs_join())

        assert len(results) == 2
        assert results[0] == ("1", "content1")
        assert results[1] == ("2", "content2")

        mock_cursor.execute.assert_called_once()
        sql = mock_cursor.execute.call_args[0][0]
        assert db_config.target_table in sql
        assert db_config.gtt_name in sql

def test_update_clob(connector, db_config):
    with patch('oracledb.connect') as mock_connect:
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_conn.cursor.return_value.__enter__.return_value = mock_cursor
        mock_connect.return_value = mock_conn

        connector.connect(db_config)
        connector.update_clob("1", "new_content")

        mock_cursor.execute.assert_called_once()
        args, _ = mock_cursor.execute.call_args
        assert "UPDATE" in args[0]
        assert args[1] == ("new_content", "1")
        mock_conn.commit.assert_called_once()

def test_runtime_error_if_not_connected(connector):
    with pytest.raises(RuntimeError):
        connector.create_gtt(["1"])
    with pytest.raises(RuntimeError):
        list(connector.fetch_clobs_join())
    with pytest.raises(RuntimeError):
        connector.update_clob("1", "content")
