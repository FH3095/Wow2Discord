package eu._4fh.wow2discord.discord;

import eu._4fh.wow2discord.db.DbGuilds.DbGuild;
import eu._4fh.wow2discord.db.Transaction;
import eu._4fh.wow2discord.util.Config;
import eu._4fh.wow2discord.util.CronTasks;
import eu._4fh.wow2discord.util.Log;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class DiscordCheck {

    private final Logger log = Log.getLog(this);

    public void start() {
        CronTasks.i().executeRegularly(this::runCheck, Duration.ofHours(Config.discordCheckInterval));
    }

    private void runCheck() {
        List<DbGuild> guilds;
        try (Transaction t = new Transaction()) {
            guilds = t.guilds.getGuilds();
        }
        for (DbGuild guild : guilds) {
            Guild dcGuild = Discord.i().getGuild(guild.guildId());
            if (dcGuild == null) {
                // Guild no longer available or wrong shard?
                continue;
            }
            log.info(() -> "Update discord guild " + guild.wowName() + "_" + guild.wowRealm() + " (" + guild.guildId() +
                    ")");
            Map<Byte, Set<Long>> wowRanksToRoles;
            try (Transaction t = new Transaction()) {
                wowRanksToRoles = t.guilds.getWowRanksToRoleIds(guild.guildId());
            }
            Set<Long> managedRoles = new HashSet<>(wowRanksToRoles.size());
            wowRanksToRoles.values().forEach(managedRoles::addAll);

            List<Member> members = dcGuild.loadMembers().get();

            updateDcMembers(guild.guildId(), members);
        }
    }

    /**
     * Updated nicknames for discord users in database
     */
    private void updateDcMembers(long guildId, List<Member> members) {
        try (Transaction t = new Transaction()) {
            for (Member member : members) {
                t.guildCharacters.update(guildId, member.getIdLong(), member.getEffectiveName());
            }
            t.commit();
        }
    }
}
