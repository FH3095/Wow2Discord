package eu._4fh.wow2discord.wow;

import eu._4fh.abstract_bnet_api.oauth2.BattleNetClient;
import eu._4fh.abstract_bnet_api.oauth2.BattleNetRegion;
import eu._4fh.abstract_bnet_api.restclient.RequestExecutor;
import eu._4fh.abstract_bnet_api.restclient.data.BattleNetWowCharacter;
import eu._4fh.abstract_bnet_api.restclient.requests.BattleNetGuildMembersRequest;
import eu._4fh.wow2discord.db.DbGuilds.DbGuild;
import eu._4fh.wow2discord.db.Transaction;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WowDcComparator {

    private final DbGuild guild;
    private final Map<Long, BattleNetWowCharacter> bnetCharacters;
    private final Map<Long, BattleNetWowCharacter> stayedInGuildCharacters;
    private final Set<Long> newInGuild;
    private final Set<Long> removedFromGuild;

    public WowDcComparator(DbGuild guild) {
        this.guild = guild;
        bnetCharacters = Collections.unmodifiableMap(fetchBnetCharacters());

        Set<Long> dcCharacters;
        try (Transaction t = new Transaction()) {
            dcCharacters = t.guildCharacters.existingCharactersForGuild(guild.guildId());
        }

        Set<Long> tmp = new HashSet<>(bnetCharacters.keySet());
        tmp.removeAll(dcCharacters);
        newInGuild = Collections.unmodifiableSet(tmp);
        tmp = new HashSet<>(dcCharacters);
        tmp.removeAll(bnetCharacters.keySet());
        removedFromGuild = Collections.unmodifiableSet(tmp);

        Map<Long, BattleNetWowCharacter> tmpMap = new HashMap<>(bnetCharacters);
        tmpMap.keySet().removeAll(newInGuild);
        stayedInGuildCharacters = Collections.unmodifiableMap(tmpMap);
    }

    private Map<Long, BattleNetWowCharacter> fetchBnetCharacters() {
        BattleNetRegion region = BattleNetRegion.getRegion(guild.wowRegion());
        BattleNetClient client = BnetClients.i().getClient(region);
        RequestExecutor executor = new RequestExecutor(client, region.locales.iterator().next().toLanguageTag());
        return executor.executeRequest(BattleNetGuildMembersRequest.getApiPath(guild.wowRealm(), guild.wowName()),
                        new BattleNetGuildMembersRequest())
                .stream()
                .collect(Collectors.toMap(character -> character.id, Function.identity()));
    }

    public Map<Long, BattleNetWowCharacter> getBnetCharacters() {
        return bnetCharacters;
    }

    public Map<Long, BattleNetWowCharacter> getStayedInGuildCharacters() {
        return stayedInGuildCharacters;
    }

    public Set<Long> getNewInGuild() {
        return newInGuild;
    }

    public Set<Long> getRemovedFromGuild() {
        return removedFromGuild;
    }
}
