# Audit: ORA-01722 Invalid Number Error

The error `ORA-01722: Invalid number` was encountered during the execution of the `download` command in the Java implementation. This audit identifies potential causes and recommends corrective actions.

## Stack Trace Analysis
The error occurred at:
```
com.oracle.tool.OracleConnector.fetchClobsIn(OracleConnector.java:182)
```
This indicates the failure happened when executing a query using an `IN` clause strategy for small ID lists.

## Potential Causes

### 1. Inadequate CSV Header Detection
The `InputManager.java` contains the following logic for header detection:
```java
// Simple header detection: if first row is "ID" (case-insensitive), skip it
if (firstRecord && val.equalsIgnoreCase("ID")) {
    firstRecord = false;
    continue;
}
```
**Issue:** If the CSV file has a header other than "ID" (e.g., "IDENTIFIER", "USER_ID") and the corresponding database column is of type `NUMBER`, Oracle will attempt to convert the header string to a number and fail.

**Recommendation:** Ensure the CSV header is exactly "ID" or remove it entirely. Consider updating `InputManager.java` to support custom header names or more robust detection.

### 2. Data Type Mismatch (CSV vs. Database)
The tool reads IDs from the CSV as strings and binds them using `pstmt.setString()`.
```java
for (int i = 0; i < ids.size(); i++) {
    pstmt.setString(i + 1, ids.get(i));
}
```
**Issue:** If the `id-column` in the target table is a numeric type (`NUMBER`, `INTEGER`, etc.), Oracle performs implicit conversion from the bound string to a number. If any value in the CSV (including unexpected characters or malformed numbers) cannot be converted, `ORA-01722` is raised.

**Recommendation:** Verify that all IDs in the CSV file are valid numbers if the database column is numeric. Check for trailing spaces or hidden characters.

### 3. Implicit Conversion in GTT Join Strategy
Although the reported error was in `fetchClobsIn`, the same issue can occur with the GTT Join strategy (`fetchClobsJoin`). The Global Temporary Table is created with a `VARCHAR2(255)` column:
```java
String createSql = String.format(
    "CREATE GLOBAL TEMPORARY TABLE %s (%s VARCHAR2(255)) "
        + "ON COMMIT PRESERVE ROWS",
    config.gttName(), config.idColumn()
);
```
**Issue:** When joining this GTT with a source table where the ID is a `NUMBER`, Oracle may attempt to convert the `VARCHAR2` values from the GTT to `NUMBER` to perform the join, leading to `ORA-01722` if non-numeric data exists in the GTT.

**Recommendation:** If the source table ID is numeric, ensure the GTT is created with a compatible type or use explicit conversion in the join query.

## Corrective Actions for the User
1. **Check the CSV content:** Ensure there are no non-numeric values in the ID column if the database column is a `NUMBER`.
2. **Check the CSV header:** If the first line is not "ID", it is being treated as a data row.
3. **Validate Database Schema:** Confirm the data type of the column specified by `--id-column`.
