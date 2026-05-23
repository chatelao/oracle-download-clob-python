package com.oracle.tool;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.SQLXML;

/**
 * Handles streaming of large text data between database and file system.
 */
public class CLOBProcessor {

  /**
   * Reads from database LOB and writes to disk.
   *
   * @param lob        Oracle CLOB or BLOB object.
   * @param targetPath Path to the target file.
   * @throws SQLException If a database access error occurs.
   * @throws IOException  If an I/O error occurs.
   */
  public void streamToFile(Object lob, Path targetPath) throws SQLException, IOException {
    if (lob instanceof Clob clob) {
      try (Reader reader = clob.getCharacterStream();
          BufferedWriter writer = Files.newBufferedWriter(
              targetPath, StandardCharsets.UTF_8)) {
        reader.transferTo(writer);
      }
    } else if (lob instanceof Blob blob) {
      try (InputStream is = blob.getBinaryStream();
          OutputStream os = Files.newOutputStream(targetPath)) {
        is.transferTo(os);
      }
    } else if (lob instanceof SQLXML sqlxml) {
      try (Reader reader = sqlxml.getCharacterStream();
          BufferedWriter writer = Files.newBufferedWriter(
              targetPath, StandardCharsets.UTF_8)) {
        reader.transferTo(writer);
      }
    } else if (lob != null) {
      throw new IllegalArgumentException("Unsupported LOB type: " + lob.getClass().getName());
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

  /**
   * Opens a file for reading as a binary stream.
   *
   * @param sourcePath Path to the source file.
   * @return InputStream for the file.
   * @throws IOException If an I/O error occurs.
   */
  public InputStream openFileAsStream(Path sourcePath) throws IOException {
    return Files.newInputStream(sourcePath);
  }
}
