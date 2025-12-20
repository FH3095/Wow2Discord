package eu._4fh.wow2discord.db;

import java.util.List;

public class DbGuilds {
    public record DbGuild(long guildId, long channelId, long wowGuildId, String wowRegion, String wowRealm,
                          String wowName) {
    }

    private final Transaction t;

    DbGuilds(Transaction t) {
        this.t = t;
    }

    public List<DbGuild> getGuilds() {
        return t.query(DbGuild.class,
                "SELECT guild_id, channel_id, wow_guild_id, wow_region, wow_realm_slug, wow_name_slug FROM dc_settings ORDER BY wow_region, guild_id");
    }
}
