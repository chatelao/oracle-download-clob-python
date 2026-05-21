package com.oracle.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilenameColumnTest {

    @Mock
    private InputManager inputManager;
    @Mock
    private OracleConnector dbConnector;
    @Mock
    private CLOBProcessor clobProcessor;
    @Mock
    private FSManager fsManager;
    @Mock
    private DBConfig dbConfig;

    private Orchestrator orchestrator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        orchestrator = new Orchestrator(inputManager, dbConnector, clobProcessor, fsManager);
    }

    @Test
    void downloadMode_WithFilenameColumn_UsesFilename() throws IOException, SQLException {
        List<String> ids = List.of("1", "2");
        when(inputManager.loadIds(any())).thenReturn(ids);

        Object lob1 = new Object();
        Object lob2 = new Object();

        Stream<LobRecord> clobStream = Stream.of(
            new LobRecord("1", lob1, "file1.txt"),
            new LobRecord("2", lob2, "file2.pdf")
        );
        when(dbConnector.fetchClobsIn(ids)).thenReturn(clobStream);

        orchestrator.downloadMode(Path.of("test.csv"), tempDir, dbConfig);

        verify(clobProcessor).streamToFile(lob1, tempDir.resolve("file1.txt"));
        verify(clobProcessor).streamToFile(lob2, tempDir.resolve("file2.pdf"));
    }

    @Test
    void downloadMode_WithoutFilename_FallsBackToId() throws IOException, SQLException {
        List<String> ids = List.of("1");
        when(inputManager.loadIds(any())).thenReturn(ids);

        Object lob1 = new Object();

        Stream<LobRecord> clobStream = Stream.of(
            new LobRecord("1", lob1, null)
        );
        when(dbConnector.fetchClobsIn(ids)).thenReturn(clobStream);

        orchestrator.downloadMode(Path.of("test.csv"), tempDir, dbConfig);

        verify(clobProcessor).streamToFile(lob1, tempDir.resolve("1.txt"));
    }
}
