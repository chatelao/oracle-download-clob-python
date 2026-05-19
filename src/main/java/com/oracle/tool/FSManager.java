package com.oracle.tool;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstracts disk I/O and directory management.
 */
public class FSManager {

    /**
     * Creates directory if it doesn't exist.
     *
     * @param path The directory path to ensure.
     * @throws IOException If an I/O error occurs.
     */
    public void ensureDirectory(Path path) throws IOException {
        Files.createDirectories(path);
    }

    /**
     * Matches local files with IDs using a glob pattern.
     *
     * @param directory The directory to search in.
     * @param pattern   The glob pattern (e.g., "*.txt").
     * @return A list of matching paths.
     * @throws IOException If an I/O error occurs.
     */
    public List<Path> listFiles(Path directory, String pattern) throws IOException {
        List<Path> files = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            return files;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, pattern)) {
            for (Path entry : stream) {
                files.add(entry);
            }
        }
        return files;
    }
}
