# Roadmap to Increase Test Coverage

This document outlines the strategy and phases to increase the test coverage for both Python and Java implementations of the Oracle LOB Download and Upload Tool.

## Current Status (2026-06-09)

| Implementation | Current Coverage | Target Coverage | Key Gaps |
| :--- | :--- | :--- | :--- |
| Python | 82% | 90%+ | `src/cli.py` (78%), `src/orchestrator.py` (76%) |
| Java | 53% | 90%+ | `OracleIntegrationTest` (failing), Unit tests for CLI and Orchestrator edge cases |

## Objectives

- Achieve 90%+ statement coverage in both Python and Java implementations.
- Stabilize integration tests using Oracle Testcontainers.
- Ensure all edge cases, including error handling and resource management, are thoroughly tested.

---

## Phases

### Phase 1: Unit Test Gap Analysis & Quick Wins
Focus on filling the gaps in core components where logic is currently untested.

- [ ] **Python Implementation**:
    - [ ] Increase coverage for `src/cli.py` by testing all command-line options and subcommands.
    - [ ] Increase coverage for `src/orchestrator.py` by mocking various failure scenarios (e.g., database connection loss, filesystem permission issues).
- [ ] **Java Implementation**:
    - [ ] Add unit tests for `CliCommand` to verify argument parsing and default value provider.
    - [ ] Add missing unit tests for `Orchestrator` to cover all logical branches in `downloadMode` and `uploadMode`.

### Phase 2: Integration Test Stabilization
Address the reliability issues in integration tests to ensure they run consistently in CI environments.

- [ ] **Fix Oracle Testcontainers for Java**:
    - [ ] Resolve the "Database name cannot be set to xepdb1" issue in `OracleIntegrationTest.java`.
    - [ ] Optimize the container startup and connection retry mechanism.
- [ ] **Enhance Python Integration Tests**:
    - [ ] Ensure `test_integration.py` covers all supported LOB types (CLOB, BLOB, XMLTYPE).
    - [ ] Add integration tests for configuration file loading (.ini, .toml).

### Phase 3: Edge Case and Error Handling Coverage
Ensure the tool robustly handles unexpected situations.

- [ ] **Resource Safety**:
    - [ ] Verify that all database connections and file handles are correctly closed even in the event of an exception.
- [ ] **Data Integrity**:
    - [ ] Test with zero-length LOBs, extremely large LOBs, and LOBs containing special/Unicode characters.
    - [ ] Test with non-existent IDs and invalid CSV formats.

### Phase 4: Mocking Optimization and Reliability
Improve the quality of mocks to better reflect real-world behavior and reduce test fragility.

- [ ] Refactor tests to use more maintainable mocking strategies.
- [ ] Ensure mocks correctly simulate Oracle-specific behaviors (e.g., ORA error codes).

### Phase 5: Continuous Coverage Monitoring
Integrate coverage reporting into the standard development workflow.

- [ ] Configure GitHub Actions to fail if coverage drops below the established threshold.
- [ ] Generate and publish coverage reports (HTML) for every PR.

---

## Verification Commands

### Python
```bash
pytest --cov=src --cov-report=term-missing test/
```

### Java
```bash
mvn jacoco:prepare-agent test jacoco:report
```
