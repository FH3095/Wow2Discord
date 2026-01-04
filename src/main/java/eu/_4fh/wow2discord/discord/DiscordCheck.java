package eu._4fh.wow2discord.discord;

import eu._4fh.wow2discord.db.DbGuilds.DbGuild;
import eu._4fh.wow2discord.db.Transaction;
import eu._4fh.wow2discord.util.Config;
import eu._4fh.wow2discord.util.CronTasks;
import eu._4fh.wow2discord.util.Log;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
        for (DbGuild dbGuild : guilds) {
            Guild dcGuild = Discord.i().getGuild(dbGuild.guildId());
            if (dcGuild == null) {
                // Guild no longer available or wrong shard?
                continue;
            }
            log.info(
                    () -> "Update discord guild " + dbGuild.wowName() + "_" + dbGuild.wowRealm() + " (" + dbGuild.guildId() + ")");
            List<Member> members = dcGuild.loadMembers().get();

            updateDcMemberNames(dbGuild.guildId(), members);
            removeNonDcMembersFromCharacters(dbGuild.guildId(), members);
            new DiscordRoleUpdater(dbGuild, dcGuild, members).updateRoles();
            new LastOnlineRolesUpdater(dbGuild, dcGuild, members).updateRoles();
        }
    }

    /**
     * Updated nicknames for discord users in database
     */
    private void updateDcMemberNames(long guildId, List<Member> members) {
        try (Transaction t = new Transaction()) {
            for (Member member : members) {
                t.guildCharacters.update(guildId, member.getIdLong(), member.getEffectiveName());
            }
            t.commit();
        }
    }

    /**
     * Remove discord links from characters, when the user is no longer a member of the discord.
     */
    private void removeNonDcMembersFromCharacters(long guildId, List<Member> members) {
        Set<Long> memberIds = members.stream().map(Member::getIdLong).collect(Collectors.toUnmodifiableSet());
        try (Transaction t = new Transaction()) {
            long changedCharacters = t.guildCharacters.removeDiscordAccountsOtherThan(guildId, memberIds);
            log.info(
                    () -> "Removed discord accounts from " + changedCharacters + " characters, because the users are no longer on the discord server");
            t.commit();
        }
    }
}
