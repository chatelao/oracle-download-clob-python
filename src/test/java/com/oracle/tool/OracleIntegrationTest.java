package com.oracle.tool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Blob;
import java.sql.Clob;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class OracleIntegrationTest {

    private static OracleContainer oracle;

    private OracleConnector connector;
    private DBConfig config;

    @BeforeAll
    static void initDb() throws Exception {
        // First try to use the existing database if it's running (e.g. in CI)
        String ciJdbcUrl = "jdbc:oracle:thin:@127.0.0.1:1521/FREEPDB1";
        try (Connection conn = DriverManager.getConnection(ciJdbcUrl, "system", "password")) {
            System.out.println("Using existing database at " + ciJdbcUrl);
            configStaticDb(ciJdbcUrl, "system", "password");
            initializeSchema(conn);
            return;
        } catch (SQLException e) {
            System.out.println("Existing database not found, starting Testcontainers: " + e.getMessage());
        }

        try {
            oracle = new OracleContainer(
                    DockerImageName.parse("container-registry.oracle.com/database/free:latest")
                            .asCompatibleSubstituteFor("gvenzl/oracle-xe"))
                    .withPassword("password");
            oracle.start();
        } catch (Exception e) {
            Assumptions.abort("Docker is not available or failed to start: " + e.getMessage());
        }

        try (Connection conn = DriverManager.getConnection(oracle.getJdbcUrl(), oracle.getUsername(), oracle.getPassword())) {
            configStaticDb(oracle.getJdbcUrl(), oracle.getUsername(), oracle.getPassword());
            initializeSchema(conn);
        }
    }

    private static String staticJdbcUrl;
    private static String staticUser;
    private static String staticPassword;

    private static void configStaticDb(String jdbcUrl, String user, String password) {
        staticJdbcUrl = jdbcUrl;
        staticUser = user;
        staticPassword = password;
    }

    private static void initializeSchema(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            Path sqlFile = Path.of("test/init_db.sql");
            if (!Files.exists(sqlFile)) {
                return;
            }
            String sql = Files.readString(sqlFile);

            // Simple SQL splitter that handles comments and basic commands
            String[] commands = sql.split(";");
            for (String cmd : commands) {
                String trimmedCmd = cmd.trim();
                // Remove single line comments
                trimmedCmd = trimmedCmd.replaceAll("--.*", "");
                if (trimmedCmd.isEmpty() || trimmedCmd.equalsIgnoreCase("EXIT")) {
                    continue;
                }
                try {
                    stmt.execute(trimmedCmd);
                } catch (SQLException e) {
                    // Ignore "table or view does not exist" on DROP
                    if (e.getErrorCode() == 942 && trimmedCmd.toUpperCase().contains("DROP")) {
                        continue;
                    }
                    // Ignore already exists
                    if (e.getErrorCode() == 955) {
                        continue;
                    }
                    throw e;
                }
            }
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        connector = new OracleConnector();
        // Extract DSN from jdbc:oracle:thin:@localhost:PORT/SERVICE
        String dsn = staticJdbcUrl.substring(staticJdbcUrl.indexOf("@") + 1);

        config = new DBConfig(
                staticUser,
                staticPassword,
                dsn,
                "CLOB_DATA",
                "ID",
                "CONTENT",
                "GTT_IDS_INTEGRATION"
        );
        connector.connect(config);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connector != null) {
            connector.close();
        }
    }

    @Test
    void testConnection() throws SQLException {
        assertNotNull(connector);
    }

    @Test
    void testCreateGttAndFetchJoin() throws SQLException {
        // Use IDs 1 and 2
        List<String> ids = List.of("1", "2");
        connector.createGtt(ids);

        try (Stream<LobRecord> results = connector.fetchClobsJoin()) {
            List<LobRecord> list = results.toList();
            assertEquals(2, list.size());

            List<String> resultIds = list.stream().map(LobRecord::id).sorted().toList();
            assertTrue(resultIds.contains("1"));
            assertTrue(resultIds.contains("2"));
        }
    }

    @Test
    void testUpdateClob() throws SQLException, Exception {
        // Use ID 3
        String newContent = "Updated content from Java integration test";
        String targetId = "3";
        try (Reader reader = new StringReader(newContent)) {
            connector.updateLob(targetId, reader);
            connector.commit();
        }

        // Verify
        connector.createGtt(List.of(targetId));
        try (Stream<LobRecord> results = connector.fetchClobsJoin()) {
            LobRecord record = results.findFirst().orElseThrow();
            assertEquals(targetId, record.id());

            try (Reader r = ((Clob) record.lob()).getCharacterStream()) {
                StringBuilder sb = new StringBuilder();
                int charRead;
                while ((charRead = r.read()) != -1) {
                    sb.append((char) charRead);
                }
                assertEquals(newContent, sb.toString());
            }
        }
    }

    @Test
    void testLargeClob() throws Exception {
        // Use ID 4
        String largeContent = "A".repeat(70 * 1024);
        String targetId = "4";
        try (Reader reader = new StringReader(largeContent)) {
            connector.updateLob(targetId, reader);
            connector.commit();
        }

        connector.createGtt(List.of(targetId));
        try (Stream<LobRecord> results = connector.fetchClobsJoin()) {
            LobRecord record = results.findFirst().orElseThrow();
            try (Reader r = ((Clob) record.lob()).getCharacterStream()) {
                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[8192];
                int charsRead;
                while ((charsRead = r.read(buffer)) != -1) {
                    sb.append(buffer, 0, charsRead);
                }
                assertEquals(largeContent, sb.toString());
            }
        }
    }

    @Test
    void testEmptyClob() throws Exception {
        // Use ID 5
        String targetId = "5";
        try (Reader reader = new StringReader("")) {
            connector.updateLob(targetId, reader);
            connector.commit();
        }

        connector.createGtt(List.of(targetId));
        try (Stream<LobRecord> results = connector.fetchClobsJoin()) {
            LobRecord record = results.findFirst().orElseThrow();
            if (record.lob() == null) {
                // Oracle might return null for empty CLOB depending on how it's handled
                return;
            }
            try (Reader r = ((Clob) record.lob()).getCharacterStream()) {
                assertEquals(-1, r.read());
            }
        }
    }

    @Test
    void testUnicodeClob() throws Exception {
        // Use ID 6
        String unicodeContent = "Hello 🌍, Special characters: ñ, á, é, í, ó, ú, ⚡";
        String targetId = "6";
        try (Reader reader = new StringReader(unicodeContent)) {
            connector.updateLob(targetId, reader);
            connector.commit();
        }

        connector.createGtt(List.of(targetId));
        try (Stream<LobRecord> results = connector.fetchClobsJoin()) {
            LobRecord record = results.findFirst().orElseThrow();
            try (Reader r = ((Clob) record.lob()).getCharacterStream()) {
                StringBuilder sb = new StringBuilder();
                int charRead;
                while ((charRead = r.read()) != -1) {
                    sb.append((char) charRead);
                }
                assertEquals(unicodeContent, sb.toString());
            }
        }
    }

    @Test
    void testNonExistentId() throws SQLException {
        connector.createGtt(List.of("non-existent-999"));
        try (Stream<LobRecord> results = connector.fetchClobsJoin()) {
            assertEquals(0, results.count());
        }
    }

    @Test
    void testMultipleIds() throws SQLException {
        // Use IDs 7, 8, 9
        List<String> ids = List.of("7", "8", "9");
        connector.createGtt(ids);
        try (Stream<LobRecord> results = connector.fetchClobsJoin()) {
            List<LobRecord> list = results.toList();
            assertEquals(3, list.size());
            List<String> resultIds = list.stream().map(LobRecord::id).toList();
            assertTrue(resultIds.containsAll(ids));
        }
    }

    @Test
    void testBlobDownload() throws SQLException, Exception {
        // Use BLOB_DATA table
        DBConfig blobConfig = new DBConfig(
                staticUser,
                staticPassword,
                config.dsn(),
                "BLOB_DATA",
                "ID",
                "CONTENT",
                "GTT_IDS_BLOB"
        );
        connector.close();
        connector.connect(blobConfig);

        connector.createGtt(List.of("1"));
        try (Stream<LobRecord> results = connector.fetchClobsJoin()) {
            LobRecord record = results.findFirst().orElseThrow();
            assertEquals("1", record.id());
            assertTrue(record.lob() instanceof Blob);
            try (InputStream is = ((Blob) record.lob()).getBinaryStream()) {
                byte[] bytes = is.readAllBytes();
                String content = new String(bytes, StandardCharsets.UTF_8);
                assertEquals("Initial blob content for ID 1", content);
            }
        }
    }
}
