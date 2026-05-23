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
        String[] urls = {"jdbc:oracle:thin:@127.0.0.1:1521/FREEPDB1", "jdbc:oracle:thin:@localhost:1521/FREEPDB1"};

        boolean connected = false;
        for (String url : urls) {
            System.out.println("Attempting to connect to existing database at " + url);
            // Retry loop for service registration
            for (int i = 0; i < 10; i++) {
                try (Connection conn = DriverManager.getConnection(url, "system", "password")) {
                    System.out.println("Successfully connected to existing database at " + url);
                    configStaticDb(url, "system", "password");
                    initializeSchema(conn);
                    connected = true;
                    break;
                } catch (SQLException e) {
                    System.out.println("Attempt " + (i + 1) + " failed: " + e.getMessage());
                    if (e.getErrorCode() == 12514 || e.getErrorCode() == 12516 || e.getErrorCode() == 12541) {
                         if (i < 9) {
                            Thread.sleep(10000); // Wait 10s before retry
                            continue;
                        }
                    }
                    // For other errors (like invalid credentials), don't retry this URL
                    break;
                }
            }
            if (connected) break;
        }

        if (!connected) {
            System.out.println("Starting Testcontainers fallback...");
            try {
                oracle = new OracleContainer(
                        DockerImageName.parse("gvenzl/oracle-free:latest"))
                        .withPassword("password");
                oracle.start();

                try (Connection conn = DriverManager.getConnection(oracle.getJdbcUrl(), oracle.getUsername(), oracle.getPassword())) {
                    configStaticDb(oracle.getJdbcUrl(), oracle.getUsername(), oracle.getPassword());
                    initializeSchema(conn);
                }
            } catch (Exception e) {
                System.err.println("Failed to start Testcontainers: " + e.getMessage());
                e.printStackTrace();
                Assumptions.abort("Docker is not available or failed to start: " + e.getMessage());
            }
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

    @Test
    void testBlobUpdate() throws Exception {
        DBConfig blobConfig = new DBConfig(
                staticUser,
                staticPassword,
                config.dsn(),
                "BLOB_DATA",
                "ID",
                "CONTENT",
                "GTT_IDS_BLOB_UPDATE"
        );
        connector.close();
        connector.connect(blobConfig);

        String targetId = "2";
        byte[] newContent = "Updated blob content from Java".getBytes(StandardCharsets.UTF_8);
        try (InputStream is = new java.io.ByteArrayInputStream(newContent)) {
            connector.updateLob(targetId, is);
            connector.commit();
        }

        connector.createGtt(List.of(targetId));
        try (Stream<LobRecord> results = connector.fetchClobsJoin()) {
            LobRecord record = results.findFirst().orElseThrow();
            try (InputStream is = ((Blob) record.lob()).getBinaryStream()) {
                byte[] actualBytes = is.readAllBytes();
                assertArrayEquals(newContent, actualBytes);
            }
        }
    }

    @Test
    void testXmlTypeUpdate() throws Exception {
        DBConfig xmlConfig = new DBConfig(
                staticUser,
                staticPassword,
                config.dsn(),
                "XMLTYPE_DATA",
                "ID",
                "CONTENT",
                "GTT_IDS_XML_UPDATE"
        );
        connector.close();
        connector.connect(xmlConfig);

        String targetId = "2";
        String newContent = "<root><item>Updated XML content from Java</item></root>";
        try (Reader reader = new StringReader(newContent)) {
            connector.updateLob(targetId, reader);
            connector.commit();
        }

        connector.createGtt(List.of(targetId));
        try (Stream<LobRecord> results = connector.fetchClobsJoin()) {
            LobRecord record = results.findFirst().orElseThrow();
            // XMLType might be returned as SQLXML or OPAQUE or other types depending on driver
            // Our improved OracleConnector handles SQLXML
            Object lob = record.lob();
            String actualContent;
            if (lob instanceof java.sql.SQLXML sqlxml) {
                actualContent = sqlxml.getString();
            } else if (lob instanceof java.sql.Clob clob) {
                actualContent = clob.getSubString(1, (int) clob.length());
            } else {
                actualContent = lob.toString();
            }
            // Oracle might return slightly different XML (e.g. with declaration)
            assertTrue(actualContent.contains("Updated XML content from Java"));
        }
    }
}
