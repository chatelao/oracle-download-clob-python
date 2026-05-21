package com.oracle.tool;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Parses CSV files and extracts a unique list of IDs.
 */
public class InputManager {

  /**
   * Reads the CSV and returns a list of unique strings from the first column.
   *
   * @param filePath Path to the CSV file.
   * @return Sorted list of unique IDs.
   * @throws IOException If an I/O error occurs or file not found.
   */
  public List<String> loadIds(Path filePath) throws IOException {
    if (!Files.exists(filePath)) {
      throw new IOException("Input file not found: " + filePath);
    }

    Set<String> ids = new HashSet<>();
    try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {

      boolean firstRecord = true;
      for (CSVRecord csvRecord : csvParser) {
        if (firstRecord) {
          firstRecord = false;
          continue;
        }
        if (csvRecord.size() > 0) {
          String val = csvRecord.get(0);
          if (val != null) {
            val = val.trim();
            if (!val.isEmpty()) {
              ids.add(val);
            }
          }
        }
      }
    }

    List<String> sortedIds = new ArrayList<>(ids);
    Collections.sort(sortedIds);
    return sortedIds;
  }

  /**
   * Ensures the CSV exists and contains at least one column.
   *
   * @param filePath Path to the CSV file.
   * @return True if valid, false otherwise.
   */
  public boolean validateFormat(Path filePath) {
    if (!Files.exists(filePath)) {
      return false;
    }

    try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT)) {
      return csvParser.iterator().hasNext();
    } catch (Exception ex) {
      return false;
    }
  }
}
