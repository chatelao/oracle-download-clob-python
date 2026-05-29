# Technical Debts

This file documents technical debts found in the Oracle LOB Download and Upload Tool.

| ID | Description | Location | Status |
| :--- | :--- | :--- | :--- |
| TD-001 | GTT schema uses business-specific column names, reducing reusability and potentially causing conflicts if the same GTT is used for different tables/columns in the same session. | `OracleConnector` (Python & Java) | Resolved |
| TD-002 | GTT lifecycle management relies on catching ORA-00955 exceptions rather than performing explicit existence checks. | `OracleConnector.create_gtt` / `createGtt` | Resolved |
| TD-003 | Hardcoded threshold of 1000 for switching between IN clause and GTT Join strategy. | `Orchestrator` (Python & Java) | Logged |
| TD-004 | Upload mode does not utilize the Dual ID Filtering Strategy, which could improve performance for large file batches. | `Orchestrator.upload_mode` | Logged |
