import pytest
import tomllib
from click.testing import CliRunner
from unittest.mock import patch, MagicMock
from src.cli import cli

@patch('src.cli.Orchestrator')
def test_download_with_config_toml(mock_orchestrator_class, tmp_path):
    mock_orchestrator = MagicMock()
    mock_orchestrator_class.return_value = mock_orchestrator

    csv_path = tmp_path / "ids.csv"
    csv_path.write_text("ID\n1")
    output_dir = tmp_path / "output"

    config_path = tmp_path / "config.toml"
    config_path.write_text("""
user = "conf_user"
password = "conf_password"
dsn = "conf_dsn"
table = "conf_table"
id-column = "conf_id"
clob-column = "conf_clob"
""")

    runner = CliRunner()
    result = runner.invoke(cli, [
        'download',
        '--config', str(config_path),
        '--csv-path', str(csv_path),
        '--output-dir', str(output_dir)
    ])

    assert result.exit_code == 0
    # Verify that values from config were used
    db_config = mock_orchestrator.download_mode.call_args[0][2]
    assert db_config.user == "conf_user"
    assert db_config.id_column == "conf_id"

@patch('src.cli.Orchestrator')
def test_download_with_config_ini(mock_orchestrator_class, tmp_path):
    mock_orchestrator = MagicMock()
    mock_orchestrator_class.return_value = mock_orchestrator

    csv_path = tmp_path / "ids.csv"
    csv_path.write_text("ID\n1")
    output_dir = tmp_path / "output"

    config_path = tmp_path / "config.ini"
    config_path.write_text("""
[oracle-clob-tool]
user = conf_user_ini
password = conf_password_ini
dsn = conf_dsn_ini
table = conf_table_ini
id-column = conf_id_ini
clob-column = conf_clob_ini
""")

    runner = CliRunner()
    result = runner.invoke(cli, [
        'download',
        '--config', str(config_path),
        '--csv-path', str(csv_path),
        '--output-dir', str(output_dir)
    ])

    assert result.exit_code == 0
    db_config = mock_orchestrator.download_mode.call_args[0][2]
    assert db_config.user == "conf_user_ini"
    assert db_config.id_column == "conf_id_ini"

@patch('src.cli.Orchestrator')
def test_config_override_by_cli(mock_orchestrator_class, tmp_path):
    mock_orchestrator = MagicMock()
    mock_orchestrator_class.return_value = mock_orchestrator

    csv_path = tmp_path / "ids.csv"
    csv_path.write_text("ID\n1")
    output_dir = tmp_path / "output"

    config_path = tmp_path / "config.toml"
    config_path.write_text("""
user = "conf_user"
password = "conf_password"
dsn = "conf_dsn"
table = "conf_table"
id-column = "conf_id"
clob-column = "conf_clob"
""")

    runner = CliRunner()
    result = runner.invoke(cli, [
        'download',
        '--config', str(config_path),
        '--csv-path', str(csv_path),
        '--output-dir', str(output_dir),
        '--user', 'cli_user'
    ])

    assert result.exit_code == 0
    db_config = mock_orchestrator.download_mode.call_args[0][2]
    assert db_config.user == "cli_user" # CLI override
    assert db_config.id_column == "conf_id" # From config
