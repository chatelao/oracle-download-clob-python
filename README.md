# Oracle CLOB Download and Upload Tool

A high-performance CLI tool to download and upload Oracle CLOB fields based on a list of IDs provided in a CSV file.

## Key Features

- **High Performance:** Uses Global Temporary Tables (GTT) and set-based JOIN operations for efficient ID filtering.
- **Memory Efficient:** Streams CLOB data in chunks to handle very large fields without high memory consumption.
- **Easy Deployment:** Built with the `python-oracledb` thin driver, requiring no Oracle Instant Client installation.

## Documentation

Detailed documentation is available in the following files:

- [CONCEPT.md](CONCEPT.md): Business goals, use cases, and high-level architecture.
- [DESIGN.md](DESIGN.md): Technical stack, interface definitions, and major design choices.
- [ROADMAP.md](ROADMAP.md): Project implementation plan and progress.
- [TECHNICAL_DEBTS.md](TECHNICAL_DEBTS.md): Logged technical debts and future improvements.

Online documentation (ReadTheDocs): [Coming Soon]

## Standalone Executable

The tool is available as a single-file executable for Linux on every GitHub Release. You can download it from the [Releases](https://github.com/chatelao/oracle-download-clob-python/releases) page.

## Getting Started

### Installation

```bash
./src/install.sh
```

### Usage

#### Download CLOBs

```bash
python3 src/cli.py download \
    --csv-path ids.csv \
    --output-dir ./output \
    --user MYUSER \
    --password MYPASS \
    --dsn MYHOST:1521/SERVICE \
    --table MY_TABLE \
    --id-column ID \
    --clob-column DATA
```

#### Upload CLOBs

```bash
python3 src/cli.py upload \
    --csv-path ids.csv \
    --input-dir ./input \
    --user MYUSER \
    --password MYPASS \
    --dsn MYHOST:1521/SERVICE \
    --table MY_TABLE \
    --id-column ID \
    --clob-column DATA
```

## Testing

```bash
./test/install.sh
python3 -m pytest
```
