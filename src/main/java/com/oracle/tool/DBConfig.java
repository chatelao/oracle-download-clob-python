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
    String query,
    String filenameColumn
) {
  /**
   * Secondary constructor with default GTT name.
   */
  public DBConfig(String user, String password, String dsn, String targetTable,
      String idColumn, String clobColumn) {
    this(user, password, dsn, targetTable, idColumn, clobColumn, "GTT_IDS", null, null);
  }

  /**
   * Tertiary constructor with all fields but query and filenameColumn.
   */
  public DBConfig(String user, String password, String dsn, String targetTable,
      String idColumn, String clobColumn, String gttName) {
    this(user, password, dsn, targetTable, idColumn, clobColumn, gttName, null, null);
  }

  /**
   * Quaternary constructor with all fields.
   */
  public DBConfig(String user, String password, String dsn, String targetTable,
      String idColumn, String clobColumn, String gttName, String query) {
    this(user, password, dsn, targetTable, idColumn, clobColumn, gttName, query, null);
  }
}
