# TECHNICAL DEBTS - CLOB Download and Upload Tool

This document tracks identified technical debts, security concerns, or architectural limitations that should be addressed in future iterations.

## Identified Debts

### 1. Large File Upload Memory Impact (Resolved)
- **Description:** `CLOBProcessor.read_from_file` currently reads the entire file into memory as a string before returning it. For extremely large CLOBs, this could lead to `MemoryError`.
- **Impact:** Medium - only affects uploads of very large files.
- **Resolution:** Implemented `CLOBProcessor.open_file` and updated `OracleConnector.update_clob` to support streaming from file handles. `Orchestrator.upload_mode` now uses this streaming approach.
- **Logged Date:** 2026-05-15
- **Resolution Date:** 2026-05-16

### 2. Missing Connection Error Handling (Resolved)
- **Description:** `OracleConnector.connect` does not currently handle exceptions (e.g., `oracledb.Error`).
- **Impact:** Low - to be addressed during CLI integration and global error handling implementation.
- **Logged Date:** 2026-05-15
- **Resolution Date:** 2026-05-15 - Implemented try-except in `OracleConnector.connect`.
