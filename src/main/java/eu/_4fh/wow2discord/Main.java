package eu._4fh.wow2discord;

import eu._4fh.wow2discord.db.Db;
import eu._4fh.wow2discord.discord.Discord;
import eu._4fh.wow2discord.discord.LastOnlineListener;
import eu._4fh.wow2discord.discord.WowCommandHandler;
import eu._4fh.wow2discord.util.CronTasks;
import eu._4fh.wow2discord.wow.BnetClients;
import eu._4fh.wow2discord.wow.GuildCheck;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.LogManager;

public class Main {

    private static final Path pidFilePath = Path.of("bot.pid");

    private static void writePid(String ownPid) throws IOException {
        Files.writeString(pidFilePath, ownPid, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE);
    }

    private static boolean checkPidFile(String ownPid) throws IOException {
        if (!Files.isReadable(pidFilePath)) {
            return false;
        }
        String pidContent = Files.readString(pidFilePath).trim();
        return ownPid.equals(pidContent);
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
        String ownPid = String.valueOf(ProcessHandle.current().pid()).trim();
        writePid(ownPid);
        configureLogging();

        try (var ignoredCronTasks = new CronTasks(); var ignoreClients = new BnetClients(); var ignoredDb = new Db(); var ignoredDiscord = new Discord(
                new LastOnlineListener(), new WowCommandHandler())) {

            new GuildCheck().start();

            while (checkPidFile(ownPid)) {
                try {
                    Thread.sleep(15_000);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
    }
}
