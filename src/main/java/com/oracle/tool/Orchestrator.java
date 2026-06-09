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
   * @param csvPath   Path to the CSV file containing IDs (optional if idQuery is provided).
   * @param outputDir Directory where CLOBs will be saved.
   * @param dbConfig  Database configuration.
   * @param reporter  Progress reporter (optional).
   * @throws IOException  If an I/O error occurs.
   * @throws SQLException If a database access error occurs.
   */
  public void downloadMode(Path csvPath, Path outputDir, DBConfig dbConfig,
      ProgressReporter reporter)
      throws IOException, SQLException {
    dbConnector.connect(dbConfig);
    try {
      List<String> ids;
      if (dbConfig.idQuery() != null && !dbConfig.idQuery().isEmpty()) {
        logger.info("Fetching IDs using query: {}", dbConfig.idQuery());
        ids = dbConnector.fetchIds(dbConfig.idQuery());
      } else if (csvPath != null) {
        ids = inputManager.loadIds(csvPath);
      } else {
        throw new IllegalArgumentException("Either csvPath or idQuery must be provided.");
      }

      if (ids.isEmpty()) {
        logger.info("No IDs found.");
        return;
      }

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
   * @param csvPath     Path to the CSV file containing IDs (optional if idQuery is provided).
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
    dbConnector.connect(dbConfig);
    try {
      List<String> patternsOrIds;
      if (dbConfig.idQuery() != null && !dbConfig.idQuery().isEmpty()) {
        logger.info("Fetching IDs using query: {}", dbConfig.idQuery());
        patternsOrIds = dbConnector.fetchIds(dbConfig.idQuery());
      } else if (csvPath != null) {
        patternsOrIds = inputManager.loadIds(csvPath);
      } else {
        throw new IllegalArgumentException("Either csvPath or idQuery must be provided.");
      }

      if (patternsOrIds.isEmpty()) {
        logger.info("No IDs found.");
        return;
      }

      OracleConnector.LobMetadata metadata = dbConnector.getLobColumnMetadata();
      boolean isBinary = (metadata.type() == Types.BLOB);
      boolean isXml = metadata.typeName() != null && metadata.typeName().toUpperCase().contains("XMLTYPE");
      int uploadAttempted = 0;
      int uploadSuccess = 0;

      List<AutoCloseable> resources = new ArrayList<>();
      List<String> currentBatchIds = new ArrayList<>();
      List<Path> currentBatchPaths = new ArrayList<>();

      try {
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

                  if (isXml) {
                    CLOBProcessor.ValidationResult result = clobProcessor.validateXml(filePath);
                    if (!result.valid()) {
                      logger.error("Invalid XML content in file {}. Skipping upload for ID {}. Error: {}",
                          filePath.getFileName(), dbId, result.errorMessage());
                      continue;
                    }
                  }

                  addFileToBatch(dbId, filePath, isBinary, resources, currentBatchIds, currentBatchPaths);
                  uploadAttempted++;

                  if (uploadAttempted % batchSize == 0) {
                    uploadSuccess += executeBatchAndClose(resources, currentBatchIds, currentBatchPaths);
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
              if (isXml) {
                CLOBProcessor.ValidationResult result = clobProcessor.validateXml(filePath);
                if (!result.valid()) {
                  logger.error("Invalid XML content in file {}. Skipping upload for ID {}. Error: {}",
                      filePath.getFileName(), idVal, result.errorMessage());
                  continue;
                }
              }

              logger.info("Uploading file {} for ID {}", filePath.getFileName(), idVal);
              addFileToBatch(idVal, filePath, isBinary, resources, currentBatchIds, currentBatchPaths);
              uploadAttempted++;

              if (uploadAttempted % batchSize == 0) {
                uploadSuccess += executeBatchAndClose(resources, currentBatchIds, currentBatchPaths);
              }
            } else {
              logger.warn("File not found for ID {}: {}", idVal, filePath);
            }
          }
        }
        uploadSuccess += executeBatchAndClose(resources, currentBatchIds, currentBatchPaths);
        logger.info("Total files attempted: {}, Successfully updated: {}",
            uploadAttempted, uploadSuccess);
      } finally {
        for (AutoCloseable res : resources) {
          try {
            res.close();
          } catch (Exception e) {
            logger.error("Error closing resource: {}", e.getMessage());
          }
        }
      }
    } finally {
      dbConnector.close();
    }
  }

  private void addFileToBatch(String id, Path filePath, boolean isBinary,
      List<AutoCloseable> resources, List<String> currentBatchIds, List<Path> currentBatchPaths)
      throws IOException, SQLException {
    if (isBinary) {
      InputStream is = clobProcessor.openFileAsStream(filePath);
      resources.add(is);
      dbConnector.addUpdateToBatch(id, is);
    } else {
      Reader reader = clobProcessor.openFile(filePath);
      resources.add(reader);
      dbConnector.addUpdateToBatch(id, reader);
    }
    currentBatchIds.add(id);
    currentBatchPaths.add(filePath);
  }

  private int executeBatchAndClose(List<AutoCloseable> resources, List<String> currentBatchIds,
      List<Path> currentBatchPaths)
      throws SQLException {
    if (currentBatchIds.isEmpty()) {
      return 0;
    }

    int successCount = 0;
    try {
      int[] results = dbConnector.executeUpdateBatch();
      for (int i = 0; i < results.length; i++) {
        if (results[i] > 0 || results[i] == -2) { // -2 is SUCCESS_NO_INFO
          successCount++;
        } else if (results[i] == 0) {
          logger.warn("No rows updated for ID {}. Record may not exist.", currentBatchIds.get(i));
        }
      }
      dbConnector.commit();
    } catch (SQLException ex) {
      logger.error("Batch update failed. Data preview for failed batch:");
      for (Path path : currentBatchPaths) {
        try {
          List<String> lines = Files.readAllLines(path);
          logger.error("File: {} - First 3 lines:", path.getFileName());
          lines.stream().limit(3).forEach(line -> logger.error("  {}", line));
        } catch (IOException e) {
          logger.error("Could not read file {} for logging: {}", path, e.getMessage());
        }
      }
      throw ex;
    } finally {
      for (AutoCloseable res : resources) {
        try {
          res.close();
        } catch (Exception e) {
          logger.error("Error closing resource: {}", e.getMessage());
        }
      }
      resources.clear();
      currentBatchIds.clear();
      currentBatchPaths.clear();
    }

    return successCount;
  }
}
