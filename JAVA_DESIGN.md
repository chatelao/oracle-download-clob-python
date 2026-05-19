# JAVA_DESIGN - CLOB Download and Upload Tool (Java Port)

## Technology Stack

The following technologies have been selected for the Java implementation:

### Development & Production
- **Language:** **Java 21 (LTS)**
  - *Justification:* Provides modern features like Virtual Threads (for high-concurrency potential, though not strictly required here) and improved pattern matching.
- **Database Driver:** **Oracle JDBC Thin Driver (ojdbc11)**
  - *Justification:* The official, high-performance driver for Oracle Database, supporting all modern database features without native client dependencies.
- **Build System & Dependency Management:** **Maven 3.9+**
  - *Justification:* The industry standard for Java project management and dependency resolution.
- **CLI Framework:** **Picocli**
  - *Justification:* An exceptionally powerful and popular library for creating Java command-line applications with minimal code and excellent documentation.
- **Data Handling:** **Apache Commons CSV**
  - *Justification:* A reliable and flexible library for parsing various CSV formats.
- **Packaging:** **Maven Shade Plugin (Executable Uber-JAR)**
  - *Justification:* Creates a single, executable JAR file containing all dependencies, making distribution straightforward on any machine with a JVM.

### Testing
- **Framework:** **JUnit 5 (Jupiter)**
  - *Justification:* The modern standard for Java testing.
- **Mocking:** **Mockito**
  - *Justification:* The most popular mocking framework for Java, essential for isolating components and mocking database/file system interactions.
- **Assertion Library:** **AssertJ**
  - *Justification:* Provides a rich, fluent API for assertions, improving test readability.

---

## Detailed Architecture

### Component Overview
The architecture follows the same functional decomposition as the Python version, mapped to Java packages:

- `com.oracleclob.input.InputManager`
- `com.oracleclob.db.OracleConnector`
- `com.oracleclob.processor.CLOBProcessor`
- `com.oracleclob.fs.FSManager`
- `com.oracleclob.engine.Orchestrator`

### Technical Interfaces

#### 1. Input Manager
- **Interface:** `CsvInputReader`
- **Methods:**
  - `List<String> loadIds(Path filePath)`
  - `boolean validateFormat(Path filePath)`

#### 2. Oracle Database Connector
- **Responsibility:** JDBC connection management and SQL execution.
- **Methods:**
  - `void connect(DBConfig config)`
  - `void createGtt(List<String> ids)`
  - `Stream<ClobRecord> fetchClobsJoin()`
  - `void updateClob(String id, InputStream content)`

#### 3. CLOB Processor
- **Responsibility:** Streaming data between Java `Clob` objects and local files.
- **Methods:**
  - `void streamToFile(Clob clob, Path targetPath)`
  - `InputStream openFile(Path sourcePath)`

#### 4. File System Manager
- **Responsibility:** Directory and file operations using `java.nio.file`.
- **Methods:**
  - `void ensureDirectory(Path path)`
  - `List<Path> listFiles(Path directory, String globPattern)`

#### 5. Orchestrator Engine
- **Methods:**
  - `void runDownload(Path csvPath, Path outputDir)`
  - `void runUpload(Path csvPath, Path inputDir)`

---

## Major Design Choices

### Choice 1: Database Driver Selection
**Chosen Alternative: Oracle JDBC Thin Driver (ojdbc11)**
Selected for its performance, reliability, and lack of native dependencies, ensuring the tool is purely Java-based and portable.

### Choice 2: CLI Framework Selection
**Chosen Alternative: Picocli**
Picocli was chosen over alternatives like `commons-cli` for its annotation-based approach, which leads to cleaner code and easier subcommand management.

### Choice 3: Data Access Strategy
**Chosen Alternative: Raw JDBC with `PreparedStatement` and Batching**
For a tool focused on performance and low-level CLOB streaming, raw JDBC is preferred over heavy ORMs like Hibernate. This ensures minimal overhead and maximum control over the GTT Join and streaming processes.

---

## Packaging & Distribution
The application will be packaged as a single executable JAR using the Maven Shade Plugin.
End-users will run the tool using:
`java -jar oracle-clob-tool.jar download --csv ids.csv --output ./downloads ...`
