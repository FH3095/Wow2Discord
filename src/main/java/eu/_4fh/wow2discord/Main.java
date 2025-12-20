package eu._4fh.wow2discord;

import eu._4fh.wow2discord.db.Db;
import eu._4fh.wow2discord.discord.Discord;
import eu._4fh.wow2discord.discord.LastOnlineListener;
import eu._4fh.wow2discord.discord.WowCommandHandler;
import eu._4fh.wow2discord.util.CronTasks;
import eu._4fh.wow2discord.wow.BnetClients;
import eu._4fh.wow2discord.wow.GuildCheck;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.LogManager;

public class Main {
    private static void writePid() throws IOException {
        Files.writeString(Path.of("bot.pid"), String.valueOf(ProcessHandle.current().pid()), StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    private static void configureLogging() throws IOException {
        Path loggingPropertiesFile = Path.of("logging.properties");
        if (Files.isReadable(loggingPropertiesFile)) {
            try (InputStream in = Files.newInputStream(loggingPropertiesFile, StandardOpenOption.READ)) {
                LogManager.getLogManager().readConfiguration(in);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        writePid();
        configureLogging();

        try (var ignoredCronTasks = new CronTasks(); var ignoreClients = new BnetClients(); var ignoredDb = new Db(); var ignoredDiscord = new Discord(
                new LastOnlineListener(), new WowCommandHandler()); BufferedReader inReader = new BufferedReader(
                new InputStreamReader(System.in))) {
            new GuildCheck().start();
            String line;
            do {
                line = inReader.readLine().trim();
            } while (!line.equalsIgnoreCase("exit"));
        }
    }
}
