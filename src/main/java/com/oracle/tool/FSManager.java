package com.oracle.tool;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages directory and file operations.
 */
public class FSManager {

  /**
   * Ensures the given directory exists, creating it if necessary.
   *
   * @param dirPath Path to the directory.
   * @throws IOException If an I/O error occurs.
   */
  public void ensureDirectory(Path dirPath) throws IOException {
    if (!Files.exists(dirPath)) {
      Files.createDirectories(dirPath);
    }
  }

  /**
   * Returns a list of paths matching a pattern within a directory.
   *
   * @param dirPath Directory to search.
   * @param pattern Glob pattern (e.g., "*.txt").
   * @return List of matching Paths.
   * @throws IOException If an I/O error occurs.
   */
  public List<Path> listFiles(Path dirPath, String pattern) throws IOException {
    if (!Files.exists(dirPath)) {
      return java.util.Collections.emptyList();
    }
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, pattern)) {
      List<Path> result = new ArrayList<>();
      for (Path path : stream) {
        result.add(path);
      }
      return result;
    }
  }
}
