package eu._4fh.wow2discord.wow;

import eu._4fh.abstract_bnet_api.oauth2.BattleNetClient;
import eu._4fh.abstract_bnet_api.oauth2.BattleNetClients;
import eu._4fh.abstract_bnet_api.oauth2.BattleNetRegion;
import eu._4fh.wow2discord.util.Config;
import eu._4fh.wow2discord.util.Singleton;

import java.time.Duration;

/**
 * BattleNetClients to fetch characters from the guild
 */
public class BnetClients implements AutoCloseable {
    private static final Singleton<BnetClients> instance = new Singleton<>(BnetClients.class);

    public static BnetClients i() {
        return instance.get();
    }

    private final BattleNetClients clients;

    public BnetClients() {
        instance.set(this);
        clients = new BattleNetClients(Config.bnetApiKey, Config.bnetApiSecret, (int) Duration.ofHours(12).toSeconds(),
                "");
    }

    @Override
    public void close() {
        instance.unset(this);
    }

    public BattleNetClient getClient(BattleNetRegion region) {
        return clients.getApiClient(region);
    }
}
