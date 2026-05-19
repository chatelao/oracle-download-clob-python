# JAVA_CONCEPT - CLOB Download and Upload Tool (Java Port)

## Goal
The goal of this project is to provide a 100% Java implementation of the CLOB Download and Upload Tool, ensuring high performance and cross-platform compatibility through the Java Virtual Machine (JVM).

## Business Cases
- **Enterprise Integration:** Seamlessly integrates with existing Java-based enterprise ecosystems and middleware.
- **Data Portability:** Facilitates the extraction of large text data (CLOBs) from Oracle databases for external analysis, migration, or reporting.
- **Offline Content Management:** Enables developers and business users to download database-resident content, edit them using local tools, and sync changes back to the database.
- **Granular Data Recovery:** Provides a mechanism to restore or update specific records from a file-based backup using a list of identifiers.

## Use Cases
### UC-1: Download CLOBs to Files
- **Actor:** User / Automated Process
- **Preconditions:** Oracle database is accessible; CSV file with target IDs exists.
- **Flow:**
  1. Provide path to the CSV file containing the IDs.
  2. Provide the target directory for the downloaded files.
  3. System connects to Oracle via JDBC.
  4. System reads IDs and fetches corresponding CLOB data using a GTT Join.
  5. System saves each CLOB into a file named after its ID.
- **Postconditions:** Files are created in the target directory for all found IDs.

### UC-2: Upload Files to CLOBs
- **Actor:** User / Automated Process
- **Preconditions:** Oracle database is accessible; CSV file with IDs exists; Source files exist in a directory.
- **Flow:**
  1. Provide path to the CSV file containing the IDs.
  2. Provide the source directory containing files to upload.
  3. System connects to Oracle via JDBC.
  4. System matches IDs in CSV with filenames in the directory.
  5. System updates the CLOB field in the database with the content of the corresponding file using streaming.
- **Postconditions:** Database CLOB fields are updated with new content.

## High-Level Architecture

### Functional Components
- **Input Manager (`com.oracleclob.input`):** Responsible for parsing the input CSV file using a robust CSV library.
- **Oracle Database Connector (`com.oracleclob.db`):** Handles the lifecycle of the database connection using the Oracle JDBC Thin driver.
- **CLOB Processor (`com.oracleclob.processor`):** Manages the streaming of CLOB data using Java's `Reader` and `Writer` interfaces to prevent memory exhaustion.
- **File System Manager (`com.oracleclob.fs`):** Abstracts the reading and writing operations to the local disk using Java NIO.
- **Orchestrator Engine (`com.oracleclob.engine`):** Coordinates the data flow between the components based on the selected mode.

### Business Interfaces
- **Command Line Interface (CLI):** Implemented using a Java CLI framework (e.g., Picocli).
- **Database Schema API:** The set of SQL queries and procedures used to interact with the target tables.

## Major Architectural Choice: ID Filtering Strategy
We maintain the **Global Temporary Table (GTT) Join** strategy as the most performant approach for large datasets in the Java port.

### Alternative 3: Global Temporary Table (GTT) Join (Selected)
- **Description:** Bulk insert the IDs from the CSV into a Global Temporary Table (GTT) using JDBC Batch updates and then perform a JOIN with the target business table.
- **Pros:** Highest performance; leverages the Oracle optimizer; set-based operation.
- **Cons:** Requires the existence of a GTT or permissions to create one.

## Discarded Alternatives Summary
- **IN Clause with Batching:** Discarded due to Oracle's 1000-expression limit and increased application logic complexity.
- **Individual Statements:** Discarded due to lack of scalability and poor performance.
