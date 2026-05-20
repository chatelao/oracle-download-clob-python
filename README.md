# Oracle CLOB Download and Upload Tool

A high-performance CLI tool to download and upload Oracle CLOB fields based on a list of IDs provided in a CSV file.

## Key Features

- **High Performance:** Uses Global Temporary Tables (GTT) and set-based JOIN operations for efficient ID filtering.
- **Memory Efficient:** Streams CLOB data in chunks to handle very large fields without high memory consumption.
- **Easy Deployment:** Built with the `python-oracledb` thin driver, requiring no Oracle Instant Client installation.

## Documentation

Detailed documentation is available in the following files:

- [INSTALL.md](INSTALL.md): **Installation and Usage Guide**.
- [CONCEPT.md](CONCEPT.md): Business goals, use cases, and high-level architecture.
- [DESIGN.md](DESIGN.md): Technical stack, interface definitions, and major design choices.
- [ROADMAP.md](ROADMAP.md): Project implementation plan and progress.
- [TECHNICAL_DEBTS.md](TECHNICAL_DEBTS.md): Logged technical debts and future improvements.
- [JAVA_HOWTO_USE.md](JAVA_HOWTO_USE.md): **Java CLI Usage Guide**.

Online documentation (ReadTheDocs): [Coming Soon]

## Getting Started

Refer to [INSTALL.md](INSTALL.md) for detailed instructions on how to install and use the tool.

### Quick Start (Source)

```bash
# Install dependencies
./src/install.sh

# Download CLOBs
python3 src/cli.py download --csv-path ids.csv --output-dir ./output [DB_OPTIONS]

# Upload CLOBs
python3 src/cli.py upload --csv-path ids.csv --input-dir ./input [DB_OPTIONS]
```

## Testing

```bash
./test/install.sh
python3 -m pytest
```
