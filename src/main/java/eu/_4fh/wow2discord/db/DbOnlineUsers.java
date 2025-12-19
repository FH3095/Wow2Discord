package eu._4fh.wow2discord.db;

import java.time.Instant;
import java.util.List;

public class DbOnlineUsers {
    private final Transaction t;

    DbOnlineUsers(Transaction t) {
        this.t = t;
    }

    public boolean exists(long guildId, long userId) {
        List<Transaction.ExistsRecord> result = t.query(Transaction.ExistsRecord.class,
                "SELECT 1 FROM dc_online_users WHERE guild_id = ? AND member_id = ?", guildId, userId);
        return !result.isEmpty();
    }

    public void insert(long guildId, long userId, String name) {
        long inserted = t.update(
                "INSERT INTO dc_online_users(guild_id, member_id, last_online, member_name) VALUES (?,?,?,?)", guildId,
                userId, Instant.now(), name);
        assert inserted == 1 : "Not correctly inserted " + inserted + " for " + guildId + " " + userId;
    }

    public void update(long guildId, long userId, String name) {
        long updated = t.update(
                "UPDATE dc_online_users SET last_online = ?, member_name = ? WHERE guild_id = ? AND member_id = ?",
                Instant.now(), name, guildId, userId);
        assert updated == 1 : "Not correctly updated " + updated + " for " + guildId + " " + userId;
    }
}
