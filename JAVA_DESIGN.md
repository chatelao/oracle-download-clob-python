# JAVA DESIGN - CLOB Download and Upload Tool

## Technology Stack

The following technologies have been selected for the implementation of the Java version of the CLOB Download and Upload Tool:

### Development & Production
- **Language:** **Java 21**
  - *Justification:* Latest LTS version offering modern features like virtual threads, records, and enhanced switch expressions, providing a robust and performant foundation.
- **Database Driver:** **Oracle JDBC Driver (OJDBC11)**
  - *Justification:* The official driver for Java, providing full support for Oracle-specific features like CLOB streaming and Global Temporary Tables.
- **CLI Framework:** **picocli**
  - *Justification:* A powerful and easy-to-use library for creating Java command-line applications with subcommands, type conversion, and automatic help generation.
- **Build Tool:** **Apache Maven 3.9+**
  - *Justification:* Standard build and dependency management for Java projects.
- **Data Handling:** **Apache Commons CSV**
  - *Justification:* A robust and popular library for parsing and writing CSV files in Java.
- **Logging:** **SLF4J with Logback**
  - *Justification:* SLF4J provides a standard logging facade, and Logback is a high-performance implementation.

### Testing
- **Framework:** **JUnit 5**
  - *Justification:* The modern standard for Java unit testing.
- **Mocking:** **Mockito**
  - *Justification:* The industry standard for creating mock objects in Java tests.
- **Integration Testing:** **Testcontainers**
  - *Justification:* Allows running a real Oracle Database in a Docker container for reliable integration tests.

### Documentation
- **Architecture Diagrams:** **PlantUML**
- **JavaDocs:** Standard Java documentation generation.

---

## Detailed Architecture

### Component Mapping (Python to Java)

| Python Component | Java Class / Interface | Responsibility |
| :--- | :--- | :--- |
| `InputManager` | `com.oracle.tool.InputManager` | Parses CSV and extracts unique IDs using Apache Commons CSV. |
| `OracleConnector` | `com.oracle.tool.OracleConnector` | Manages JDBC connections, GTT lifecycle, and SQL execution. |
| `CLOBProcessor` | `com.oracle.tool.CLOBProcessor` | Handles streaming of CLOB data using `Reader` and `Writer`. |
| `FSManager` | `com.oracle.tool.FSManager` | Manages directory and file operations using `java.nio.file`. |
| `Orchestrator` | `com.oracle.tool.Orchestrator` | Coordinates the high-level business logic for UC-1 and UC-2. |
| `cli.py` | `com.oracle.tool.CliCommand` | Entry point using Picocli to define the command-line interface. |

### Technical Interfaces (Java Examples)

#### 1. Input Manager
- `List<String> loadIds(Path filePath) throws IOException`
- `boolean validateFormat(Path filePath)`

#### 2. Oracle Database Connector
- `void connect(DBConfig config) throws SQLException`
- `void createGtt(List<String> ids) throws SQLException`
- `Stream<ClobRecord> fetchClobsJoin() throws SQLException`
- `void updateClob(String id, Reader content) throws SQLException`

#### 3. CLOB Processor
- `void streamToFile(Clob clob, Path targetPath) throws SQLException, IOException`
- `Reader openFile(Path sourcePath) throws IOException`

---

## Major Design Choices

### Choice 1: Database Driver Selection
**How should the application connect to the Oracle Database?**

- **Alternative 1: Spring Data JPA / Hibernate**
  - *Description:* Full ORM framework.
  - *Pros:* High-level abstraction.
  - *Cons:* Too heavy for a simple CLI tool; potential performance overhead for streaming large LOBs.
- **Alternative 2: JDBC (OJDBC) (Selected)**
  - *Description:* Standard Java Database Connectivity.
  - *Pros:* Direct control over SQL and streaming; lightweight; official Oracle support.
  - *Cons:* More boilerplate compared to high-level frameworks.

**Chosen Alternative: Alternative 2**
Direct JDBC is preferred for its performance, low overhead, and precise control over LOB streaming.

### Choice 2: CLI Framework Selection
**Which library should be used to build the command-line interface?**

- **Alternative 1: Apache Commons CLI**
  - *Description:* Traditional CLI library for Java.
  - *Pros:* Well-known and stable.
  - *Cons:* Very verbose and lacks modern features like subcommand nesting and type conversion.
- **Alternative 2: picocli (Selected)**
  - *Description:* Modern CLI framework for Java.
  - *Pros:* Minimal boilerplate; excellent support for subcommands; can generate GraalVM native images easily.
  - *Cons:* External dependency.

**Chosen Alternative: Alternative 2**
Picocli is the superior choice for modern Java CLI applications due to its developer ergonomics and GraalVM compatibility.

### Choice 3: Packaging and Distribution
**How should the tool be distributed?**

- **Alternative 1: Executable JAR (Fat JAR)**
  - *Description:* A single JAR containing all dependencies.
  - *Pros:* Simple to build; runs anywhere with a JVM.
  - *Cons:* Requires a pre-installed Java Runtime Environment (JRE).
- **Alternative 2: GraalVM Native Image (Selected)**
  - *Description:* Compiles the Java application into a standalone native executable.
  - *Pros:* No JRE required; fast startup; single binary distribution (similar to PyInstaller).
  - *Cons:* More complex build process; platform-specific binaries.

**Chosen Alternative: Alternative 2**
GraalVM Native Image matches the distribution model of the Python version (standalone executable) and provides the best user experience.

---

## Discarded Alternatives Summary
- **ORM:** Discarded in favor of direct JDBC for performance and simplicity.
- **Apache Commons CLI:** Discarded in favor of Picocli for better ergonomics.
- **Standard JAR:** Discarded in favor of Native Image to avoid JRE dependency for end-users.
