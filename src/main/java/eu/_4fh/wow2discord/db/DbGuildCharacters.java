package eu._4fh.wow2discord.db;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class to interact with the dc_acc2wow_char-Table. Used to query and update for characters that is in the guild im Battle.net.
 */
public class DbGuildCharacters {

    public record Wow2DcMapping(long wowCharId, String wowCharServer, String wowCharName, byte wowCharRank, Long dcId,
                                String dcName) {
    }

    public record WowCharacter(long guildId, long wowCharId, String wowCharServer, String wowCharName,
                               byte wowCharRank) {
    }

    public record WowCharId(long charId) {
    }

    public record Wow2Dc(long guildId, long charId, long dcId, String dcName) {
    }

    private final Transaction t;

    public DbGuildCharacters(Transaction t) {
        this.t = t;
    }

    public Set<Long> existingCharactersForGuild(long guildId) {
        List<WowCharId> charIds =
                t.query(WowCharId.class, "SELECT wow_char_id FROM dc_acc2wow_char WHERE guild_id = ?", guildId);
        return charIds.stream().map(WowCharId::charId).collect(Collectors.toUnmodifiableSet());
    }

    public List<Wow2DcMapping> getAllMappingsOrderedByRank(long guildId) {
        return t.query(Wow2DcMapping.class, "SELECT wow_char_id, wow_char_server, wow_char_name, wow_char_rank, dc_id, dc_member_name FROM dc_acc2wow_char WHERE guild_id = ? ORDER BY wow_char_rank ASC", guildId);
    }

    public List<Wow2DcMapping> getAllUnlinkedMappingsWithNameMatchOrderedByRankLimit25(long guildId, String namePattern) {
        return t.query(Wow2DcMapping.class, "SELECT wow_char_id, wow_char_server, wow_char_name, wow_char_rank, dc_id, dc_member_name FROM dc_acc2wow_char WHERE guild_id = ? AND wow_char_name LIKE ? AND dc_id IS NULL ORDER BY wow_char_rank ASC LIMIT 25", guildId, namePattern);
    }

    public void insert(WowCharacter wowCharacter) {
        t.update("INSERT INTO dc_acc2wow_char (guild_id, wow_char_id, wow_char_server, wow_char_name, wow_char_rank) VALUES (?, ?, ?, ?, ?)", wowCharacter.guildId(), wowCharacter.wowCharId(), wowCharacter.wowCharServer(), wowCharacter.wowCharName(), wowCharacter.wowCharRank());
    }

    public void update(WowCharacter wowCharacter) {
        t.update("UPDATE dc_acc2wow_char SET wow_char_server = ?, wow_char_name = ?, wow_char_rank = ? WHERE guild_id = ? AND wow_char_id = ?", wowCharacter.wowCharServer(), wowCharacter.wowCharName(), wowCharacter.wowCharRank(), wowCharacter.guildId(), wowCharacter.wowCharId());
    }

    public boolean update(Wow2Dc wow2Dc) {
        long updated =
                t.update("UPDATE dc_acc2wow_char SET dc_id = ?, dc_member_name = ? WHERE guild_id = ? AND wow_char_id = ?", wow2Dc.dcId, wow2Dc.dcName, wow2Dc.guildId, wow2Dc.charId);
        return updated > 0;
    }

    /**
     * Updates all entries for a specific discord user with its username
     */
    public void update(long guildId, long dcId, String dcName) {
        t.update("UPDATE dc_acc2wow_char SET dc_member_name = ? WHERE guild_id = ? AND dc_id = ?", dcName, guildId, dcId);
    }
}
