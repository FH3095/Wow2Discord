package eu._4fh.wow2discord.wow;

import eu._4fh.abstract_bnet_api.restclient.data.BattleNetWowCharacter;
import eu._4fh.wow2discord.db.DbGuildCharacters;
import eu._4fh.wow2discord.db.DbGuilds.DbGuild;
import eu._4fh.wow2discord.db.Transaction;
import eu._4fh.wow2discord.discord.Discord;
import eu._4fh.wow2discord.util.Config;
import eu._4fh.wow2discord.util.CronTasks;
import eu._4fh.wow2discord.util.Log;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Regularly checks the WOW guild for the characters and handles the difference.
 */
public class GuildCheck {

    private final Logger log = Log.getLog(this);

    public GuildCheck() {
    }

    public void start() {
        CronTasks.i().executeRegularly(this::runCheck, Duration.ofHours(Config.guildCheckInterval));
    }

    private void runCheck() {
        List<DbGuild> guilds;
        try (Transaction t = new Transaction()) {
            guilds = t.guilds.getGuilds();
        }
        for (DbGuild guild : guilds) {
            WowDcComparator comparator = new WowDcComparator(guild);
            announceAndSaveNewCharacters(guild, comparator.getNewInGuild(), comparator.getBnetCharacters());
            updateCharacters(guild, comparator.getStayedInGuildCharacters());
        }
    }

    /**
     * Announce new characters to the channel und save them to the database
     */
    private void announceAndSaveNewCharacters(DbGuild guild, Set<Long> newInGuild, Map<Long, BattleNetWowCharacter> bnetCharacters) {
        Discord discord = Discord.i();
        int announcedCharacters = 0;
        try (Transaction t = new Transaction()) {
            for (long newCharacterId : newInGuild) {
                BattleNetWowCharacter character = bnetCharacters.get(newCharacterId);

                log.info(
                        () -> "New character for guild " + guild.wowName() + "-" + guild.wowRealm() + "(" + guild.wowGuildId() + "): " + character.name + "_" + character.realmSlug + " (" + character.id + ")");

                if (announcedCharacters < Config.maxAnnouncedCharactersAtOnce) {
                    String message = "New character in guild: " + character.name + "-" + character.realmSlug + " (" + character.id + ") with rank " + character.guildRank + ". To assign the character to a discord account use /wow-link-character " + character.id + " DiscordUsername";
                    discord.sendMessage(guild.channelId(), message);
                    announcedCharacters++;
                }
                t.guildCharacters.insert(
                        new DbGuildCharacters.WowCharacter(guild.guildId(), character.id, character.realmSlug,
                                character.name, Objects.requireNonNullElse(character.guildRank, (byte) 127)));
            }
            t.commit();
        }
    }

    /**
     * Update existing characters from the guild to the database
     */
    private void updateCharacters(DbGuild guild, Map<Long, BattleNetWowCharacter> stayedInGuildCharacters) {
        try (Transaction t = new Transaction()) {
            for (BattleNetWowCharacter character : stayedInGuildCharacters.values()) {
                t.guildCharacters.update(
                        new DbGuildCharacters.WowCharacter(guild.guildId(), character.id, character.realmSlug,
                                character.name, Objects.requireNonNullElse(character.guildRank, (byte) 127)));
            }
            t.commit();
        }
    }
}
