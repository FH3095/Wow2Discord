package eu._4fh.wow2discord.discord;

import eu._4fh.wow2discord.db.DbGuilds;
import eu._4fh.wow2discord.db.Transaction;
import eu._4fh.wow2discord.util.Config;
import eu._4fh.wow2discord.util.Log;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Updates the roles of discord users, that have been inactive for a long time
 */
class LastOnlineRolesUpdater {
    private final Logger log = Log.getLog(this);

    private final DbGuilds.DbGuild dbGuild;
    private final Guild dcGuild;
    private final List<Member> members;

    public LastOnlineRolesUpdater(DbGuilds.DbGuild dbGuild, Guild dcGuild, List<Member> members) {
        this.dbGuild = dbGuild;
        this.dcGuild = dcGuild;
        this.members = members;
    }

    public void updateRoles() {
        Set<Long> rolesToCheckForInactivity;
        try (Transaction t = new Transaction()) {
            rolesToCheckForInactivity = t.guilds.getInactivityCheckRoles(dcGuild.getIdLong());
        }
        if (rolesToCheckForInactivity.isEmpty()) {
            // No Roles to check
            return;
        }
        // Calculate when to do the first removal by using from which date onwards the last-online-state was saved
        // and after how many days of inactivity a user should be removed from the roles
        Instant firstRemoveAfter = dbGuild.lastOnlineSaveStarted()
                .plus(dbGuild.inactivityRemoveAfterDays(), ChronoUnit.DAYS);
        if (Instant.now().isBefore(firstRemoveAfter)) {
            log.info(
                    () -> "No removal for " + dbGuild.wowName() + "_" + dbGuild.wowRealm() + " because first removal date " + firstRemoveAfter + " is not reached");
            return;
        }
        Instant removeBefore = Instant.now()
                .truncatedTo(ChronoUnit.DAYS)
                .minus(Duration.ofDays(dbGuild.inactivityRemoveAfterDays()));
        Set<Long> recentlyOnlineUsers;
        try (Transaction t = new Transaction()) {
            recentlyOnlineUsers = t.onlineUsers.usersRecentlyOnline(dbGuild.guildId(), removeBefore);
        }
        for (Member member : members) {
            updateRolesForMember(member, rolesToCheckForInactivity, recentlyOnlineUsers);
        }
    }

    private void updateRolesForMember(Member member, Set<Long> rolesToCheckForInactivity, Set<Long> recentlyOnlineUsers) {
        if (OnlineStatus.OFFLINE != member.getOnlineStatus() && OnlineStatus.UNKNOWN != member.getOnlineStatus()) {
            // Member is currently online
            return;
        }
        if (recentlyOnlineUsers.contains(member.getIdLong())) {
            // Member was recently online
            return;
        }
        Set<Long> toRemoveRoles = member.getUnsortedRoles()
                .stream()
                .map(Role::getIdLong)
                .filter(rolesToCheckForInactivity::contains)
                .collect(Collectors.toUnmodifiableSet());
        if (toRemoveRoles.isEmpty()) {
            // No role from the member to remove
            return;
        }
        changeRolesForMember(member, toRemoveRoles);
    }

    private void changeRolesForMember(Member member, Set<Long> rolesToRemove) {
        Set<Role> roleObjectsToRemove = dcGuild.getRoleCache()
                .stream()
                .filter(role -> rolesToRemove.contains(role.getIdLong()))
                .collect(Collectors.toUnmodifiableSet());
        String roleNamesToRemove = roleObjectsToRemove.stream()
                .map(Role::getName)
                .collect(Collectors.joining(", ", "[", "]"));
        log.info(
                () -> "Guild " + dbGuild.wowName() + "_" + dbGuild.wowRealm() + " member " + member.getEffectiveName() + " is inactive. Remove " + roleNamesToRemove);
        if (Config.doChangeRoles) {
            dcGuild.modifyMemberRoles(member, Set.of(), roleObjectsToRemove).queue();
        }
    }
}
