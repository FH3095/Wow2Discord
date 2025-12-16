package eu._4fh.wow2discord;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Properties;

public class Config {
    public static final String discordToken;

    static {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(Path.of("config.properties"), StandardOpenOption.READ)) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        discordToken = getStr(props, "discord_token");
    }

    private Config() {
    }

    private static String getStr(Properties props, String key) {
        return Objects.requireNonNull(props.getProperty(key), "Missing config-value " + key);
    }
}
