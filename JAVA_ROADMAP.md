# JAVA ROADMAP - CLOB Download and Upload Tool

This roadmap outlines the plan to port the CLOB Download and Upload Tool from Python to Java, as defined in `JAVA_CONCEPT.md` and `JAVA_DESIGN.md`.

## Progress Overview

| Phase | Description | Status |
| :--- | :--- | :--- |
| Phase 1 | Project Initialization & Environment Setup | ✅ |
| Phase 2 | Core Component Implementation | ✅ |
| Phase 3 | Orchestration & CLI Development | ✅ |
| Phase 4 | Testing & Quality Assurance | ✅ |
| Phase 5 | Packaging & Release | ✅ |
| Phase 6 | Advanced Filtering & Optimization | ✅ |
| Phase 7 | Configuration Management | ✅ |
| Phase 8 | Advanced Querying | ✅ |
| Phase 9 | Performance & Robustness | 🚧 |

## Goals

- ✅ Initialize Java project with Maven and required dependencies.
- ✅ Port `InputManager` and `FSManager` to Java.
- ✅ Port `OracleConnector` and `CLOBProcessor` using JDBC.
- ✅ Implement `Orchestrator` and Picocli-based CLI.
- ✅ Implement comprehensive JUnit 5 tests and Testcontainers integration.
- ⏳ Implement Java code style enforcement using Checkstyle (Google rules from `alpheusafpparser`).
- ✅ Configure GraalVM Native Image build for standalone distribution.
- ✅ Implement Dual ID Filtering Strategy (IN clause vs. GTT Join).

---

## Phases

### Phase 1: Project Initialization & Environment Setup
Establish the Java development environment and build configuration.

- [x] Initialize Maven project structure (`src/main/java`, `src/test/java`) ✅ 2026-05-16
- [x] Configure `pom.xml` with dependencies (OJDBC, Picocli, Commons CSV, SLF4J/Logback) ✅ 2026-05-16
- [x] Set up GitHub Actions workflow for Java CI ✅ 2026-05-16
- [x] Create `JAVA_CONCEPT.md`, `JAVA_DESIGN.md`, and `JAVA_ROADMAP.md` ✅ 2026-05-16

### Phase 2: Core Component Implementation
Porting the functional logic to Java classes.

- [x] **Input Manager**: Implementation using Apache Commons CSV ✅ 2026-05-16
- [x] **File System Manager**: Implementation using `java.nio.file` ✅ 2026-05-16
- [x] **Oracle Connector**: Implementation using JDBC ✅ 2026-05-16
    - [x] JDBC Connection management ✅ 2026-05-16
    - [x] GTT creation and bulk insert logic ✅ 2026-05-16
    - [x] JOIN-based query execution ✅ 2026-05-16
- [x] **CLOB Processor**: Streaming logic using `Clob.getCharacterStream()` and `Reader.transferTo()` ✅ 2026-05-16

### Phase 3: Orchestration & CLI Development
Integrating components and exposing them via the Java CLI.

- [x] **Orchestrator Engine**: Port Download and Upload mode flows ✅ 2026-05-16
- [x] **CLI Development**: ✅ 2026-05-16
    - [x] Implement `CliCommand` using Picocli ✅ 2026-05-16
    - [x] Integrate logging and error handling ✅ 2026-05-16

### Phase 4: Testing & Quality Assurance
Ensuring the Java implementation is reliable and functionally equivalent to the Python version.

- [x] Write unit tests for all core components using JUnit 5 and Mockito ✅ 2026-05-16
- [x] Implement integration tests using Testcontainers and Oracle Database Free ✅ 2026-05-19
- [x] Perform functional parity checks between Python and Java versions ✅ 2026-05-19
- [x] **Code Quality**: Implement Checkstyle rules from `alpheusafpparser` ✅ 2026-05-20
    - [x] Add `maven-checkstyle-plugin` to `pom.xml` ✅ 2026-05-20
    - [x] Import `checkstyle.xml` from `alpheusafpparser` ✅ 2026-05-20
    - [x] Fix existing checkstyle violations in Java code ✅ 2026-05-20

### Phase 5: Packaging & Release
Automating the build and distribution of the Java application.

- [x] Configure Maven profiles for GraalVM Native Image compilation ✅ 2026-05-19
- [x] Configure Maven for Fat JAR generation using `maven-shade-plugin` ✅ 2026-05-20
- [x] Automate Fat JAR generation and release in CI pipeline ✅ 2026-05-20
- [x] Automate native executable generation in CI pipeline ✅ 2026-05-20
- [x] Update documentation with Java-specific installation and usage instructions ✅ 2026-05-19
- [x] Release Java version 1.0.0 ✅ 2026-05-20

### Phase 6: Advanced Filtering & Optimization
Enhancing the tool's efficiency for varying dataset sizes.

- [x] Implement Dual ID Filtering Strategy (IN clause for < 1000, GTT for >= 1000) ✅ 2026-05-19
- [x] Implement configuration via .ini and .toml files ✅ 2026-05-20

### Phase 7: Configuration Management
Enabling more flexible configuration options for users.

- [x] Add support for loading database parameters from .ini or .toml files ✅ 2026-05-20

### Phase 8: Advanced Querying
Providing users with more control over the data retrieval process.

- [x] Implement `--query` option as an alternative to `--table` for downloads ✅ 2026-05-20

### Phase 9: Performance & Robustness
Improving the tool's efficiency and reliability based on architectural audit recommendations.

- [x] **Optimize GTT lifecycle management**: ✅ 2026-05-29
    - [x] Use generic column names in GTT for better reusability ✅ 2026-05-29
    - [x] Implement explicit existence check for GTT ✅ 2026-05-29
- [x] Implement batch updates for upload mode using `addBatch()` / `executeBatch()` ✅ 2026-05-29
- [ ] **Implement asynchronous processing using Virtual Threads (Project Loom)**:
    - [ ] Research thread-safety of parallel LOB streaming from a single JDBC connection
    - [ ] Implement JDBC connection pooling (e.g., HikariCP) to support parallel execution
    - [ ] Parallelize LOB processing in Orchestrator using Virtual Threads
