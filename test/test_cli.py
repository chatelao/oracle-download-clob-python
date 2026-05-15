import pytest
from click.testing import CliRunner
from unittest.mock import patch, MagicMock
from src.cli import cli

def test_cli_help():
    runner = CliRunner()
    result = runner.invoke(cli, ['--help'])
    assert result.exit_code == 0
    assert 'Download and Upload Tool' in result.output

@patch('src.cli.Orchestrator')
def test_download_command(mock_orchestrator_class, tmp_path):
    mock_orchestrator = MagicMock()
    mock_orchestrator_class.return_value = mock_orchestrator

    csv_path = tmp_path / "ids.csv"
    csv_path.write_text("ID\n1\n2")
    output_dir = tmp_path / "output"

    runner = CliRunner()
    result = runner.invoke(cli, [
        'download',
        '--csv-path', str(csv_path),
        '--output-dir', str(output_dir),
        '--user', 'test_user',
        '--password', 'test_pass',
        '--dsn', 'test_dsn',
        '--table', 'test_table',
        '--id-column', 'id_col',
        '--clob-column', 'clob_col'
    ])

    assert result.exit_code == 0
    mock_orchestrator.download_mode.assert_called_once()

@patch('src.cli.Orchestrator')
def test_upload_command(mock_orchestrator_class, tmp_path):
    mock_orchestrator = MagicMock()
    mock_orchestrator_class.return_value = mock_orchestrator

    csv_path = tmp_path / "ids.csv"
    csv_path.write_text("ID\n1\n2")
    input_dir = tmp_path / "input"
    input_dir.mkdir()

    runner = CliRunner()
    result = runner.invoke(cli, [
        'upload',
        '--csv-path', str(csv_path),
        '--input-dir', str(input_dir),
        '--user', 'test_user',
        '--password', 'test_pass',
        '--dsn', 'test_dsn',
        '--table', 'test_table',
        '--id-column', 'id_col',
        '--clob-column', 'clob_col'
    ])

    assert result.exit_code == 0
    mock_orchestrator.upload_mode.assert_called_once()
