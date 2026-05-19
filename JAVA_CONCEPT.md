# JAVA CONCEPT - CLOB Download and Upload Tool

## Goal
Allow to download and upload CLOB fields from/to files, based on a .csv list of ids to use in the WHERE / filter clauses. This document outlines the concept for the **Java-based implementation** of the tool.

## Business Cases
- **Data Portability:** Facilitates the extraction of large text data (CLOBs) from Oracle databases for external analysis, migration, or reporting.
- **Offline Content Management:** Enables developers and business users to download database-resident content (like XML, JSON, or large documents), edit them using local tools, and sync changes back to the database.
- **Granular Data Recovery:** Provides a mechanism to restore or update specific records from a file-based backup using a list of identifiers, avoiding the need for full table restores.

## Use Cases
### UC-1: Download CLOBs to Files
- **Actor:** User / Automated Process
- **Preconditions:** Oracle database is accessible; CSV file with target IDs exists.
- **Flow:**
  1. Provide path to the CSV file containing the IDs.
  2. Provide the target directory for the downloaded files.
  3. System connects to Oracle.
  4. System reads IDs and fetches corresponding CLOB data.
  5. System saves each CLOB into a file named after its ID.
- **Postconditions:** Files are created in the target directory for all found IDs.

### UC-2: Upload Files to CLOBs
- **Actor:** User / Automated Process
- **Preconditions:** Oracle database is accessible; CSV file with IDs exists; Source files exist in a directory.
- **Flow:**
  1. Provide path to the CSV file containing the IDs.
  2. Provide the source directory containing files to upload.
  3. System connects to Oracle.
  4. System matches IDs in CSV with filenames in the directory.
  5. System updates the CLOB field in the database with the content of the corresponding file.
- **Postconditions:** Database CLOB fields are updated with new content.

## High-Level Architecture

### Functional Components
- **Input Manager:** Responsible for parsing the input CSV file and validating the list of IDs.
- **Oracle Database Connector:** Handles the lifecycle of the database connection using Java JDBC and Oracle JDBC Driver (OJDBC).
- **CLOB Processor:** Manages the streaming of CLOB data using Java IO/NIO to prevent memory exhaustion when handling very large fields.
- **File System Manager:** Abstracts the reading and writing operations to the local disk using `java.nio.file`.
- **Orchestrator Engine:** Coordinates the data flow between the components based on the selected mode (Download or Upload).

### Business Interfaces
- **Command Line Interface (CLI):** Primary interface for users to trigger operations and provide configuration (CSV path, DB credentials, Directories).
- **Database Schema API:** The set of SQL queries and procedures used to interact with the target tables.

## Major Architectural Choice: ID Filtering Strategy
When processing a large list of IDs from a CSV, how should the application filter the database records?

### Alternative 1: IN Clause with Batching
- **Description:** Construct SELECT/UPDATE statements using an `IN` clause, batching the IDs into groups of up to 1000.
- **Pros:** Does not require temporary objects in the database.
- **Cons:** Limited by Oracle's 1000-expression limit in `IN` lists; multiple round-trips required.

### Alternative 2: Individual Statements
- **Description:** Execute a single database call for every ID in the CSV.
- **Pros:** Simple implementation.
- **Cons:** Significant performance degradation for large datasets due to network latency and per-statement overhead.

### Alternative 3: Global Temporary Table (GTT) Join
- **Description:** Bulk insert the IDs from the CSV into a Global Temporary Table (GTT) and then perform a JOIN with the target business table.
- **Pros:** Highest performance for large datasets; leverages the Oracle optimizer; set-based operation.
- **Cons:** Requires the existence of a GTT or permissions to create one.

### Alternative 4: Dual Filtering Strategy (Selected)
- **Description:** Combines the simplicity of `IN` clauses with the power of GTT joins based on the dataset size.
- **Implementation:**
    - Use "IN" filtering for small datasets (up to 999 records).
    - Use GTT Join strategy for larger datasets (>= 1000 records).
- **Pros:** Avoids GTT overhead for small tasks while maintaining high performance for large ones; respects Oracle's 1000-expression limit for `IN` lists.
- **Cons:** Slightly more complex orchestration logic.

### Chosen Alternative: Alternative 4
Alternative 4 is chosen because it provides the best balance between implementation simplicity for common small-scale tasks and high-performance scalability for large data volumes.
