package com.oracle.tool;

import java.io.InputStream;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages JDBC connection lifecycle and executes SQL.
 */
public class OracleConnector implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(OracleConnector.class);
  private Connection conn;
  private DBConfig config;

  /**
   * Establishes connection using Oracle JDBC Driver.
   *
   * @param config Database configuration.
   * @throws SQLException If a database access error occurs.
   */
  public void connect(DBConfig config) throws SQLException {
    this.config = config;
    String url = "jdbc:oracle:thin:@" + config.dsn();
    try {
      this.conn = DriverManager.getConnection(url, config.user(), config.password());
      this.conn.setAutoCommit(false);
    } catch (SQLException ex) {
      logger.error("Failed to connect to Oracle database: {}", ex.getMessage());
      throw ex;
    }
  }

  /**
   * Updates multiple records with new LOB data in a single batch.
   *
   * @param ids      List of record IDs.
   * @param contents List of LOB contents (Reader, InputStream, or Object).
   * @return Array of update counts.
   * @throws SQLException If a database access error occurs.
   */
  public int[] updateLobsBatch(List<String> ids, List<Object> contents) throws SQLException {
    if (conn == null) {
      throw new SQLException("Database not connected");
    }
    if (ids.size() != contents.size()) {
      throw new IllegalArgumentException("IDs and contents lists must have the same size");
    }

    String sql = String.format(
        "UPDATE %s SET %s = ? WHERE %s = ?",
        config.targetTable(), config.clobColumn(), config.idColumn()
    );

    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
      for (int i = 0; i < ids.size(); i++) {
        Object content = contents.get(i);
        String id = ids.get(i);
        if (content instanceof Reader reader) {
          pstmt.setCharacterStream(1, reader);
        } else if (content instanceof InputStream is) {
          pstmt.setBinaryStream(1, is);
        } else {
          pstmt.setObject(1, content);
        }
        pstmt.setString(2, id);
        pstmt.addBatch();
      }
      return pstmt.executeBatch();
    }
  }

  /**
   * Closes the database connection.
   */
  @Override
  public void close() throws SQLException {
    if (conn != null && !conn.isClosed()) {
      conn.close();
      conn = null;
    }
  }

  /**
   * Populates the Global Temporary Table with IDs.
   *
   * @param ids List of IDs to insert.
   * @throws SQLException If a database access error occurs.
   */
  public void createGtt(List<String> ids) throws SQLException {
    if (conn == null) {
      throw new SQLException("Database not connected");
    }

    try (Statement stmt = conn.createStatement()) {
      // Check if GTT exists
      boolean exists = false;
      String checkSql = "SELECT count(*) FROM user_tables WHERE table_name = ?";
      try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
        checkStmt.setString(1, config.gttName().toUpperCase());
        try (ResultSet rs = checkStmt.executeQuery()) {
          if (rs.next()) {
            exists = rs.getInt(1) > 0;
          }
        }
      }

      if (!exists) {
        String createSql = String.format(
            "CREATE GLOBAL TEMPORARY TABLE %s (ID_VAL VARCHAR2(255)) "
                + "ON COMMIT PRESERVE ROWS",
            config.gttName()
        );
        stmt.execute(createSql);
      }

      // Clear GTT for the current session
      stmt.execute("DELETE FROM " + config.gttName());
    }

    // Bulk insert IDs
    String insertSql = String.format("INSERT INTO %s (ID_VAL) VALUES (?)",
        config.gttName());
    try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
      for (String id : ids) {
        pstmt.setString(1, id);
        pstmt.addBatch();
      }
      pstmt.executeBatch();
    }
  }

  /**
   * Executes the JOIN query and returns a Stream of LobRecords.
   *
   * @return Stream of LobRecord objects.
   * @throws SQLException If a database access error occurs.
   */
  public Stream<LobRecord> fetchClobsJoin() throws SQLException {
    if (conn == null) {
      throw new SQLException("Database not connected");
    }

    String source = (config.query() != null && !config.query().isEmpty())
        ? "(" + config.query() + ")" : config.targetTable();

    String columns = String.format("t.%s, t.%s", config.idColumn(), config.clobColumn());
    if (config.filenameColumn() != null && !config.filenameColumn().isEmpty()) {
      columns += ", t." + config.filenameColumn();
    }

    String sql = String.format(
        "SELECT %s FROM %s t JOIN %s g ON t.%s = g.ID_VAL",
        columns, source, config.gttName(), config.idColumn()
    );

    Statement stmt = conn.createStatement();
    ResultSet rs = stmt.executeQuery(sql);
    final int columnType = rs.getMetaData().getColumnType(2);
    final boolean hasFilename = config.filenameColumn() != null && !config.filenameColumn().isEmpty();

    return StreamSupport.stream(new Spliterators.AbstractSpliterator<LobRecord>(
        Long.MAX_VALUE, 0) {
      @Override
      public boolean tryAdvance(java.util.function.Consumer<? super LobRecord> action) {
        try {
          if (!rs.next()) {
            return false;
          }
          Object lob;
          if (columnType == Types.CLOB || columnType == Types.NCLOB) {
            lob = rs.getClob(2);
          } else if (columnType == Types.BLOB) {
            lob = rs.getBlob(2);
          } else {
            lob = rs.getObject(2);
          }
          String id = rs.getString(1);
          String filename = hasFilename ? rs.getString(3) : null;
          action.accept(new LobRecord(id, lob, filename));
          return true;
        } catch (SQLException ex) {
          throw new RuntimeException(ex);
        }
      }
    }, false).onClose(() -> {
      try {
        rs.close();
        stmt.close();
      } catch (SQLException ex) {
        logger.error("Error closing ResultSet/Statement: {}", ex.getMessage());
      }
    });
  }

  /**
   * Executes the query with an IN clause and returns a Stream of LobRecords.
   *
   * @param ids List of IDs to fetch.
   * @return Stream of LobRecord objects.
   * @throws SQLException If a database access error occurs.
   */
  public Stream<LobRecord> fetchClobsIn(List<String> ids) throws SQLException {
    if (conn == null) {
      throw new SQLException("Database not connected");
    }

    if (ids.isEmpty()) {
      return Stream.empty();
    }

    String source = (config.query() != null && !config.query().isEmpty())
        ? "(" + config.query() + ")" : config.targetTable();

    StringBuilder sqlBuilder = new StringBuilder();
    sqlBuilder.append("SELECT ").append(config.idColumn())
        .append(", ").append(config.clobColumn());

    if (config.filenameColumn() != null && !config.filenameColumn().isEmpty()) {
      sqlBuilder.append(", ").append(config.filenameColumn());
    }

    sqlBuilder.append(" FROM ").append(source)
        .append(" WHERE ").append(config.idColumn())
        .append(" IN (");

    for (int i = 0; i < ids.size(); i++) {
      sqlBuilder.append("?");
      if (i < ids.size() - 1) {
        sqlBuilder.append(", ");
      }
    }
    sqlBuilder.append(")");

    PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString());
    for (int i = 0; i < ids.size(); i++) {
      pstmt.setString(i + 1, ids.get(i));
    }

    ResultSet rs = pstmt.executeQuery();
    final int columnType = rs.getMetaData().getColumnType(2);
    final boolean hasFilename = config.filenameColumn() != null && !config.filenameColumn().isEmpty();

    return StreamSupport.stream(new Spliterators.AbstractSpliterator<LobRecord>(
        Long.MAX_VALUE, 0) {
      @Override
      public boolean tryAdvance(java.util.function.Consumer<? super LobRecord> action) {
        try {
          if (!rs.next()) {
            return false;
          }
          Object lob;
          if (columnType == Types.CLOB || columnType == Types.NCLOB) {
            lob = rs.getClob(2);
          } else if (columnType == Types.BLOB) {
            lob = rs.getBlob(2);
          } else {
            lob = rs.getObject(2);
          }
          String id = rs.getString(1);
          String filename = hasFilename ? rs.getString(3) : null;
          action.accept(new LobRecord(id, lob, filename));
          return true;
        } catch (SQLException ex) {
          throw new RuntimeException(ex);
        }
      }
    }, false).onClose(() -> {
      try {
        rs.close();
        pstmt.close();
      } catch (SQLException ex) {
        logger.error("Error closing ResultSet/Statement: {}", ex.getMessage());
      }
    });
  }

  /**
   * Updates a specific record with new LOB data.
   *
   * @param id      Record ID.
   * @param content Reader or InputStream providing LOB content.
   * @return Number of rows affected.
   * @throws SQLException If a database access error occurs.
   */
  public int updateLob(String id, Object content) throws SQLException {
    if (conn == null) {
      throw new SQLException("Database not connected");
    }

    String sql = String.format(
        "UPDATE %s SET %s = ? WHERE %s = ?",
        config.targetTable(), config.clobColumn(), config.idColumn()
    );

    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
      if (content instanceof Reader reader) {
        pstmt.setCharacterStream(1, reader);
      } else if (content instanceof InputStream is) {
        pstmt.setBinaryStream(1, is);
      } else {
        pstmt.setObject(1, content);
      }
      pstmt.setString(2, id);
      return pstmt.executeUpdate();
    }
  }

  /**
   * Determines the SQL type of the LOB column.
   *
   * @return SQL type from java.sql.Types.
   * @throws SQLException If a database access error occurs.
   */
  public int getLobColumnType() throws SQLException {
    if (conn == null) {
      throw new SQLException("Database not connected");
    }
    String source = (config.query() != null && !config.query().isEmpty())
        ? "(" + config.query() + ")" : config.targetTable();
    String sql = String.format("SELECT %s FROM %s WHERE 1=0", config.clobColumn(), source);
    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {
      return rs.getMetaData().getColumnType(1);
    }
  }

  /**
   * Commits the current transaction.
   *
   * @throws SQLException If a database access error occurs.
   */
  public void commit() throws SQLException {
    if (conn != null) {
      conn.commit();
    }
  }
}
