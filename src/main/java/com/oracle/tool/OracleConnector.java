package com.oracle.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
        } catch (SQLException e) {
            logger.error("Failed to connect to Oracle database: {}", e.getMessage());
            throw e;
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
            // Attempt to create GTT if it doesn't exist
            try {
                String createSql = String.format(
                    "CREATE GLOBAL TEMPORARY TABLE %s (%s VARCHAR2(255)) ON COMMIT PRESERVE ROWS",
                    config.gttName(), config.idColumn()
                );
                stmt.execute(createSql);
            } catch (SQLException e) {
                if (e.getErrorCode() != 955) { // ORA-00955: name is already used by an existing object
                    throw e;
                }
            }

            // Clear GTT for the current session
            stmt.execute("DELETE FROM " + config.gttName());
        }

        // Bulk insert IDs
        String insertSql = String.format("INSERT INTO %s (%s) VALUES (?)", config.gttName(), config.idColumn());
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            for (String id : ids) {
                pstmt.setString(1, id);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    /**
     * Executes the JOIN query and returns a Stream of ClobRecords.
     *
     * @return Stream of ClobRecord objects.
     * @throws SQLException If a database access error occurs.
     */
    public Stream<ClobRecord> fetchClobsJoin() throws SQLException {
        if (conn == null) {
            throw new SQLException("Database not connected");
        }

        String sql = String.format(
            "SELECT t.%s, t.%s FROM %s t JOIN %s g ON t.%s = g.%s",
            config.idColumn(), config.clobColumn(), config.targetTable(),
            config.gttName(), config.idColumn(), config.idColumn()
        );

        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);

        return StreamSupport.stream(new Spliterators.AbstractSpliterator<ClobRecord>(Long.MAX_VALUE, 0) {
            @Override
            public boolean tryAdvance(java.util.function.Consumer<? super ClobRecord> action) {
                try {
                    if (!rs.next()) return false;
                    action.accept(new ClobRecord(rs.getString(1), rs.getClob(2)));
                    return true;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }, false).onClose(() -> {
            try {
                rs.close();
                stmt.close();
            } catch (SQLException e) {
                logger.error("Error closing ResultSet/Statement: {}", e.getMessage());
            }
        });
    }

    /**
     * Executes the query with an IN clause and returns a Stream of ClobRecords.
     *
     * @param ids List of IDs to fetch.
     * @return Stream of ClobRecord objects.
     * @throws SQLException If a database access error occurs.
     */
    public Stream<ClobRecord> fetchClobsIn(List<String> ids) throws SQLException {
        if (conn == null) {
            throw new SQLException("Database not connected");
        }

        if (ids.isEmpty()) {
            return Stream.empty();
        }

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("SELECT ").append(config.idColumn())
                  .append(", ").append(config.clobColumn())
                  .append(" FROM ").append(config.targetTable())
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

        return StreamSupport.stream(new Spliterators.AbstractSpliterator<ClobRecord>(Long.MAX_VALUE, 0) {
            @Override
            public boolean tryAdvance(java.util.function.Consumer<? super ClobRecord> action) {
                try {
                    if (!rs.next()) return false;
                    action.accept(new ClobRecord(rs.getString(1), rs.getClob(2)));
                    return true;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }, false).onClose(() -> {
            try {
                rs.close();
                pstmt.close();
            } catch (SQLException e) {
                logger.error("Error closing ResultSet/Statement: {}", e.getMessage());
            }
        });
    }

    /**
     * Updates a specific record with new CLOB data.
     *
     * @param id      Record ID.
     * @param content Reader providing CLOB content.
     * @throws SQLException If a database access error occurs.
     */
    public void updateClob(String id, Reader content) throws SQLException {
        if (conn == null) {
            throw new SQLException("Database not connected");
        }

        String sql = String.format(
            "UPDATE %s SET %s = ? WHERE %s = ?",
            config.targetTable(), config.clobColumn(), config.idColumn()
        );

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setCharacterStream(1, content);
            pstmt.setString(2, id);
            pstmt.executeUpdate();
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
