package com.oracle.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrchestratorTest {

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
    void downloadMode_WithNoIds_DoesNothing() throws IOException, SQLException {
        when(inputManager.loadIds(any())).thenReturn(Collections.emptyList());

        orchestrator.downloadMode(Path.of("test.csv"), Path.of("output"), dbConfig);

        verify(dbConnector, never()).connect(any());
        verify(inputManager).loadIds(any());
    }

    @Test
    void downloadMode_SmallDataset_UsesInClause() throws IOException, SQLException {
        List<String> ids = List.of("1", "2");
        when(inputManager.loadIds(any())).thenReturn(ids);
        Clob clob1 = mock(Clob.class);
        Stream<LobRecord> clobStream = Stream.of(new LobRecord("1", clob1));
        when(dbConnector.fetchClobsIn(ids)).thenReturn(clobStream);

        ProgressReporter reporter = mock(ProgressReporter.class);
        orchestrator.downloadMode(Path.of("test.csv"), Path.of("output"), dbConfig, reporter);

        verify(reporter).setTotal(2);
        verify(reporter).update(1);
        verify(dbConnector).connect(dbConfig);
        verify(dbConnector, never()).createGtt(any());
        verify(dbConnector).fetchClobsIn(ids);
        verify(dbConnector, never()).fetchClobsJoin();
        verify(clobProcessor).streamToFile(eq(clob1), any());
        verify(dbConnector).close();
    }

    @Test
    void downloadMode_LargeDataset_UsesGttJoin() throws IOException, SQLException {
        List<String> ids = Collections.nCopies(1001, "id");
        when(inputManager.loadIds(any())).thenReturn(ids);
        Clob clob1 = mock(Clob.class);
        Stream<LobRecord> clobStream = Stream.of(new LobRecord("id", clob1));
        when(dbConnector.fetchClobsJoin()).thenReturn(clobStream);

        ProgressReporter reporter = mock(ProgressReporter.class);
        orchestrator.downloadMode(Path.of("test.csv"), Path.of("output"), dbConfig, reporter);

        verify(reporter).setTotal(1001);
        verify(reporter).update(1);
        verify(dbConnector).connect(dbConfig);
        verify(dbConnector).createGtt(ids);
        verify(dbConnector).fetchClobsJoin();
        verify(dbConnector, never()).fetchClobsIn(any());
        verify(clobProcessor).streamToFile(eq(clob1), any());
        verify(dbConnector).close();
    }

    @Test
    void uploadMode_WithNoIds_DoesNothing() throws IOException, SQLException {
        when(inputManager.loadIds(any())).thenReturn(Collections.emptyList());

        orchestrator.uploadMode(Path.of("test.csv"), Path.of("input"), dbConfig);

        verify(dbConnector, never()).connect(any());
    }

    @Test
    void uploadMode_WithIds_ExecutesFlow() throws IOException, SQLException {
        List<String> ids = List.of("1");
        when(inputManager.loadIds(any())).thenReturn(ids);
        Reader reader = mock(Reader.class);
        when(clobProcessor.openFile(any())).thenReturn(reader);
        when(dbConnector.getLobColumnType()).thenReturn(Types.CLOB);
        when(dbConnector.updateLobsBatch(any(), any())).thenReturn(new int[]{1});

        Files.createFile(tempDir.resolve("1.txt"));

        orchestrator.uploadMode(Path.of("test.csv"), tempDir, dbConfig);

        verify(dbConnector).connect(dbConfig);
        verify(dbConnector).updateLobsBatch(any(), any());
        verify(dbConnector, atLeastOnce()).commit();
        verify(dbConnector).close();
    }
}
