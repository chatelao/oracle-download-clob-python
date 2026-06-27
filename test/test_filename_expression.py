import pytest
from unittest.mock import MagicMock, patch
from src.oracle_connector import OracleConnector, DBConfig

def test_fetch_clobs_join_with_filename_expression():
    connector = OracleConnector()
    db_config = DBConfig(
        user="test_user",
        password="test_password",
        dsn="test_dsn",
        target_table="TEST_TABLE",
        id_column="MY_ID",
        clob_column="MY_CLOB",
        filename_column="substr(filename,1,8)||substr(filename,-8,8)"
    )

    with patch('oracledb.connect') as mock_connect:
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_conn.cursor.return_value.__enter__.return_value = mock_cursor
        mock_connect.return_value = mock_conn

        connector.connect(db_config)

        mock_cursor.fetchmany.return_value = []

        list(connector.fetch_clobs_join())

        mock_cursor.execute.assert_called_once()
        sql = mock_cursor.execute.call_args[0][0]

        # Desired behavior: it does NOT prefix with t.
        # This allows complex expressions.
        assert "substr(filename,1,8)||substr(filename,-8,8)" in sql
        assert "t.substr(filename,1,8)||substr(filename,-8,8)" not in sql

def test_fetch_clobs_in_with_filename_expression():
    connector = OracleConnector()
    db_config = DBConfig(
        user="test_user",
        password="test_password",
        dsn="test_dsn",
        target_table="TEST_TABLE",
        id_column="MY_ID",
        clob_column="MY_CLOB",
        filename_column="substr(filename,1,8)||substr(filename,-8,8)"
    )

    with patch('oracledb.connect') as mock_connect:
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_conn.cursor.return_value.__enter__.return_value = mock_cursor
        mock_connect.return_value = mock_conn

        connector.connect(db_config)

        mock_cursor.fetchmany.return_value = []

        list(connector.fetch_clobs_in(["1"]))

        mock_cursor.execute.assert_called_once()
        sql = mock_cursor.execute.call_args[0][0]

        # Already correctly does NOT prefix with t.
        assert "substr(filename,1,8)||substr(filename,-8,8)" in sql
        assert "t.substr" not in sql
