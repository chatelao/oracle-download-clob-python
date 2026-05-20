package com.oracle.tool;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High-level execution flow for Download and Upload modes.
 */
public class Orchestrator {
  private static final Logger logger = LoggerFactory.getLogger(Orchestrator.class);

  private final InputManager inputManager;
  private final OracleConnector dbConnector;
  private final CLOBProcessor clobProcessor;
  private final FSManager fsManager;

  public Orchestrator(InputManager inputManager,
      OracleConnector dbConnector,
      CLOBProcessor clobProcessor,
      FSManager fsManager) {
    this.inputManager = inputManager;
    this.dbConnector = dbConnector;
    this.clobProcessor = clobProcessor;
    this.fsManager = fsManager;
  }

  /**
   * Orchestrates UC-1: Download CLOBs.
   *
   * @param csvPath   Path to the CSV file containing IDs.
   * @param outputDir Directory where CLOBs will be saved.
   * @param dbConfig  Database configuration.
   * @throws IOException  If an I/O error occurs.
   * @throws SQLException If a database access error occurs.
   */
  public void downloadMode(Path csvPath, Path outputDir, DBConfig dbConfig)
      throws IOException, SQLException {
    List<String> ids = inputManager.loadIds(csvPath);
    if (ids.isEmpty()) {
      logger.info("No IDs found in CSV file.");
      return;
    }

    dbConnector.connect(dbConfig);
    try {
      fsManager.ensureDirectory(outputDir);

      Stream<ClobRecord> clobStream;
      if (ids.size() < 1000) {
        logger.info("Using IN clause strategy for {} IDs", ids.size());
        clobStream = dbConnector.fetchClobsIn(ids);
      } else {
        logger.info("Using GTT Join strategy for {} IDs", ids.size());
        dbConnector.createGtt(ids);
        clobStream = dbConnector.fetchClobsJoin();
      }

      try (clobStream) {
        Iterable<ClobRecord> iterable = clobStream::iterator;
        for (ClobRecord record : iterable) {
          Path targetPath = outputDir.resolve(record.id() + ".txt");
          clobProcessor.streamToFile(record.clob(), targetPath);
        }
      } catch (RuntimeException ex) {
        if (ex.getCause() instanceof SQLException sqlException) {
          throw sqlException;
        }
        throw ex;
      }
    } finally {
      dbConnector.close();
    }
  }

  /**
   * Orchestrates UC-2: Upload CLOBs.
   *
   * @param csvPath  Path to the CSV file containing IDs.
   * @param inputDir Directory containing files to upload.
   * @param dbConfig Database configuration.
   * @throws IOException  If an I/O error occurs.
   * @throws SQLException If a database access error occurs.
   */
  public void uploadMode(Path csvPath, Path inputDir, DBConfig dbConfig)
      throws IOException, SQLException {
    List<String> ids = inputManager.loadIds(csvPath);
    if (ids.isEmpty()) {
      logger.info("No IDs found in CSV file.");
      return;
    }

    dbConnector.connect(dbConfig);
    try {
      for (String idVal : ids) {
        Path filePath = inputDir.resolve(idVal + ".txt");
        if (Files.exists(filePath)) {
          try (Reader reader = clobProcessor.openFile(filePath)) {
            dbConnector.updateClob(idVal, reader);
          }
        } else {
          logger.warn("File not found for ID {}: {}", idVal, filePath);
        }
      }
      dbConnector.commit();
    } finally {
      dbConnector.close();
    }
  }
}
