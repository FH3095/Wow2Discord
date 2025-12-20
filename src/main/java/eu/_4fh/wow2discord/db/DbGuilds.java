package eu._4fh.wow2discord.db;

import java.util.*;

public class DbGuilds {
    public record DbGuild(long guildId, long channelId, long wowGuildId, String wowRegion, String wowRealm,
                          String wowName) {
    }

    record WowRank2DcRole(short wowRank, long dcRole) {
    }

    private final Transaction t;

    DbGuilds(Transaction t) {
        this.t = t;
    }

    public List<DbGuild> getGuilds() {
        return t.query(DbGuild.class, "SELECT guild_id, channel_id, wow_guild_id, wow_region, wow_realm_slug, wow_name_slug FROM dc_settings ORDER BY wow_region, guild_id");
    }

    public Map<Byte, Set<Long>> getWowRanksToRoleIds(long guildId) {
        List<WowRank2DcRole> mappings =
                t.query(WowRank2DcRole.class, "SELECT wow_rank, role_id FROM wow_rank2dc_role WHERE guild_id = ?", guildId);
        Map<Byte, Set<Long>> result = new HashMap<>();
        for (WowRank2DcRole mapping : mappings) {
            result.computeIfAbsent((byte) mapping.wowRank(), HashSet::new).add(mapping.dcRole());
        }
        return Collections.unmodifiableMap(result);
    }
}
