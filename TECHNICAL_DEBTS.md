# TECHNICAL DEBTS - CLOB Download and Upload Tool

This document tracks identified technical debts, security concerns, or architectural limitations that should be addressed in future iterations.

## Identified Debts

### 1. Large File Upload Memory Impact
- **Description:** `CLOBProcessor.read_from_file` currently reads the entire file into memory as a string before returning it. For extremely large CLOBs, this could lead to `MemoryError`.
- **Impact:** Medium - only affects uploads of very large files.
- **Recommended Fix:** Implement a streaming read/upload mechanism similar to the download's chunked approach, although Oracle's LOB update often requires the whole content or chunked `write` calls.
- **Logged Date:** 2026-05-15

### 2. Missing Connection Error Handling (Resolved)
- **Description:** `OracleConnector.connect` does not currently handle exceptions (e.g., `oracledb.Error`).
- **Impact:** Low - to be addressed during CLI integration and global error handling implementation.
- **Logged Date:** 2026-05-15
- **Resolution Date:** 2026-05-15 - Implemented try-except in `OracleConnector.connect`.
