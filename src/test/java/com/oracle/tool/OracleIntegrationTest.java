package com.oracle.tool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
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
        List<String> ids = List.of("1", "2");
        connector.createGtt(ids);

        try (Stream<ClobRecord> results = connector.fetchClobsJoin()) {
            List<ClobRecord> list = results.toList();
            assertEquals(2, list.size());

            List<String> resultIds = list.stream().map(ClobRecord::id).sorted().toList();
            assertTrue(resultIds.contains("1"));
            assertTrue(resultIds.contains("2"));
        }
    }

    @Test
    void testUpdateClob() throws SQLException, Exception {
        String newContent = "Updated content from Java integration test";
        try (Reader reader = new StringReader(newContent)) {
            connector.updateClob("3", reader);
            connector.commit();
        }

        // Verify
        connector.createGtt(List.of("3"));
        try (Stream<ClobRecord> results = connector.fetchClobsJoin()) {
            ClobRecord record = results.findFirst().orElseThrow();
            assertEquals("3", record.id());

            try (Reader r = record.clob().getCharacterStream()) {
                StringBuilder sb = new StringBuilder();
                int charRead;
                while ((charRead = r.read()) != -1) {
                    sb.append((char) charRead);
                }
                assertEquals(newContent, sb.toString());
            }
        }
    }
}
