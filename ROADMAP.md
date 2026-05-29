# ROADMAP - CLOB Download and Upload Tool

This roadmap outlines the implementation plan for the CLOB Download and Upload Tool, as defined in `CONCEPT.md` and `DESIGN.md`.

## Progress Overview

| Phase | Description | Status |
| :--- | :--- | :--- |
| Phase 1 | Project Foundation & CI/CD Setup | ✅ |
| Phase 2 | Technical Interface Definitions | ✅ |
| Phase 3 | Core Component Implementation | ✅ |
| Phase 4 | Orchestration & CLI Development | ✅ |
| Phase 5 | Documentation & Finalization | ✅ |
| Phase 6 | Advanced Filtering & Optimization | ✅ |
| Phase 7 | Configuration Management | ✅ |
| Phase 8 | Advanced Querying | ✅ |
| Phase 9 | Performance & Robustness | 🚧 |

## Goals

- ✅ Define project concept and business use cases.
- ✅ Define detailed technical design and architecture.
- ✅ Establish a robust CI/CD pipeline and project structure.
- ✅ Implement high-performance CLOB download using GTT Join strategy.
- ✅ Implement reliable CLOB upload from local files.
- ✅ Provide a user-friendly CLI for data operations.
- ✅ Implement Dual ID Filtering Strategy (IN clause vs. GTT Join).

---

## Phases

### Phase 1: Project Foundation & CI/CD Setup
Focuses on establishing the development environment and automated testing pipelines.

- [x] Create `CONCEPT.md` and `DESIGN.md` 2024-05-15
- [x] Create `TOP_ARCHITECTURE.puml` 2024-05-15
- [x] Initialize repository structure (`src/`, `test/`, `specification/`, `build/`) 2024-05-15
- [x] Create `ROADMAP.md` 2024-05-15
- [x] Implement `src/install.sh` for build tool dependencies ✅ 2024-05-16
- [x] Implement `test/install.sh` for testing tool dependencies ✅ 2024-05-16
- [x] Configure GitHub Actions workflow for CI/CD ✅ 2024-05-16
- [x] Setup initial `pytest` configuration ✅ 2024-05-16

### Phase 2: Technical Interface Definitions
Defining interfaces early to allow for potential parallel development and clear contract-based implementation.

- [x] Define `InputManager` class interface ✅ 2026-05-15
- [x] Define `OracleConnector` class interface ✅ 2026-05-15
- [x] Define `CLOBProcessor` class interface ✅ 2026-05-15
- [x] Define `FSManager` class interface ✅ 2026-05-15
- [x] Define `Orchestrator` class interface ✅ 2026-05-15

### Phase 3: Core Component Implementation
Building the functional logic of the individual modules.

- [x] **Input Manager**: CSV parsing and ID validation logic ✅ 2026-05-15
- [x] **File System Manager**: Directory management and file matching ✅ 2026-05-15
- [x] **Oracle Connector**: ✅ 2026-05-15
    - [x] Connection lifecycle management ✅ 2026-05-15
    - [x] Global Temporary Table (GTT) creation and population ✅ 2026-05-15
    - [x] Join-based fetch logic ✅ 2026-05-15
- [x] **CLOB Processor**: Streaming logic for reading/writing LOBs to/from files ✅ 2026-05-15

### Phase 4: Orchestration & CLI Development
Integrating components and exposing them via the command-line interface.

- [x] **Orchestrator Engine**: ✅ 2026-05-15
    - [x] Implement Download mode flow (UC-1) ✅ 2026-05-15
    - [x] Implement Upload mode flow (UC-2) ✅ 2026-05-15
- [x] **CLI Development**: ✅ 2026-05-15
    - [x] Implement `click` commands and subcommands ✅ 2026-05-15
    - [x] Integrate logging and error handling ✅ 2026-05-15

### Phase 5: Documentation & Finalization
Ensuring the project is well-documented and meets all quality standards.

- [x] Configure ReadTheDocs (RTD) for automated documentation publishing ✅ 2026-05-16
- [x] Perform end-to-end integration testing with an Oracle instance ✅ 2026-05-16
- [x] Final code review and technical debt assessment ✅ 2026-05-16
- [x] Implement automated packaging and release of standalone executable ✅ 2026-05-15
- [x] Release version 1.0.0 ✅ 2026-05-16

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

- [x] **Optimize GTT lifecycle management**:
    - [x] Use generic column names in GTT for better reusability
    - [x] Implement explicit existence check for GTT
- [x] Implement batch updates for upload mode to improve performance
