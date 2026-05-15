# ROADMAP - CLOB Download and Upload Tool

This roadmap outlines the implementation plan for the CLOB Download and Upload Tool, as defined in `CONCEPT.md` and `DESIGN.md`.

## Progress Overview

| Phase | Description | Status |
| :--- | :--- | :--- |
| Phase 1 | Project Foundation & CI/CD Setup | ✅ |
| Phase 2 | Technical Interface Definitions | ✅ |
| Phase 3 | Core Component Implementation | ⏳ |
| Phase 4 | Orchestration & CLI Development | ⏳ |
| Phase 5 | Documentation & Finalization | ⏳ |

## Goals

- ✅ Define project concept and business use cases.
- ✅ Define detailed technical design and architecture.
- ✅ Establish a robust CI/CD pipeline and project structure.
- ⏳ Implement high-performance CLOB download using GTT Join strategy.
- ⏳ Implement reliable CLOB upload from local files.
- ⏳ Provide a user-friendly CLI for data operations.

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
- [ ] **Oracle Connector**: 🚧
    - [ ] Connection lifecycle management ⏳
    - [ ] Global Temporary Table (GTT) creation and population ⏳
    - [ ] Join-based fetch logic ⏳
- [ ] **CLOB Processor**: Streaming logic for reading/writing LOBs to/from files ⏳

### Phase 4: Orchestration & CLI Development
Integrating components and exposing them via the command-line interface.

- [ ] **Orchestrator Engine**: ⏳
    - [ ] Implement Download mode flow (UC-1) ⏳
    - [ ] Implement Upload mode flow (UC-2) ⏳
- [ ] **CLI Development**: ⏳
    - [ ] Implement `click` commands and subcommands ⏳
    - [ ] Integrate logging and error handling ⏳

### Phase 5: Documentation & Finalization
Ensuring the project is well-documented and meets all quality standards.

- [ ] Configure ReadTheDocs (RTD) for automated documentation publishing ⏳
- [ ] Perform end-to-end integration testing with an Oracle instance ⏳
- [ ] Final code review and technical debt assessment ⏳
- [ ] Release version 1.0.0 ⏳
