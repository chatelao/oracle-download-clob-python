package com.oracle.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CLOBProcessorTest {

    private CLOBProcessor processor;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        processor = new CLOBProcessor();
    }

    @Test
    void streamToFile_WritesClobContent() throws SQLException, IOException {
        Clob clob = mock(Clob.class);
        String content = "Hello Oracle";
        when(clob.getCharacterStream()).thenReturn(new StringReader(content));
        Path target = tempDir.resolve("output.txt");

        processor.streamToFile(clob, target);

        assertTrue(Files.exists(target));
        assertEquals(content, Files.readString(target));
    }

    @Test
    void streamToFile_WritesBlobContent() throws SQLException, IOException {
        Blob blob = mock(Blob.class);
        byte[] content = "Binary Data".getBytes();
        when(blob.getBinaryStream()).thenReturn(new ByteArrayInputStream(content));
        Path target = tempDir.resolve("output.bin");

        processor.streamToFile(blob, target);

        assertTrue(Files.exists(target));
        assertArrayEquals(content, Files.readAllBytes(target));
    }

    @Test
    void openFile_ReturnsReader() throws IOException {
        Path source = tempDir.resolve("input.txt");
        String content = "Hello World";
        Files.writeString(source, content);

        try (Reader reader = processor.openFile(source)) {
            char[] buffer = new char[content.length()];
            reader.read(buffer);
            assertEquals(content, new String(buffer));
        }
    }

    @Test
    void readFromFile_ReturnsContent() throws IOException {
        Path source = tempDir.resolve("input.txt");
        String content = "Hello content";
        Files.writeString(source, content);

        String actual = processor.readFromFile(source);
        assertEquals(content, actual);
    }
}
