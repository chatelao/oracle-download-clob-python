package com.oracle.tool;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Clob;
import java.sql.SQLException;

/**
 * Handles streaming of large text data between database and file system.
 */
public class CLOBProcessor {

  /**
   * Reads from database LOB and writes to disk.
   *
   * @param clob       Oracle CLOB object.
   * @param targetPath Path to the target file.
   * @throws SQLException If a database access error occurs.
   * @throws IOException  If an I/O error occurs.
   */
  public void streamToFile(Clob clob, Path targetPath) throws SQLException, IOException {
    try (Reader reader = clob.getCharacterStream();
        BufferedWriter writer = Files.newBufferedWriter(
            targetPath, StandardCharsets.UTF_8)) {
      reader.transferTo(writer);
    }
  }

  /**
   * Reads file content for upload. Note: Reads entire file into memory.
   *
   * @param sourcePath Path to the source file.
   * @return Content of the file as a String.
   * @throws IOException If an I/O error occurs.
   */
  public String readFromFile(Path sourcePath) throws IOException {
    return Files.readString(sourcePath, StandardCharsets.UTF_8);
  }

  /**
   * Opens a file for reading, providing a handle for streaming.
   *
   * @param sourcePath Path to the source file.
   * @return Reader for the file.
   * @throws IOException If an I/O error occurs.
   */
  public Reader openFile(Path sourcePath) throws IOException {
    return Files.newBufferedReader(sourcePath, StandardCharsets.UTF_8);
  }
}
