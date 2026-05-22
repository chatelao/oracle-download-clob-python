# DESIGN - CLOB Download and Upload Tool

## Technology Stack

The following technologies have been selected for the implementation, production, and testing of the CLOB Download and Upload Tool:

### Development & Production
- **Language:** **Python 3.12+**
  - *Justification:* Python provides excellent libraries for database connectivity and file system operations. Version 3.12+ ensures access to modern language features and performance improvements.
- **Database Driver:** **python-oracledb**
  - *Justification:* The successor to `cx_Oracle`, it offers a "Thin" mode that does not require the Oracle Instant Client, simplifying deployment and CI/CD pipelines.
- **CLI Framework:** **click**
  - *Justification:* Highly composable and easy to use for creating professional command-line interfaces with automatic help generation and argument validation.
- **Packaging:** **PyInstaller**
  - *Justification:* Widely used for creating standalone executables from Python projects, simplifying distribution for end-users without requiring a Python environment.
- **Data Handling:** **csv (standard library)**
  - *Justification:* The built-in `csv` module is sufficient for parsing the ID lists without adding external dependencies like pandas.
- **Configuration Parsing:** **configparser** and **tomllib** (standard library)
  - *Justification:* `configparser` handles INI files, and `tomllib` (available since Python 3.11) handles TOML files, both part of the standard library, avoiding extra dependencies.

### Testing
- **Framework:** **pytest**
  - *Justification:* The industry standard for Python testing, offering simple syntax and powerful plugin support.
- **Mocking:** **unittest.mock (standard library)**
  - *Justification:* Used to simulate database interactions and file system state during unit tests.

### Documentation
- **Architecture Diagrams:** **PlantUML**
- **Publishing:** **ReadTheDocs (RTD)**

---

## Detailed Architecture

### Component Overview
![Top Architecture](https://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/chatelao/oracle-download-clob-python/main/TOP_ARCHITECTURE.puml)

### Technical Interfaces

#### 1. Input Manager (`InputManager` class)
- **Responsibility:** Parses CSV files and extracts a unique list of IDs.
- **Methods:**
  - `load_ids(file_path: Path) -> List[str]`: Reads the CSV and returns a list of strings.
  - `validate_format(file_path: Path) -> bool`: Ensures the CSV contains the expected column.

#### 2. Oracle Database Connector (`OracleConnector` class)
- **Responsibility:** Manages connection lifecycle and executes SQL.
- **Methods:**
  - `connect(config: DBConfig)`: Establishes connection using `python-oracledb`.
  - `create_gtt(ids: List[str])`: Populates the Global Temporary Table with IDs.
  - `fetch_clobs_join() -> Iterator[Tuple[str, LOB]]`: Executes the JOIN query and yields CLOB objects. Supports using a custom query as a subquery.
  - `update_lob(id: str, content: Any)`: Updates a specific record with new LOB data.

#### 3. CLOB Processor (`CLOBProcessor` class)
- **Responsibility:** Handles streaming of large text data.
- **Methods:**
  - `stream_to_file(clob_lob: LOB, target_path: Path)`: Reads from database LOB and writes to disk in chunks.
  - `read_from_file(source_path: Path) -> str`: Reads file content for upload.

#### 4. File System Manager (`FSManager` class)
- **Responsibility:** Abstracts disk I/O and directory management.
- **Methods:**
  - `ensure_directory(path: Path)`: Creates directory if it doesn't exist.
  - `list_files(directory: Path, pattern: str) -> List[Path]`: Matches local files with IDs.

#### 5. Orchestrator Engine (`Orchestrator` class)
- **Responsibility:** High-level execution flow for Download and Upload modes.
- **Methods:**
  - `download_mode(csv_path: Path, output_dir: Path)`: Orchestrates UC-1.
  - `upload_mode(csv_path: Path, input_dir: Path)`: Orchestrates UC-2.

---

## Major Design Choices

### Choice 1: Database Driver Selection
**How should the application connect to the Oracle Database?**

- **Alternative 1: cx_Oracle**
  - *Description:* The traditional Oracle driver for Python.
  - *Pros:* Extremely stable and well-documented.
  - *Cons:* Requires Oracle Instant Client installation on the host machine.
- **Alternative 2: sqlalchemy**
  - *Description:* A full Object-Relational Mapper (ORM).
  - *Pros:* High-level abstraction; database agnostic.
  - *Cons:* Overhead of ORM features not needed for simple CLOB streaming; still requires a driver like `cx_Oracle`.
- **Alternative 3: python-oracledb (Selected)**
  - *Description:* The new "Thin" driver from Oracle.
  - *Pros:* No Instant Client required; faster; official support from Oracle.
  - *Cons:* Slightly newer than `cx_Oracle`.

**Chosen Alternative: Alternative 3**
Selected for its ease of deployment and removal of the Instant Client dependency, which is particularly beneficial for containerized environments and CI/CD.

### Choice 2: CLI Framework Selection
**Which library should be used to build the command-line interface?**

- **Alternative 1: argparse (Standard Library)**
  - *Description:* The built-in Python module for parsing command-line arguments.
  - *Pros:* No external dependencies.
  - *Cons:* Verbose code; less intuitive for complex subcommands.
- **Alternative 2: Typer**
  - *Description:* Built on top of Click, using type hints.
  - *Pros:* Very fast to develop; great IDE support.
  - *Cons:* Additional dependency layer over Click.
- **Alternative 3: Click (Selected)**
  - *Description:* A mature, widely-used package for creating CLIs.
  - *Pros:* Excellent subcommand support; very stable; well-documented.
  - *Cons:* External dependency.

**Chosen Alternative: Alternative 3**
Click provides the best balance of features and stability for a tool that requires distinct modes (Download/Upload) and multiple configuration parameters.

### Choice 3: Logging and Error Handling Strategy
**How should the tool report progress and handle failures?**

- **Alternative 1: Print Statements**
  - *Description:* Simple `print()` calls to stdout.
  - *Pros:* Easiest to implement.
  - *Cons:* No log levels; hard to redirect to files; difficult to debug production issues.
- **Alternative 2: External Observability Platform (e.g., Sentry)**
  - *Description:* Sending errors to a cloud-based monitoring service.
  - *Pros:* Detailed error reports and stack traces.
  - *Cons:* Overkill for a CLI tool; requires internet access and API keys.
- **Alternative 3: Standard Logging Module with Structured Output (Selected)**
  - *Description:* Using Python’s built-in `logging` module configured with a StreamHandler (stderr) and optionally a FileHandler.
  - *Pros:* Configurable levels (INFO, DEBUG, ERROR); easy to integrate into larger systems.
  - *Cons:* Requires boilerplate setup.

**Chosen Alternative: Alternative 3**
The standard `logging` module is the professional choice for CLI tools, providing enough flexibility for both end-users and developers without external complexity.

---

## Discarded Alternatives Summary
- **Database Driver:** `cx_Oracle` was discarded due to the Instant Client requirement; `sqlalchemy` was deemed unnecessarily heavy.
- **CLI Framework:** `argparse` was discarded for lack of developer ergonomics; `Typer` was discarded to minimize the dependency chain.
- **Logging:** `print` was discarded for lack of control; external platforms were discarded as they are unsuitable for a standalone CLI tool.
