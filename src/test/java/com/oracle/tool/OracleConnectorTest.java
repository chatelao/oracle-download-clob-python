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
import java.sql.SQLException;
import java.sql.Statement;
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
        // We need to set the connection manually since it's private and we don't want to mock static everywhere
        // Or we just use the connect method with mockStatic
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(connection);
            connector.connect(config);

            when(connection.createStatement()).thenReturn(statement);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

            connector.createGtt(List.of("1", "2"));

            verify(statement).execute(contains("CREATE GLOBAL TEMPORARY TABLE"));
            verify(statement).execute(contains("DELETE FROM"));
            verify(preparedStatement, times(2)).setString(eq(1), anyString());
            verify(preparedStatement).executeBatch();
        }
    }

    @Test
    void createGtt_AlreadyExists() throws SQLException {
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(connection);
            connector.connect(config);

            when(connection.createStatement()).thenReturn(statement);
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

            // Mock ORA-00955
            doThrow(new SQLException("Table already exists", "42000", 955)).when(statement).execute(contains("CREATE"));

            connector.createGtt(List.of("1"));

            verify(statement).execute(contains("DELETE FROM"));
            verify(preparedStatement).executeBatch();
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
            when(resultSet.next()).thenReturn(true, false);
            when(resultSet.getString(1)).thenReturn("1");

            Stream<ClobRecord> stream = connector.fetchClobsJoin();
            assertNotNull(stream);
            List<ClobRecord> results = stream.toList();

            assertEquals(1, results.size());
            assertEquals("1", results.get(0).id());
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
            when(resultSet.next()).thenReturn(true, false);
            when(resultSet.getString(1)).thenReturn("1");

            List<String> ids = List.of("1");
            Stream<ClobRecord> stream = connector.fetchClobsIn(ids);
            assertNotNull(stream);
            List<ClobRecord> results = stream.toList();

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
            when(resultSet.next()).thenReturn(true, false);
            when(resultSet.getString(1)).thenReturn("1");

            List<String> ids = List.of("1");
            Stream<ClobRecord> stream = connector.fetchClobsIn(ids);
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

            Stream<ClobRecord> stream = connector.fetchClobsIn(List.of());
            assertEquals(0, stream.count());
            verify(connection, never()).prepareStatement(anyString());
        }
    }

    @Test
    void updateClob_Success() throws SQLException {
        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(connection);
            connector.connect(config);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

            Reader reader = new StringReader("content");
            connector.updateClob("1", reader);

            verify(preparedStatement).setCharacterStream(eq(1), eq(reader));
            verify(preparedStatement).setString(eq(2), eq("1"));
            verify(preparedStatement).executeUpdate();
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
