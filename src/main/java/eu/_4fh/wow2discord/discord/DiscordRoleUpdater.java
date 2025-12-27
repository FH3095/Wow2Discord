package eu._4fh.wow2discord.discord;

import eu._4fh.wow2discord.db.DbGuildCharacters.Wow2DcMapping;
import eu._4fh.wow2discord.db.DbGuilds.DbGuild;
import eu._4fh.wow2discord.db.Transaction;
import eu._4fh.wow2discord.util.Config;
import eu._4fh.wow2discord.util.Log;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogChange;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.audit.AuditLogKey;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

/**
 * Updates the discord roles according to the wow ranks
 */
class DiscordRoleUpdater {
    private final Logger log = Log.getLog(this);

    private final DbGuild dbGuild;
    private final Guild dcGuild;
    private final Map<Long, Member> members;
    /// Mapping von WOW Ranks to Discord Roles
    private final Map<Byte, Set<Long>> wowRanksToRoles;
    /// List of Users with the roles they were added to within the last 7 days
    private final Map<Long, Set<Long>> addedRoles;
    /// Roles we manage at all.
    private final Set<Long> managedRoles;

    public DiscordRoleUpdater(DbGuild dbGuild, Guild dcGuild, List<Member> members) {
        this.dcGuild = dcGuild;
        this.dbGuild = dbGuild;
        this.members = members.stream().collect(Collectors.toMap(Member::getIdLong, identity()));

        wowRanksToRoles = getWowRanksToRoles(dbGuild.guildId());
        managedRoles = new HashSet<>(wowRanksToRoles.size());
        wowRanksToRoles.values().forEach(managedRoles::addAll);
        addedRoles = getAddedRolesPerUserWithin7Days(dcGuild);
    }

    private Map<Byte, Set<Long>> getWowRanksToRoles(long guildId) {
        try (Transaction t = new Transaction()) {
            return t.guilds.getWowRanksToRoleIds(guildId);
        }
    }

    /***
     * Get the Audit Logs to get which user was added to which role within the last 7 days
     */
    private Map<Long, Set<Long>> getAddedRolesPerUserWithin7Days(Guild dcGuild) {
        Map<Long, Set<Long>> result = new HashMap<>();
        Instant oneWeekAgo = Instant.now().minus(Duration.ofDays(7)).truncatedTo(ChronoUnit.DAYS);
        List<AuditLogEntry> logEntries = dcGuild.retrieveAuditLogs()
                .type(ActionType.MEMBER_ROLE_UPDATE)
                .cache(false)
                .complete();
        for (AuditLogEntry entry : logEntries) {
            if (entry.getTimeCreated().toInstant().isBefore(oneWeekAgo)) {
                continue;
            }
            AuditLogChange change = entry.getChangeByKey(AuditLogKey.MEMBER_ROLES_ADD);
            if (change != null) {
                Set<Long> userRoles = result.computeIfAbsent(entry.getTargetIdLong(), key -> new HashSet<>());
                // The Map contains two entries: One with key=name, value=RoleName, one with key=id, value=RoleId (as String)
                List<Map<String, String>> roles = Objects.requireNonNull(change.getNewValue());
                roles.forEach(map -> userRoles.add(Long.parseUnsignedLong(map.get("id"))));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Go through each wow character, return a Map of Discord-User-IDs with the Roles they SHOULD have according
     * to their wow Rank.
     */
    private Map<Long, Set<Long>> calcRolesPerUserByWowRank() {
        List<Wow2DcMapping> characters;
        try (Transaction t = new Transaction()) {
            characters = t.guildCharacters.getAllMappingsOrderedByRank(dbGuild.guildId());
        }
        Map<Long, Set<Long>> result = new HashMap<>(characters.size());
        for (Wow2DcMapping character : characters) {
            Set<Long> rankRoles = wowRanksToRoles.getOrDefault(character.wowCharRank(), Set.of());
            if (rankRoles.isEmpty() || character.dcId() == null) {
                continue;
            }
            result.computeIfAbsent(character.dcId(), dcId -> new HashSet<>()).addAll(rankRoles);
        }
        return result;
    }

    /**
     * Go through each DC User and fetch the Roles they have that we manage.
     */
    private Map<Long, Set<Long>> getDcRolesPerUser() {
        Map<Long, Set<Long>> result = new HashMap<>(members.size());
        for (Member member : members.values()) {
            Set<Long> managedRoleMemberships = member.getUnsortedRoles()
                    .stream()
                    .map(Role::getIdLong)
                    .filter(managedRoles::contains)
                    .collect(Collectors.toUnmodifiableSet());
            result.put(member.getIdLong(), managedRoleMemberships);
        }
        return result;
    }

    /**
     * Actually update the discord Roles
     */
    public void updateRoles() {
        Map<Long, Set<Long>> wowRolesPerUser = calcRolesPerUserByWowRank();
        Map<Long, Set<Long>> dcRolesPerUser = getDcRolesPerUser();

        for (long userId : dcRolesPerUser.keySet()) {
            Set<Long> dcRoles = dcRolesPerUser.get(userId);
            // Probably a user left the guild, then this user has no wow role at all
            Set<Long> wowRoles = wowRolesPerUser.getOrDefault(userId, Set.of());

            Set<Long> newRoles = new HashSet<>(wowRoles);
            newRoles.removeAll(dcRoles);
            Set<Long> removeRoles = new HashSet<>(dcRoles);
            removeRoles.removeAll(wowRoles);
            // We do not want to change roles, to which the user was added recently
            removeRoles.removeAll(addedRoles.getOrDefault(userId, Set.of()));

            updateUserRoles(userId, newRoles, removeRoles);
        }

        // newUsers are user which are not yet in dcRolesPerUser
        // Since the loop above iterates over the dc Roles it wouldn't find them.
        // So we add them here
        Map<Long, Set<Long>> newUsers = new HashMap<>(wowRolesPerUser);
        newUsers.keySet().removeAll(dcRolesPerUser.keySet());
        for (Map.Entry<Long, Set<Long>> newUser : newUsers.entrySet()) {
            updateUserRoles(newUser.getKey(), newUser.getValue(), Set.of());
        }
    }

    private void updateUserRoles(long userId, Set<Long> newRoles, Set<Long> removeRoles) {
        if (newRoles.isEmpty() && removeRoles.isEmpty()) {
            return;
        }

        Member member = members.get(userId);
        List<Role> newRoleObjects = newRoles.stream().map(dcGuild::getRoleById).toList();
        List<Role> removeRoleObjects = removeRoles.stream().map(dcGuild::getRoleById).toList();

        String newRoleNames = newRoleObjects.stream().map(Role::getName).collect(Collectors.joining(", ", "[", "]"));
        String removeRoleNames = removeRoleObjects.stream()
                .map(Role::getName)
                .collect(Collectors.joining(", ", "[", "]"));
        log.info(
                () -> "Change roles for " + member.getEffectiveName() + ". New Roles: " + newRoleNames + "; Remove Roles: " + removeRoleNames);

        if (Config.doChangeRoles) {
            dcGuild.modifyMemberRoles(member, newRoleObjects, removeRoleObjects).queue();
        }
    }
}
