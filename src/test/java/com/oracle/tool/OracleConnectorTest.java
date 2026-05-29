package com.oracle.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OracleConnectorTest {

    @Mock
    private Connection connection;
    @Mock
    private Statement statement;
    @Mock
    private PreparedStatement preparedStatement;
    @Mock
    private ResultSet resultSet;
    @Mock
    private ResultSetMetaData resultSetMetaData;

    private OracleConnector connector;
    private DBConfig config;

    @BeforeEach
    void setUp() {
        connector = new OracleConnector();
        config = new DBConfig("user", "pass", "dsn", "table", "id", "clob");
    }

    @Test
    void connect_Success() throws SQLException {
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(connection);

            connector.connect(config);

            verify(connection).setAutoCommit(false);
        }
    }

    @Test
    void createGtt_Success() throws SQLException {
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(connection);
            connector.connect(config);

            when(connection.createStatement()).thenReturn(statement);
            when(connection.prepareStatement(contains("user_tables"))).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt(1)).thenReturn(0); // Not exists

            PreparedStatement insertStmt = mock(PreparedStatement.class);
            when(connection.prepareStatement(contains("INSERT INTO"))).thenReturn(insertStmt);

            connector.createGtt(List.of("1", "2"));

            verify(statement).execute(contains("CREATE GLOBAL TEMPORARY TABLE"));
            verify(statement).execute(contains("(ID_VAL VARCHAR2(255))"));
            verify(statement).execute(contains("DELETE FROM"));
            verify(insertStmt, times(2)).setString(eq(1), anyString());
            verify(insertStmt).executeBatch();
        }
    }

    @Test
    void createGtt_AlreadyExists() throws SQLException {
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(connection);
            connector.connect(config);

            when(connection.createStatement()).thenReturn(statement);
            when(connection.prepareStatement(contains("user_tables"))).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getInt(1)).thenReturn(1); // Already exists

            PreparedStatement insertStmt = mock(PreparedStatement.class);
            when(connection.prepareStatement(contains("INSERT INTO"))).thenReturn(insertStmt);

            connector.createGtt(List.of("1"));

            verify(statement, never()).execute(contains("CREATE"));
            verify(statement).execute(contains("DELETE FROM"));
            verify(insertStmt).executeBatch();
        }
    }

    @Test
    void fetchClobsJoin_ReturnsStream() throws SQLException {
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(connection);
            connector.connect(config);

            when(connection.createStatement()).thenReturn(statement);
            when(statement.executeQuery(anyString())).thenReturn(resultSet);
            when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
            when(resultSetMetaData.getColumnType(2)).thenReturn(Types.CLOB);
            when(resultSet.next()).thenReturn(true, false);
            when(resultSet.getString(1)).thenReturn("1");

            Stream<LobRecord> stream = connector.fetchClobsJoin();
            assertNotNull(stream);
            List<LobRecord> results = stream.toList();

            assertEquals(1, results.size());
            assertEquals("1", results.get(0).id());
            verify(statement).executeQuery(contains("JOIN " + config.gttName() + " g ON t.id = g.ID_VAL"));
        }
    }

    @Test
    void fetchClobsIn_ReturnsStream() throws SQLException {
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(connection);
            connector.connect(config);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
            when(resultSetMetaData.getColumnType(2)).thenReturn(Types.CLOB);
            when(resultSet.next()).thenReturn(true, false);
            when(resultSet.getString(1)).thenReturn("1");

            List<String> ids = List.of("1");
            Stream<LobRecord> stream = connector.fetchClobsIn(ids);
            assertNotNull(stream);
            List<LobRecord> results = stream.toList();

            assertEquals(1, results.size());
            assertEquals("1", results.get(0).id());

            verify(connection).prepareStatement(contains("IN (?)"));
            verify(preparedStatement).setString(1, "1");
        }
    }

    @Test
    void fetchClobsIn_WithQuery() throws SQLException {
        config = new DBConfig("user", "pass", "dsn", "table", "id", "clob", "GTT_IDS", "SELECT * FROM VIEW");
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(connection);
            connector.connect(config);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
            when(resultSetMetaData.getColumnType(2)).thenReturn(Types.CLOB);
            when(resultSet.next()).thenReturn(true, false);
            when(resultSet.getString(1)).thenReturn("1");

            List<String> ids = List.of("1");
            Stream<LobRecord> stream = connector.fetchClobsIn(ids);
            assertNotNull(stream);
            stream.toList();

            verify(connection).prepareStatement(contains("FROM (SELECT * FROM VIEW)"));
        }
    }

    @Test
    void fetchClobsIn_EmptyIds() throws SQLException {
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(connection);
            connector.connect(config);

            Stream<LobRecord> stream = connector.fetchClobsIn(List.of());
            assertEquals(0, stream.count());
            verify(connection, never()).prepareStatement(anyString());
        }
    }

    @Test
    void updateLob_ClobSuccess() throws SQLException {
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(connection);
            connector.connect(config);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

            Reader reader = new StringReader("content");
            connector.updateLob("1", reader);

            verify(preparedStatement).setCharacterStream(eq(1), eq(reader));
            verify(preparedStatement).setString(eq(2), eq("1"));
            verify(preparedStatement).executeUpdate();
        }
    }

    @Test
    void updateLobBatch_Success() throws SQLException {
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(connection);
            connector.connect(config);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeBatch()).thenReturn(new int[]{1, 1});

            Reader reader1 = new StringReader("c1");
            Reader reader2 = new StringReader("c2");
            List<LobUpdate> batch = List.of(
                new LobUpdate("1", reader1, reader1),
                new LobUpdate("2", reader2, reader2)
            );

            int[] results = connector.updateLobBatch(batch);

            assertEquals(2, results.length);
            verify(preparedStatement).setCharacterStream(eq(1), eq(reader1));
            verify(preparedStatement).setString(eq(2), eq("1"));
            verify(preparedStatement).setCharacterStream(eq(1), eq(reader2));
            verify(preparedStatement).setString(eq(2), eq("2"));
            verify(preparedStatement, times(2)).addBatch();
            verify(preparedStatement).executeBatch();
        }
    }

    @Test
    void close_ClosesConnection() throws SQLException {
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(connection);
            connector.connect(config);

            when(connection.isClosed()).thenReturn(false);

            connector.close();

            verify(connection).close();
        }
    }
}
