package com.oracle.tool;

/**
 * Configuration for Oracle database connection and schema.
 */
public record DBConfig(
    String user,
    String password,
    String dsn,
    String targetTable,
    String idColumn,
    String clobColumn,
    String gttName,
    String query
) {
  /**
   * Secondary constructor with default GTT name.
   */
  public DBConfig(String user, String password, String dsn, String targetTable,
      String idColumn, String clobColumn) {
    this(user, password, dsn, targetTable, idColumn, clobColumn, "GTT_IDS", null);
  }

  /**
   * Tertiary constructor with all fields but query.
   */
  public DBConfig(String user, String password, String dsn, String targetTable,
      String idColumn, String clobColumn, String gttName) {
    this(user, password, dsn, targetTable, idColumn, clobColumn, gttName, null);
  }
}
