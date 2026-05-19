package com.oracle.tool;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

import java.nio.file.Path;
import java.util.concurrent.Callable;

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

    @Command(name = "download", description = "Download CLOBs to local files.")
    static class Download implements Callable<Integer> {
        @CommandLine.ParentCommand
        private CliCommand parent;

        @Option(names = "--csv-path", required = true, description = "Path to the CSV file containing IDs.")
        private Path csvPath;

        @Option(names = "--output-dir", required = true, description = "Target directory for downloaded files.")
        private Path outputDir;

        @Option(names = "--user", required = true, description = "Oracle DB username.")
        private String user;

        @Option(names = "--password", required = true, description = "Oracle DB password.")
        private String password;

        @Option(names = "--dsn", required = true, description = "Oracle DB DSN.")
        private String dsn;

        @Option(names = "--table", required = true, description = "Target table name.")
        private String table;

        @Option(names = "--id-column", required = true, description = "Column name for IDs.")
        private String idColumn;

        @Option(names = "--clob-column", required = true, description = "Column name for CLOBs.")
        private String clobColumn;

        @Option(names = "--gtt-name", defaultValue = "GTT_IDS", description = "Name of the Global Temporary Table.")
        private String gttName;

        @Override
        public Integer call() throws Exception {
            if (parent.debug) {
                configureDebugLogging();
            }
            try {
                DBConfig dbConfig = new DBConfig(user, password, dsn, table, idColumn, clobColumn, gttName);
                Orchestrator orchestrator = createOrchestrator();

                logger.info("Starting download mode. CSV: {}, Output: {}", csvPath, outputDir);
                orchestrator.downloadMode(csvPath, outputDir, dbConfig);
                logger.info("Download completed successfully.");
                return 0;
            } catch (Exception e) {
                logger.error("Download failed: {}", e.getMessage());
                if (logger.isDebugEnabled()) {
                    logger.debug("Stack trace:", e);
                }
                return 1;
            }
        }
    }

    @Command(name = "upload", description = "Upload local files to Oracle CLOBs.")
    static class Upload implements Callable<Integer> {
        @CommandLine.ParentCommand
        private CliCommand parent;

        @Option(names = "--csv-path", required = true, description = "Path to the CSV file containing IDs.")
        private Path csvPath;

        @Option(names = "--input-dir", required = true, description = "Source directory containing files to upload.")
        private Path inputDir;

        @Option(names = "--user", required = true, description = "Oracle DB username.")
        private String user;

        @Option(names = "--password", required = true, description = "Oracle DB password.")
        private String password;

        @Option(names = "--dsn", required = true, description = "Oracle DB DSN.")
        private String dsn;

        @Option(names = "--table", required = true, description = "Target table name.")
        private String table;

        @Option(names = "--id-column", required = true, description = "Column name for IDs.")
        private String idColumn;

        @Option(names = "--clob-column", required = true, description = "Column name for CLOBs.")
        private String clobColumn;

        @Override
        public Integer call() throws Exception {
            if (parent.debug) {
                configureDebugLogging();
            }
            try {
                DBConfig dbConfig = new DBConfig(user, password, dsn, table, idColumn, clobColumn);
                Orchestrator orchestrator = createOrchestrator();

                logger.info("Starting upload mode. CSV: {}, Input: {}", csvPath, inputDir);
                orchestrator.uploadMode(csvPath, inputDir, dbConfig);
                logger.info("Upload completed successfully.");
                return 0;
            } catch (Exception e) {
                logger.error("Upload failed: {}", e.getMessage());
                if (logger.isDebugEnabled()) {
                    logger.debug("Stack trace:", e);
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

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CliCommand()).execute(args);
        System.exit(exitCode);
    }
}
