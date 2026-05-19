package com.oracle.tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FSManagerTest {

    private final FSManager fsManager = new FSManager();

    @Test
    void testEnsureDirectory(@TempDir Path tempDir) throws IOException {
        Path newDir = tempDir.resolve("test-dir");
        assertFalse(Files.exists(newDir));
        fsManager.ensureDirectory(newDir);
        assertTrue(Files.exists(newDir));
        assertTrue(Files.isDirectory(newDir));
    }

    @Test
    void testListFiles(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("file1.txt"));
        Files.createFile(tempDir.resolve("file2.txt"));
        Files.createFile(tempDir.resolve("other.csv"));

        List<Path> files = fsManager.listFiles(tempDir, "*.txt");
        assertEquals(2, files.size());
        assertTrue(files.stream().anyMatch(p -> p.getFileName().toString().equals("file1.txt")));
        assertTrue(files.stream().anyMatch(p -> p.getFileName().toString().equals("file2.txt")));
    }

    @Test
    void testListFilesNoDirectory() throws IOException {
        Path nonExistent = Path.of("non-existent-dir-12345");
        List<Path> files = fsManager.listFiles(nonExistent, "*");
        assertTrue(files.isEmpty());
    }
}
