# Java CLI Usage Guide

This guide describes how to build and use the Java implementation of the Oracle CLOB Download and Upload Tool.

## Prerequisites

- **Java Development Kit (JDK):** Version 21 or higher.
- **Apache Maven:** Version 3.9 or higher.
- **Oracle Database:** Access to an Oracle Database (12c or higher).

## Building the Tool

To build the executable fat JAR, run the following command from the project root:

```bash
mvn clean package
```

The generated JAR file will be located at `target/oracle-clob-tool.jar`.

## Usage

The tool provides two main commands: `download` and `upload`.

### Global Options

- `--debug`: Enables detailed debug logging.
- `--help`: Shows the help message and exits.
- `--version`: Shows the version information and exits.

### 1. Download CLOBs to Local Files

Downloads CLOB data from Oracle based on a list of IDs in a CSV file.

```bash
java -jar target/oracle-clob-tool.jar download \
    --csv-path ids.csv \
    --output-dir ./output \
    --user MYUSER \
    --password MYPASS \
    --dsn localhost:1521/FREEPDB1 \
    --table MY_TABLE \
    --id-column ID \
    --clob-column CONTENT \
    --gtt-name GTT_IDS
```

#### Options for `download`:

| Option | Required | Description | Default |
| :--- | :--- | :--- | :--- |
| `--csv-path` | Yes | Path to the CSV file containing IDs. | |
| `--output-dir` | Yes | Target directory for downloaded files. | |
| `--user` | Yes | Oracle DB username. | |
| `--password` | Yes | Oracle DB password. | |
| `--dsn` | Yes | Oracle DB DSN (e.g., `host:port/service`). | |
| `--table` | No | Target table name (required if `--query` is not provided). | |
| `--query` | No | User written SELECT statement (required if `--table` is not provided). | |
| `--id-column` | Yes | Column name for IDs. | |
| `--clob-column, --lob-column` | Yes | Column name for CLOBs or BLOBs. | |
| `--filename-column` | No | Column name for custom filenames. | |
| `--gtt-name` | No | Name of the Global Temporary Table. | `GTT_IDS` |

#### Using Custom Filenames from a Query

When downloading, you can specify a column to be used as the filename for each LOB. If `--filename-column` is not provided, the tool defaults to using the record ID with a `.txt` extension.

This is particularly powerful when combined with the `--query` option:

```bash
java -jar target/oracle-clob-tool.jar download \
    --csv-path ids.csv \
    --output-dir ./output \
    --user MYUSER \
    --password MYPASS \
    --dsn localhost:1521/FREEPDB1 \
    --query "SELECT ID, CONTENT, FILE_NAME || '.pdf' as TARGET_NAME FROM MY_TABLE" \
    --id-column ID \
    --clob-column CONTENT \
    --filename-column TARGET_NAME
```

### 2. Upload Local Files to Oracle CLOBs

Uploads local file content into Oracle CLOB fields, matching IDs from a CSV file with filenames.

```bash
java -jar target/oracle-clob-tool.jar upload \
    --csv-path ids.csv \
    --input-dir ./input \
    --user MYUSER \
    --password MYPASS \
    --dsn localhost:1521/FREEPDB1 \
    --table MY_TABLE \
    --id-column ID \
    --clob-column CONTENT
```

#### Options for `upload`:

| Option | Required | Description |
| :--- | :--- | :--- |
| `--csv-path` | Yes | Path to the CSV file containing IDs. |
| `--input-dir` | Yes | Source directory containing files to upload. |
| `--user` | Yes | Oracle DB username. |
| `--password` | Yes | Oracle DB password. |
| `--dsn` | Yes | Oracle DB DSN (e.g., `host:port/service`). |
| `--table` | Yes | Target table name. |
| `--id-column` | Yes | Column name for IDs. |
| `--clob-column, --lob-column` | Yes | Column name for CLOBs or BLOBs. |

---

## Configuration Files

The tool supports loading parameters from TOML or INI configuration files using the `--config` option. This is useful for storing database credentials and connection details.

**Note:** Command-line arguments consistently override configuration file defaults. The `--csv-path` must always be provided via the command line.

### TOML Example (`config.toml`)

```toml
user = "MYUSER"
password = "MYPASS"
dsn = "localhost:1521/FREEPDB1"
# Use table:
table = "MY_TABLE"
# OR use query:
# query = "SELECT * FROM MY_TABLE WHERE STATUS = 'ACTIVE'"
id-column = "ID"
clob-column = "CONTENT"
# lob-column = "CONTENT" # Alias for clob-column
filename-column = "TARGET_NAME"
gtt-name = "GTT_IDS"
```

### INI Example (`config.ini`)

The tool looks for settings under the `[oracle-clob-tool]` or `[DEFAULT]` section.

```ini
[oracle-clob-tool]
user = MYUSER
password = MYPASS
dsn = localhost:1521/FREEPDB1
table = MY_TABLE
# query = SELECT * FROM MY_TABLE WHERE STATUS = 'ACTIVE'
id-column = ID
clob-column = CONTENT
# lob-column = CONTENT
filename-column = TARGET_NAME
gtt-name = GTT_IDS
```

### Usage with Config File

```bash
java -jar target/oracle-clob-tool.jar download \
    --config config.toml \
    --csv-path ids.csv \
    --output-dir ./output
```

---

## Performance Tip: Dual ID Filtering

The tool automatically optimizes ID filtering based on the number of records in your CSV:

- **< 1000 IDs:** Uses a standard SQL `IN` clause.
- **>= 1000 IDs:** Uses a Global Temporary Table (GTT) and a `JOIN` operation for better performance.

If you are using the GTT approach, ensure the GTT exists in your schema:

```sql
CREATE GLOBAL TEMPORARY TABLE GTT_IDS (
    ID VARCHAR2(255)
) ON COMMIT PRESERVE ROWS;
```
