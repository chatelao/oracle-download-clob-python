package com.oracle.tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InputManagerTest {

    private final InputManager inputManager = new InputManager();

    @Test
    void testLoadIds(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("ids.csv");
        Files.writeString(csvFile, "ID\n1\n2\n2\n3\n");

        List<String> ids = inputManager.loadIds(csvFile);
        assertEquals(3, ids.size());
        assertEquals(List.of("1", "2", "3"), ids);
    }

    @Test
    void testLoadIdsNoHeaderSkipsFirstRow(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("ids_no_header.csv");
        // "10" will be skipped as it's the first line
        Files.writeString(csvFile, "10\n20\n30\n");

        List<String> ids = inputManager.loadIds(csvFile);
        assertEquals(2, ids.size());
        assertEquals(List.of("20", "30"), ids);
    }

    @Test
    void testValidateFormat(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("valid.csv");
        Files.writeString(csvFile, "col1\nval1\n");
        assertTrue(inputManager.validateFormat(csvFile));

        Path emptyFile = tempDir.resolve("empty.csv");
        Files.createFile(emptyFile);
        assertFalse(inputManager.validateFormat(emptyFile));

        assertFalse(inputManager.validateFormat(tempDir.resolve("missing.csv")));
    }

    @Test
    void testLoadIdsFileNotFound() {
        assertThrows(IOException.class, () -> inputManager.loadIds(Path.of("non-existent.csv")));
    }
}
