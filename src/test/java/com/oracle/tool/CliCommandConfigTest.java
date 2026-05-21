package com.oracle.tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CliCommandConfigTest {

    @TempDir
    Path tempDir;

    @Test
    public void testDownloadWithConfigToml() throws Exception {
        Path csvPath = tempDir.resolve("ids.csv");
        Files.writeString(csvPath, "ID\n1");
        Path outputDir = tempDir.resolve("output");

        Path configPath = tempDir.resolve("config.toml");
        Files.writeString(configPath, """
                user = "conf_user"
                password = "conf_password"
                dsn = "conf_dsn"
                table = "conf_table"
                id-column = "conf_id"
                clob-column = "conf_clob"
                """);

        CliCommand.Download download = new CliCommand.Download();
        CommandLine cmd = new CommandLine(download);
        cmd.execute(
                "--config", configPath.toString(),
                "--csv-path", csvPath.toString(),
                "--output-dir", outputDir.toString()
        );

        assertEquals("conf_user", download.user);
        assertEquals("conf_id", download.idColumn);
    }

    @Test
    public void testDownloadWithFilenameColumn() throws Exception {
        Path csvPath = tempDir.resolve("ids.csv");
        Files.writeString(csvPath, "ID\n1");
        Path outputDir = tempDir.resolve("output");

        CliCommand.Download download = new CliCommand.Download();
        CommandLine cmd = new CommandLine(download);
        cmd.execute(
                "--csv-path", csvPath.toString(),
                "--output-dir", outputDir.toString(),
                "--user", "u",
                "--password", "p",
                "--dsn", "d",
                "--table", "t",
                "--id-column", "i",
                "--clob-column", "c",
                "--filename-column", "f"
        );

        assertEquals("f", download.filenameColumn);
    }

    @Test
    public void testDownloadWithConfigIni() throws Exception {
        Path csvPath = tempDir.resolve("ids.csv");
        Files.writeString(csvPath, "ID\n1");
        Path outputDir = tempDir.resolve("output");

        Path configPath = tempDir.resolve("config.ini");
        Files.writeString(configPath, """
                [oracle-clob-tool]
                user = conf_user_ini
                password = conf_password_ini
                dsn = conf_dsn_ini
                table = conf_table_ini
                id-column = conf_id_ini
                clob-column = conf_clob_ini
                """);

        CliCommand.Download download = new CliCommand.Download();
        CommandLine cmd = new CommandLine(download);
        cmd.execute(
                "--config", configPath.toString(),
                "--csv-path", csvPath.toString(),
                "--output-dir", outputDir.toString()
        );

        assertEquals("conf_user_ini", download.user);
        assertEquals("conf_id_ini", download.idColumn);
    }

    @Test
    public void testConfigOverrideByCli() throws Exception {
        Path csvPath = tempDir.resolve("ids.csv");
        Files.writeString(csvPath, "ID\n1");
        Path outputDir = tempDir.resolve("output");

        Path configPath = tempDir.resolve("config.toml");
        Files.writeString(configPath, """
                user = "conf_user"
                password = "conf_password"
                dsn = "conf_dsn"
                table = "conf_table"
                id-column = "conf_id"
                clob-column = "conf_clob"
                """);

        CliCommand.Download download = new CliCommand.Download();
        CommandLine cmd = new CommandLine(download);
        cmd.execute(
                "--config", configPath.toString(),
                "--csv-path", csvPath.toString(),
                "--output-dir", outputDir.toString(),
                "--user", "cli_user"
        );

        assertEquals("cli_user", download.user); // CLI override
        assertEquals("conf_id", download.idColumn); // From config
    }
}
