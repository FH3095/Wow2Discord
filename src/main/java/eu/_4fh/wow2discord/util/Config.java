package eu._4fh.wow2discord.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Properties;

public class Config {
    public static final String discordToken;
    public static final String bnetApiKey;
    public static final String bnetApiSecret;
    public static final int guildCheckInterval;
    public static final int maxAnnouncedCharactersAtOnce;
    public static final int discordCheckInterval;

    static {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(Path.of("config.properties"), StandardOpenOption.READ)) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        discordToken = getStr(props, "discord_token");
        bnetApiKey = getStr(props, "bnet_api_key");
        bnetApiSecret = getStr(props, "bnet_api_secret");
        guildCheckInterval = Integer.decode(getStr(props, "guild_check_interval"));
        maxAnnouncedCharactersAtOnce = Integer.decode(getStr(props, "max_announced_characters_at_once"));
        discordCheckInterval = Integer.decode(getStr(props, "discord_check_interval"));
    }

    private Config() {
    }

    private static String getStr(Properties props, String key) {
        return Objects.requireNonNull(props.getProperty(key), "Missing config-value " + key);
    }
}
