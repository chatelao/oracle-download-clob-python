package com.oracle.tool;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    downloadMode(csvPath, outputDir, dbConfig, null);
  }

  /**
   * Orchestrates UC-1: Download CLOBs with progress reporting.
   *
   * @param csvPath   Path to the CSV file containing IDs.
   * @param outputDir Directory where CLOBs will be saved.
   * @param dbConfig  Database configuration.
   * @param reporter  Progress reporter (optional).
   * @throws IOException  If an I/O error occurs.
   * @throws SQLException If a database access error occurs.
   */
  public void downloadMode(Path csvPath, Path outputDir, DBConfig dbConfig,
      ProgressReporter reporter)
      throws IOException, SQLException {
    List<String> ids = inputManager.loadIds(csvPath);
    if (ids.isEmpty()) {
      logger.info("No IDs found in CSV file.");
      return;
    }

    dbConnector.connect(dbConfig);
    try {
      fsManager.ensureDirectory(outputDir);

      Stream<LobRecord> clobStream;
      if (ids.size() < 1000) {
        logger.info("Using IN clause strategy for {} IDs", ids.size());
        if (reporter != null) {
          reporter.setTotal(ids.size());
        }
        clobStream = dbConnector.fetchClobsIn(ids);
      } else {
        logger.info("Using GTT Join strategy for {} IDs", ids.size());
        if (reporter != null) {
          reporter.setTotal(ids.size());
        }
        dbConnector.createGtt(ids);
        clobStream = dbConnector.fetchClobsJoin();
      }

      try (clobStream) {
        Iterable<LobRecord> iterable = clobStream::iterator;
        for (LobRecord record : iterable) {
          String fileName = record.filename();
          if (fileName == null || fileName.isEmpty()) {
            fileName = record.id() + ".txt";
          }
          Path targetPath = outputDir.resolve(fileName);
          clobProcessor.streamToFile(record.lob(), targetPath);
          if (reporter != null) {
            reporter.update(1);
          }
        }
      } catch (RuntimeException ex) {
        if (ex.getCause() instanceof SQLException sqlException) {
          throw sqlException;
        }
        throw ex;
      }
    } finally {
      if (reporter != null) {
        reporter.finish();
      }
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
    uploadMode(csvPath, inputDir, dbConfig, false, 100);
  }

  /**
   * Orchestrates UC-2: Upload CLOBs with optional regex matching and batch size.
   *
   * @param csvPath     Path to the CSV file containing IDs.
   * @param inputDir    Directory containing files to upload.
   * @param dbConfig    Database configuration.
   * @param idAsRegex   Whether to treat IDs as regex patterns.
   * @param batchSize   Batch size for periodic commits.
   * @throws IOException  If an I/O error occurs.
   * @throws SQLException If a database access error occurs.
   */
  public void uploadMode(Path csvPath, Path inputDir, DBConfig dbConfig, boolean idAsRegex,
      int batchSize)
      throws IOException, SQLException {
    List<String> patternsOrIds = inputManager.loadIds(csvPath);
    if (patternsOrIds.isEmpty()) {
      logger.info("No IDs or patterns found in CSV file.");
      return;
    }

    dbConnector.connect(dbConfig);
    List<String> batchIds = new ArrayList<>();
    List<Object> batchContents = new ArrayList<>();
    try {
      int columnType = dbConnector.getLobColumnType();
      boolean isBinary = (columnType == Types.BLOB);
      int uploadAttempted = 0;
      int uploadSuccess = 0;

      if (idAsRegex) {
        List<Pattern> compiledPatterns = new ArrayList<>();
        for (String p : patternsOrIds) {
          try {
            compiledPatterns.add(Pattern.compile(p));
          } catch (Exception e) {
            logger.error("Invalid regex pattern '{}': {}", p, e.getMessage());
          }
        }

        try (Stream<Path> paths = Files.list(inputDir)) {
          Iterable<Path> filePaths = paths.filter(Files::isRegularFile)::iterator;
          for (Path filePath : filePaths) {
            String filename = filePath.getFileName().toString();
            for (Pattern pattern : compiledPatterns) {
              Matcher matcher = pattern.matcher(filename);
              if (matcher.find()) {
                String dbId = matcher.groupCount() > 0 ? matcher.group(1) : matcher.group(0);
                logger.info("Matched file {} with pattern {} -> ID: {}",
                    filename, pattern.pattern(), dbId);

                batchIds.add(dbId);
                batchContents.add(isBinary ? clobProcessor.openFileAsStream(filePath)
                    : clobProcessor.openFile(filePath));
                uploadAttempted++;

                if (batchIds.size() >= batchSize) {
                  uploadSuccess += processUploadBatch(batchIds, batchContents);
                }
                break;
              }
            }
          }
        }
      } else {
        for (String idVal : patternsOrIds) {
          Path filePath = inputDir.resolve(idVal + ".txt");
          if (!Files.exists(filePath)) {
            filePath = inputDir.resolve(idVal);
          }
          if (!Files.exists(filePath)) {
            try (Stream<Path> matches = Files.list(inputDir)) {
              filePath = matches
                  .filter(p -> p.getFileName().toString().startsWith(idVal + "."))
                  .findFirst()
                  .orElse(filePath);
            }
          }

          if (Files.exists(filePath)) {
            logger.info("Uploading file {} for ID {}", filePath.getFileName(), idVal);
            batchIds.add(idVal);
            batchContents.add(isBinary ? clobProcessor.openFileAsStream(filePath)
                : clobProcessor.openFile(filePath));
            uploadAttempted++;

            if (batchIds.size() >= batchSize) {
              uploadSuccess += processUploadBatch(batchIds, batchContents);
            }
          } else {
            logger.warn("File not found for ID {}: {}", idVal, filePath);
          }
        }
      }

      if (!batchIds.isEmpty()) {
        uploadSuccess += processUploadBatch(batchIds, batchContents);
      }

      dbConnector.commit();
      logger.info("Total files attempted: {}, Successfully updated: {}",
          uploadAttempted, uploadSuccess);
    } finally {
      closeBatchResources(batchContents);
      dbConnector.close();
    }
  }

  private int processUploadBatch(List<String> ids, List<Object> contents) throws SQLException {
    int success = 0;
    try {
      int[] results = dbConnector.updateLobsBatch(ids, contents);
      for (int i = 0; i < results.length; i++) {
        if (results[i] != java.sql.Statement.EXECUTE_FAILED && results[i] != 0) {
          success++;
        } else {
          logger.warn("No rows updated for ID {}. Record may not exist.", ids.get(i));
        }
      }
      dbConnector.commit();
    } finally {
      closeBatchResources(contents);
      ids.clear();
      contents.clear();
    }
    return success;
  }

  private void closeBatchResources(List<Object> contents) {
    for (Object content : contents) {
      if (content instanceof AutoCloseable ac) {
        try {
          ac.close();
        } catch (Exception e) {
          logger.error("Failed to close resource: {}", e.getMessage());
        }
      }
    }
  }
}
