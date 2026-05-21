# Installation and Usage Guide

This guide provides instructions on how to install and use the Oracle CLOB Download and Upload Tool.

## Prerequisites

- **Oracle Database:** Access to an Oracle Database (12c or higher recommended).
- **Network:** Network connectivity from the client machine to the Oracle Database host and port.
- **Global Temporary Table (GTT):** By default, the tool uses a GTT named `GTT_IDS`. Ensure this table exists or you have permissions to create it if it doesn't.
  ```sql
  CREATE GLOBAL TEMPORARY TABLE GTT_IDS (
      ID VARCHAR2(255)
  ) ON COMMIT PRESERVE ROWS;
  ```

---

## Installation Options

### Option 1: Standalone Binary (Recommended)

The easiest way to use the tool is to download the standalone executable from the GitHub Releases page. This version does not require Python to be installed on your system.

1. Go to the [Releases](https://github.com/chatelao/oracle-download-clob-python/releases) page.
2. Download the `oracle-clob-tool` binary for your platform.
3. (Linux/macOS) Make the binary executable:
   ```bash
   chmod +x oracle-clob-tool
   ```

### Option 2: From Source (Python 3.12+)

If you prefer to run the tool from source, you will need Python 3.12 or higher installed.

1. Clone the repository:
   ```bash
   git clone https://github.com/chatelao/oracle-download-clob-python.git
   cd oracle-download-clob-python
   ```
2. Install the required dependencies:
   ```bash
   ./src/install.sh
   # Or manually: pip install -r src/requirements.txt
   ```

### Option 3: From Source (Java 21+)

The tool can also be run or built from source using Java and Maven.

1. Clone the repository:
   ```bash
   git clone https://github.com/chatelao/oracle-download-clob-python.git
   cd oracle-download-clob-python
   ```
2. Run directly using Maven:
   ```bash
   mvn compile exec:java -Dexec.mainClass="com.oracle.tool.CliCommand" -Dexec.args="download --help"
   ```
3. (Optional) Build a native executable using GraalVM:
   ```bash
   mvn package -Pnative
   # The executable will be available at target/oracle-clob-tool
   ```

---

## Usage

The tool has two primary modes: `download` and `upload`.

### 1. Download CLOBs to Local Files

Downloads CLOB data for a specific set of IDs provided in a CSV file.

```bash
# Using the binary
./oracle-clob-tool download \
    --csv-path ids.csv \
    --output-dir ./output \
    --user MYUSER \
    --password MYPASS \
    --dsn MYHOST:1521/SERVICE \
    --table MY_TABLE \
    --id-column ID \
    --clob-column DATA

# OR use a custom query for download
./oracle-clob-tool download \
    --csv-path ids.csv \
    --output-dir ./output \
    --user MYUSER \
    --password MYPASS \
    --dsn MYHOST:1521/SERVICE \
    --query "SELECT ID, DATA FROM MY_TABLE WHERE STATUS = 'ACTIVE'" \
    --id-column ID \
    --clob-column DATA

# Using source code
python3 src/cli.py download [OPTIONS]
```

### 2. Upload Local Files to Oracle CLOBs

Uploads local file content into the CLOB fields of an Oracle table, matching IDs from a CSV file with local filenames.

```bash
# Using the binary
./oracle-clob-tool upload \
    --csv-path ids.csv \
    --input-dir ./input \
    --user MYUSER \
    --password MYPASS \
    --dsn MYHOST:1521/SERVICE \
    --table MY_TABLE \
    --id-column ID \
    --clob-column DATA

# Using source code
python3 src/cli.py upload [OPTIONS]

# Upload using regex patterns to match filenames
./oracle-clob-tool upload \
    --csv-path patterns.csv \
    --input-dir ./input \
    --user MYUSER \
    --password MYPASS \
    --dsn MYHOST:1521/SERVICE \
    --table MY_TABLE \
    --id-column ID \
    --clob-column DATA \
    --id-as-regex
```

### Global Options

- `--debug`: Enable detailed debug logging.
- `--help`: Show help message and exit.

---

## Troubleshooting

- **Connection Errors:** Ensure the DSN is correct and the database is reachable. The tool uses the `python-oracledb` thin driver, so it does not require Oracle Instant Client.
- **ORA-00942: table or view does not exist:** Check that the table names (target table and GTT) are correct and that the user has the necessary permissions.
- **Missing Files (Upload):** Ensure that the filenames in your input directory match the IDs in your CSV file (e.g., if ID is `123`, the file should be named `123`). When using `--id-as-regex`, the filenames must be matched by the patterns in your CSV.
- **CSV Format:** The CSV should have a header. The tool attempts to detect the ID column automatically (e.g., 'ID').
