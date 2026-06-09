# ROADMAP: Python Implementation Removal

This document outlines the strategy and phases for transitioning the project from a dual-language repository (Python and Java) to a pure Java repository. The goal is to reduce maintenance overhead and consolidate the tool into a single, high-performance implementation.

## Strategy
The transition will follow a phased approach to ensure that no functionality is lost and that the user experience remains consistent throughout the migration.

## Phases

### Phase 1: Feature Parity & Stability
**Goal:** Ensure the Java implementation is a 100% drop-in replacement for the Python tool.

- [ ] Complete all remaining items in `JAVA_ROADMAP.md` (e.g., Asynchronous processing with Virtual Threads).
- [ ] Conduct comprehensive benchmark comparisons between Python and Java versions.
- [ ] Resolve all remaining technical debts (`TECHNICAL_DEBTS.md`) in the Java implementation.
- [ ] Perform a final, exhaustive functional parity check using `test/parity_check.py`.

### Phase 2: Documentation Consolidation
**Goal:** Transition all user-facing and technical documentation to focus exclusively on the Java version.

- [ ] Merge `JAVA_CONCEPT.md` into `CONCEPT.md`.
- [ ] Merge `JAVA_DESIGN.md` into `DESIGN.md`.
- [ ] Update `README.md` to remove Python examples and point to the Java-based binary/source.
- [ ] Update `INSTALL.md` to remove Python installation steps.
- [ ] Remove `JAVA_HOWTO_USE.md` and move its contents to `README.md` or a new `USAGE.md`.

### Phase 3: Infrastructure Migration
**Goal:** Clean up the CI/CD pipelines and repository configuration.

- [ ] Update `.github/workflows/ci.yml` to remove Python build, test, and release steps.
- [ ] Remove `pytest.ini` and other Python-specific configuration files.
- [ ] Update `docker-compose.yml` if it contains Python-specific service configurations.
- [ ] Update `.gitignore` to remove Python-specific entries (e.g., `__pycache__`, `.pytest_cache`, `dist/` from PyInstaller).

### Phase 4: Code & Artifact Removal
**Goal:** Physically remove the Python source code and tests.

- [ ] Delete `src/*.py` files.
- [ ] Delete `src/requirements.txt` and `src/requirements-packaging.txt`.
- [ ] Delete `src/install.sh` (or refactor it to handle Java only).
- [ ] Delete all Python unit tests in `test/`.
- [ ] Delete `test/install.sh` and `test/requirements-test.txt`.
- [ ] Delete `test/parity_check.py`.

### Phase 5: Final Project Restructuring
**Goal:** Finalize the repository structure for a standard Java project.

- [ ] Move `src/main/java/*` to a more standard Maven/Gradle layout if necessary.
- [ ] Remove `JAVA_ROADMAP.md` and rename `REMOVE_PYTHON.md` to `MIGRATION_HISTORY.md` or delete it.
- [ ] Tag a final "Dual-Language" release before the final removal for archival purposes.

---

## Timeline
*This timeline is indicative and subject to change based on the completion of Phase 1.*

- **Phase 1-2:** Estimated 2 weeks.
- **Phase 3-4:** Estimated 1 week.
- **Phase 5:** Final cleanup.
