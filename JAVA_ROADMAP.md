# JAVA ROADMAP - CLOB Download and Upload Tool

This roadmap outlines the plan to port the CLOB Download and Upload Tool from Python to Java, as defined in `JAVA_CONCEPT.md` and `JAVA_DESIGN.md`.

## Progress Overview

| Phase | Description | Status |
| :--- | :--- | :--- |
| Phase 1 | Project Initialization & Environment Setup | ✅ |
| Phase 2 | Core Component Implementation | ✅ |
| Phase 3 | Orchestration & CLI Development | 🏗️ |
| Phase 4 | Testing & Quality Assurance | ⏳ |
| Phase 5 | Packaging & Release | ⏳ |

## Goals

- ✅ Initialize Java project with Maven and required dependencies.
- ✅ Port `InputManager` and `FSManager` to Java.
- ✅ Port `OracleConnector` and `CLOBProcessor` using JDBC.
- ⏳ Implement `Orchestrator` and Picocli-based CLI.
- ⏳ Implement comprehensive JUnit 5 tests and Testcontainers integration.
- ⏳ Configure GraalVM Native Image build for standalone distribution.

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

- [ ] **Orchestrator Engine**: Port Download and Upload mode flows 🏗️
- [ ] **CLI Development**: ⏳
    - [ ] Implement `CliCommand` using Picocli ⏳
    - [ ] Integrate logging and error handling ⏳

### Phase 4: Testing & Quality Assurance
Ensuring the Java implementation is reliable and functionally equivalent to the Python version.

- [ ] Write unit tests for all core components using JUnit 5 and Mockito ⏳
- [ ] Implement integration tests using Testcontainers and Oracle Database Free ⏳
- [ ] Perform functional parity checks between Python and Java versions ⏳

### Phase 5: Packaging & Release
Automating the build and distribution of the Java application.

- [ ] Configure Maven profiles for GraalVM Native Image compilation ⏳
- [ ] Automate native executable generation in CI pipeline ⏳
- [ ] Update documentation with Java-specific installation and usage instructions ⏳
- [ ] Release Java version 1.0.0 ⏳
