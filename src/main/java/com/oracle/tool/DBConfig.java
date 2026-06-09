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
    String filenameColumn,
    String idQuery
) {
  /**
   * Secondary constructor with default GTT name.
   */
  public DBConfig(String user, String password, String dsn, String targetTable,
      String idColumn, String clobColumn) {
    this(user, password, dsn, targetTable, idColumn, clobColumn, "GTT_IDS", null, null, null);
  }

  /**
   * Tertiary constructor with all fields but query and idQuery.
   */
  public DBConfig(String user, String password, String dsn, String targetTable,
      String idColumn, String clobColumn, String gttName) {
    this(user, password, dsn, targetTable, idColumn, clobColumn, gttName, null, null, null);
  }

  /**
   * Quaternary constructor with all fields but filenameColumn and idQuery.
   */
  public DBConfig(String user, String password, String dsn, String targetTable,
      String idColumn, String clobColumn, String gttName, String query) {
    this(user, password, dsn, targetTable, idColumn, clobColumn, gttName, query, null, null);
  }

  /**
   * Constructor used by Download command.
   */
  public DBConfig(String user, String password, String dsn, String targetTable,
      String idColumn, String clobColumn, String gttName, String query, String filenameColumn) {
    this(user, password, dsn, targetTable, idColumn, clobColumn, gttName, query, filenameColumn,
        null);
  }
}
