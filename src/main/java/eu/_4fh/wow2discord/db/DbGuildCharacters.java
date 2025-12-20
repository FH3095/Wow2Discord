package eu._4fh.wow2discord.db;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DbGuildCharacters {

    public record WowCharacter(long guildId, long wowCharId, String wowCharServer, String wowCharName) {
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
        List<WowCharId> charIds = t.query(WowCharId.class, "SELECT wow_char_id FROM dc_acc2wow_char WHERE guild_id = ?",
                guildId);
        return charIds.stream().map(WowCharId::charId).collect(Collectors.toUnmodifiableSet());
    }

    public void insert(WowCharacter wowCharacter) {
        t.update(
                "INSERT INTO dc_acc2wow_char (guild_id, wow_char_id, wow_char_server, wow_char_name) VALUES (?, ?, ?, ?)",
                wowCharacter.guildId(), wowCharacter.wowCharId(), wowCharacter.wowCharServer(),
                wowCharacter.wowCharName());
    }

    public boolean update(Wow2Dc wow2Dc) {
        long updated = t.update(
                "UPDATE dc_acc2wow_char SET dc_id = ?, dc_member_name = ? WHERE guild_id = ? AND wow_char_id = ?",
                wow2Dc.dcId, wow2Dc.dcName, wow2Dc.guildId, wow2Dc.charId);
        return updated > 0;
    }
}
