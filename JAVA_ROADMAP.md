# JAVA_ROADMAP - CLOB Download and Upload Tool (Java Port)

This roadmap outlines the implementation plan for porting the CLOB Download and Upload Tool to Java, as defined in `JAVA_CONCEPT.md` and `JAVA_DESIGN.md`.

## Progress Overview

| Phase | Description | Status |
| :--- | :--- | :--- |
| Phase 1 | Project Initialization & Build Setup | 🏗️ |
| Phase 2 | Core Java Components Implementation | ⏳ |
| Phase 3 | Orchestration & CLI Development (Java) | ⏳ |
| Phase 4 | Testing & Quality Assurance | ⏳ |
| Phase 5 | Packaging & Release | ⏳ |

## Goals

- ⏳ Port the complete functionality of the Python tool to Java 21.
- ⏳ Maintain high performance using the GTT Join strategy and JDBC streaming.
- ⏳ Provide a user-friendly CLI using Picocli.
- ⏳ Ensure 100% test coverage for core logic using JUnit 5 and Mockito.
- ⏳ Automate the build and packaging process with Maven.

---

## Phases

### Phase 1: Project Initialization & Build Setup
Establishing the Java project structure and build environment.

- [x] Create `JAVA_CONCEPT.md` and `JAVA_DESIGN.md` 2026-05-16
- [x] Create `JAVA_ROADMAP.md` 2026-05-16
- [ ] Initialize Maven project structure (`pom.xml`, `src/main/java`, `src/test/java`)
- [ ] Configure dependencies (OJDBC, Picocli, Commons CSV, JUnit 5)
- [ ] Setup CI pipeline for Java (GitHub Actions)

### Phase 2: Core Java Components Implementation
Implementing the functional logic in Java.

- [ ] **Input Manager**: Implementation of `CsvInputReader` using Apache Commons CSV.
- [ ] **File System Manager**: Implementation using `java.nio.file`.
- [ ] **Oracle Connector**:
    - [ ] JDBC connection lifecycle management.
    - [ ] GTT creation and JDBC Batch insertion.
    - [ ] Join-based fetch logic with `ResultSet` streaming.
- [ ] **CLOB Processor**: Streaming logic using Java `Clob` and `InputStream/OutputStream`.

### Phase 3: Orchestration & CLI Development (Java)
Integrating components and building the CLI.

- [ ] **Orchestrator Engine**: Implementation of Download and Upload flows.
- [ ] **CLI Development**:
    - [ ] Implement Picocli commands and subcommands.
    - [ ] Integrate SLF4J/Logback for logging.

### Phase 4: Testing & Quality Assurance
Ensuring the Java port is robust and correct.

- [ ] Unit tests for all core components (JUnit 5 + Mockito).
- [ ] Integration tests with a real Oracle instance (Testcontainers or Docker).
- [ ] Performance benchmarking against the Python version.

### Phase 5: Packaging & Release
Finalizing the Java artifact for distribution.

- [ ] Configure Maven Shade Plugin for executable JAR generation.
- [ ] Automated release workflow for the JAR artifact.
- [ ] Final documentation update (README.md updates for Java).
