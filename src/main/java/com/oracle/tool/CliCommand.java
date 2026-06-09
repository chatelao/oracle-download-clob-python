package com.oracle.tool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import org.ini4j.Ini;
import org.ini4j.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IDefaultValueProvider;
import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(name = "oracle-clob-tool",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "Oracle CLOB Download and Upload Tool.",
    subcommands = {
        CliCommand.Download.class,
        CliCommand.Upload.class
    })
public class CliCommand implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(CliCommand.class);

  @Option(names = {"--debug"}, description = "Enables debug logging.")
  private boolean debug;

  @Override
  public void run() {
    if (debug) {
      LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
      loggerContext.getLogger("com.oracle.tool").setLevel(Level.DEBUG);
      logger.debug("Debug logging enabled.");
    }
  }

  @Command(name = "download", description = "Download CLOBs to local files.",
      defaultValueProvider = ConfigFileDefaultProvider.class)
  static class Download implements Callable<Integer> {
    @CommandLine.ParentCommand
    private CliCommand parent;

    @Spec
    CommandSpec spec;

    @Option(names = "--config", description = "Path to INI or TOML config file.")
    private void setConfig(File config) {
      if (config != null && config.exists()) {
        ((ConfigFileDefaultProvider) spec.defaultValueProvider()).loadConfig(config);
      }
    }

    @Option(names = "--csv-path", required = false,
        description = "Path to the CSV file containing IDs.")
    Path csvPath;

    @Option(names = "--id-query", required = false,
        description = "SQL query to fetch IDs from the database.")
    String idQuery;

    @Option(names = "--output-dir", required = true,
        description = "Target directory for downloaded files.")
    Path outputDir;

    @Option(names = "--user", required = true, description = "Oracle DB username.")
    String user;

    @Option(names = "--password", required = true, description = "Oracle DB password.")
    String password;

    @Option(names = "--dsn", required = true, description = "Oracle DB DSN.")
    String dsn;

    @Option(names = "--table", required = false, description = "Target table name.")
    String table;

    @Option(names = "--query", required = false, description = "User written SELECT statement.")
    String query;

    @Option(names = "--id-column", required = true, description = "Column name for IDs.")
    String idColumn;

    @Option(names = {"--clob-column", "--lob-column"}, required = true,
        description = "Column name for CLOBs or BLOBs.")
    String clobColumn;

    @Option(names = "--filename-column", required = false,
        description = "Column name for filenames.")
    String filenameColumn;

    @Option(names = "--gtt-name", defaultValue = "GTT_IDS",
        description = "Name of the Global Temporary Table.")
    private String gttName;

    @Override
    public Integer call() throws Exception {
      if (parent != null && parent.debug) {
        configureDebugLogging();
      }

      if ((table == null || table.isEmpty()) && (query == null || query.isEmpty())) {
        spec.commandLine().getErr().println("Error: Either --table or --query must be provided.");
        spec.commandLine().usage(spec.commandLine().getErr());
        return 1;
      }
      if (csvPath == null && (idQuery == null || idQuery.isEmpty())) {
        spec.commandLine().getErr().println("Error: Either --csv-path or --id-query must be provided.");
        spec.commandLine().usage(spec.commandLine().getErr());
        return 1;
      }

      try {
        DBConfig dbConfig = new DBConfig(user, password, dsn, table,
            idColumn, clobColumn, gttName, query, filenameColumn, idQuery);
        Orchestrator orchestrator = createOrchestrator();

        logger.info("Starting download mode. CSV: {}, ID Query: {}, Output: {}", csvPath, idQuery, outputDir);
        orchestrator.downloadMode(csvPath, outputDir, dbConfig, new ConsoleProgressReporter());
        logger.info("Download completed successfully.");
        return 0;
      } catch (Exception ex) {
        logger.error("Download failed: {}", ex.getMessage());
        if (logger.isDebugEnabled()) {
          logger.debug("Stack trace:", ex);
        }
        return 1;
      }
    }
  }

  @Command(name = "upload", description = "Upload local files to Oracle CLOBs.",
      defaultValueProvider = ConfigFileDefaultProvider.class)
  static class Upload implements Callable<Integer> {
    @CommandLine.ParentCommand
    private CliCommand parent;

    @Spec
    CommandSpec spec;

    @Option(names = "--config", description = "Path to INI or TOML config file.")
    private void setConfig(File config) {
      if (config != null && config.exists()) {
        ((ConfigFileDefaultProvider) spec.defaultValueProvider()).loadConfig(config);
      }
    }

    @Option(names = "--csv-path", required = false,
        description = "Path to the CSV file containing IDs.")
    Path csvPath;

    @Option(names = "--id-query", required = false,
        description = "SQL query to fetch IDs from the database.")
    String idQuery;

    @Option(names = "--input-dir", required = true,
        description = "Source directory containing files to upload.")
    Path inputDir;

    @Option(names = "--user", required = true, description = "Oracle DB username.")
    String user;

    @Option(names = "--password", required = true, description = "Oracle DB password.")
    String password;

    @Option(names = "--dsn", required = true, description = "Oracle DB DSN.")
    String dsn;

    @Option(names = "--table", required = true, description = "Target table name.")
    String table;

    @Option(names = "--id-column", required = true, description = "Column name for IDs.")
    String idColumn;

    @Option(names = {"--clob-column", "--lob-column"}, required = true,
        description = "Column name for CLOBs or BLOBs.")
    String clobColumn;

    @Option(names = "--id-as-regex", description = "Treat IDs as regex patterns to match filenames.")
    private boolean idAsRegex;

    @Option(names = "--batch-size", defaultValue = "100",
        description = "Batch size for periodic commits.")
    private int batchSize;

    @Override
    public Integer call() throws Exception {
      if (parent != null && parent.debug) {
        configureDebugLogging();
      }
      if (csvPath == null && (idQuery == null || idQuery.isEmpty())) {
        spec.commandLine().getErr().println("Error: Either --csv-path or --id-query must be provided.");
        spec.commandLine().usage(spec.commandLine().getErr());
        return 1;
      }
      try {
        DBConfig dbConfig = new DBConfig(user, password, dsn, table, idColumn, clobColumn,
            "GTT_IDS", null, null, idQuery);
        Orchestrator orchestrator = createOrchestrator();

        logger.info("Starting upload mode. CSV: {}, ID Query: {}, Input: {}, ID as Regex: {}, Batch Size: {}",
            csvPath, idQuery, inputDir, idAsRegex, batchSize);
        orchestrator.uploadMode(csvPath, inputDir, dbConfig, idAsRegex, batchSize);
        logger.info("Upload completed successfully.");
        return 0;
      } catch (Exception ex) {
        logger.error("Upload failed: {}", ex.getMessage());
        if (logger.isDebugEnabled()) {
          logger.debug("Stack trace:", ex);
        }
        return 1;
      }
    }
  }

  private static void configureDebugLogging() {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    loggerContext.getLogger("com.oracle.tool").setLevel(Level.DEBUG);
    logger.debug("Debug logging enabled.");
  }

  private static Orchestrator createOrchestrator() {
    return new Orchestrator(
        new InputManager(),
        new OracleConnector(),
        new CLOBProcessor(),
        new FSManager()
    );
  }

  static class ConfigFileDefaultProvider implements IDefaultValueProvider {
    private Map<String, String> values = new HashMap<>();

    public void loadConfig(File configFile) {
      String fileName = configFile.getName().toLowerCase();
      try {
        if (fileName.endsWith(".toml")) {
          TomlMapper mapper = new TomlMapper();
          Map<String, Object> data = mapper.readValue(configFile, Map.class);
          data.forEach((k, v) -> {
            // Skip id list sources as per requirement "except the id list"
            if (!"csv-path".equals(k) && !"id-query".equals(k)) {
              values.put("--" + k, String.valueOf(v));
            }
          });
        } else if (fileName.endsWith(".ini")) {
          Ini ini = new Ini(configFile);
          Profile.Section section = ini.get("oracle-clob-tool");
          if (section == null) {
            section = ini.get("DEFAULT");
          }
          if (section != null) {
            section.forEach((k, v) -> {
              // Skip id list sources as per requirement "except the id list"
              if (!"csv-path".equals(k) && !"id-query".equals(k)) {
                values.put("--" + k, v);
              }
            });
          }
        }
      } catch (IOException e) {
        logger.error("Failed to load config file {}: {}", configFile, e.getMessage());
      }
    }

    @Override
    public String defaultValue(ArgSpec argSpec) throws Exception {
      if (argSpec.isOption()) {
        for (String name : ((CommandLine.Model.OptionSpec) argSpec).names()) {
          if (values.containsKey(name)) {
            return values.get(name);
          }
        }
      }
      return null;
    }
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new CliCommand()).execute(args);
    System.exit(exitCode);
  }
}
