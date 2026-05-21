import click
import logging
import sys
import configparser
import tomllib
from pathlib import Path
from src.oracle_connector import DBConfig, OracleConnector
from src.input_manager import InputManager
from src.clob_processor import CLOBProcessor
from src.fs_manager import FSManager
from src.orchestrator import Orchestrator, ProgressReporter

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[logging.StreamHandler(sys.stderr)]
)
logger = logging.getLogger(__name__)

class ClickProgressReporter(ProgressReporter):
    """Progress reporter using click.progressbar."""
    def __init__(self):
        self.bar = None
        self._started = False

    def set_total(self, total: int):
        self.bar = click.progressbar(length=total,
                                    label='Downloading',
                                    show_pos=True,
                                    show_percent=True)

    def update(self, n: int):
        if self.bar:
            if not self._started:
                self.bar.__enter__()
                self._started = True
            self.bar.update(n)

    def finish(self):
        if self.bar and self._started:
            self.bar.__exit__(None, None, None)
            self._started = False

def load_config(ctx, param, value):
    """Callback to load configuration from a file."""
    if not value:
        return None

    config_path = Path(value)
    if not config_path.exists():
        return value

    config_data = {}
    ext = config_path.suffix.lower()
    try:
        if ext == '.toml':
            with open(config_path, "rb") as f:
                config_data = tomllib.load(f)
        elif ext == '.ini':
            config = configparser.ConfigParser()
            config.read(config_path)
            section = 'oracle-clob-tool'
            if section in config:
                config_data = dict(config.items(section))
            else:
                config_data = dict(config.items('DEFAULT'))

        # Normalize keys: replace '-' with '_' to match click parameter names
        # Skip csv-path as per requirement "except the id list"
        normalized_config = {k.replace('-', '_'): v for k, v in config_data.items() if k != 'csv-path'}

        ctx.default_map = ctx.default_map or {}
        ctx.default_map.update(normalized_config)
    except Exception as e:
        raise click.BadParameter(f"Error loading config file: {e}")

    return value

@click.group()
@click.option('--debug', is_flag=True, help='Enables debug logging.')
def cli(debug):
    """Oracle CLOB Download and Upload Tool."""
    if debug:
        logging.getLogger().setLevel(logging.DEBUG)

@cli.command()
@click.option('--config', type=click.Path(exists=True), callback=load_config, is_eager=True, expose_value=False, help='Path to INI or TOML config file.')
@click.option('--csv-path', type=click.Path(exists=True), required=True, help='Path to the CSV file containing IDs.')
@click.option('--output-dir', type=click.Path(), required=True, help='Target directory for downloaded files.')
@click.option('--user', required=True, help='Oracle DB username.')
@click.option('--password', required=True, help='Oracle DB password.')
@click.option('--dsn', required=True, help='Oracle DB DSN.')
@click.option('--table', required=False, help='Target table name.')
@click.option('--query', required=False, help='User written SELECT statement.')
@click.option('--id-column', required=True, help='Column name for IDs.')
@click.option('--clob-column', required=True, help='Column name for CLOBs.')
@click.option('--filename-column', required=False, help='Column name for filenames.')
@click.option('--gtt-name', default="GTT_IDS", help='Name of the Global Temporary Table.')
def download(csv_path, output_dir, user, password, dsn, table, query, id_column, clob_column, filename_column, gtt_name):
    """Download CLOBs to local files."""
    if not table and not query:
        raise click.UsageError("Either --table or --query must be provided.")

    try:
        db_config = DBConfig(
            user=user,
            password=password,
            dsn=dsn,
            target_table=table,
            id_column=id_column,
            clob_column=clob_column,
            gtt_name=gtt_name,
            query=query,
            filename_column=filename_column
        )

        orchestrator = Orchestrator(
            input_manager=InputManager(),
            db_connector=OracleConnector(),
            clob_processor=CLOBProcessor(),
            fs_manager=FSManager()
        )

        logger.info(f"Starting download mode. CSV: {csv_path}, Output: {output_dir}")
        reporter = ClickProgressReporter()
        try:
            orchestrator.download_mode(Path(csv_path), Path(output_dir), db_config, reporter=reporter)
        finally:
            reporter.finish()
        logger.info("Download completed successfully.")
    except Exception as e:
        logger.error(f"Download failed: {e}")
        if logging.getLogger().isEnabledFor(logging.DEBUG):
            logger.exception(e)
        sys.exit(1)

@cli.command()
@click.option('--config', type=click.Path(exists=True), callback=load_config, is_eager=True, expose_value=False, help='Path to INI or TOML config file.')
@click.option('--csv-path', type=click.Path(exists=True), required=True, help='Path to the CSV file containing IDs.')
@click.option('--input-dir', type=click.Path(exists=True), required=True, help='Source directory containing files to upload.')
@click.option('--user', required=True, help='Oracle DB username.')
@click.option('--password', required=True, help='Oracle DB password.')
@click.option('--dsn', required=True, help='Oracle DB DSN.')
@click.option('--table', required=True, help='Target table name.')
@click.option('--id-column', required=True, help='Column name for IDs.')
@click.option('--clob-column', required=True, help='Column name for CLOBs.')
def upload(csv_path, input_dir, user, password, dsn, table, id_column, clob_column):
    """Upload local files to Oracle CLOBs."""
    try:
        db_config = DBConfig(
            user=user,
            password=password,
            dsn=dsn,
            target_table=table,
            id_column=id_column,
            clob_column=clob_column
        )

        orchestrator = Orchestrator(
            input_manager=InputManager(),
            db_connector=OracleConnector(),
            clob_processor=CLOBProcessor(),
            fs_manager=FSManager()
        )

        logger.info(f"Starting upload mode. CSV: {csv_path}, Input: {input_dir}")
        orchestrator.upload_mode(Path(csv_path), Path(input_dir), db_config)
        logger.info("Upload completed successfully.")
    except Exception as e:
        logger.error(f"Upload failed: {e}")
        if logging.getLogger().isEnabledFor(logging.DEBUG):
            logger.exception(e)
        sys.exit(1)

if __name__ == '__main__':
    cli()
