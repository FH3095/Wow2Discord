package eu._4fh.wow2discord.discord;

import eu._4fh.wow2discord.util.Config;
import eu._4fh.wow2discord.util.Singleton;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Discord implements AutoCloseable {

    private static final Singleton<Discord> instance = new Singleton<>(Discord.class);

    public static Discord i() {
        return instance.get();
    }

    private final JDA jda;

    public Discord(EventListener... listeners) {
        instance.set(this);
        int maximumThreads = Runtime.getRuntime().availableProcessors() * 5;
        // ThreadPool prefers to put a task into the queue before starting a new non-core-thread.
        // If we use a SynchronousQueue with limited number of threads, tasks that don't get a thread instantaneous
        // would be rejected.
        // Therefor, to have a limited number of threads that also timeout but not reject tasks that can't get a task
        // right away we have to assign all possible threads as coreThreads and have the coreThreads also time out.
        ThreadPoolExecutor executor = new ThreadPoolExecutor(maximumThreads, maximumThreads, 90, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
        executor.allowCoreThreadTimeOut(true);
        jda = JDABuilder.create(Config.discordToken, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES,
                        GatewayIntent.GUILD_MODERATION)
                .enableCache(CacheFlag.ONLINE_STATUS)
                .disableCache(CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.SCHEDULED_EVENTS)
                // To receive UserUpdateOnlineStatusEvents correctly, Users must be cached even when they are offline
                // Otherwise UserUpdateOnlineStatusEvents will only be fired when the user is not in the cache
                .setMemberCachePolicy(MemberCachePolicy.lru(5000))
                // Chunking=None means for no guild load all members on startup
                // Need to use loadMembers later to load them
                .setChunkingFilter(ChunkingFilter.NONE)
                .setAutoReconnect(true)
                .setEnableShutdownHook(true)
                .setStatus(OnlineStatus.ONLINE)
                .setEventPool(executor, true)
                .addEventListeners((Object[]) listeners)
                .build();
        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        updateCommands();
    }

    private void updateCommands() {
        SlashCommandData wowCmd = Commands.slash("wow", "WOW Commands");

        SubcommandData linkCmd = new SubcommandData("link",
                "Adds a link from a wow-character to a discord account").addOption(OptionType.INTEGER, "wow-char-id",
                        "WOW character id", true)
                .addOption(OptionType.USER, "discord-user", "Discord user to link character to", true);
        wowCmd.addSubcommands(linkCmd);

        SubcommandGroupData searchCmd = new SubcommandGroupData("search", "Commands to search");
        wowCmd.addSubcommandGroups(searchCmd);

        SubcommandData searchUnlinkedCmd = new SubcommandData("unlinked",
                "Search for not yet linked characters").addOption(OptionType.STRING, "char-name",
                        "Pattern of character name", false)
                .addOption(OptionType.INTEGER, "guild-rank", "Rank of the characters in the guild", false);
        searchCmd.addSubcommands(searchUnlinkedCmd);

        SubcommandData searchLinkedCmd = new SubcommandData("linked", "Search for already linked characters").addOption(
                        OptionType.STRING, "char-name", "Pattern of character name", false)
                .addOption(OptionType.INTEGER, "guild-rank", "Rank of the characters in the guild", false)
                .addOption(OptionType.USER, "discord-user", "Characters for this discord user", false);
        searchCmd.addSubcommands(searchLinkedCmd);

        jda.updateCommands().addCommands(wowCmd).complete();
    }

    public void close() {
        jda.shutdown();
        try {
            jda.awaitShutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        instance.unset(this);
    }

    public void sendMessage(long channelId, String message) {
        TextChannel channel = jda.getTextChannelById(channelId);
        Objects.requireNonNull(channel, "Unknown channel with id " + channelId + " for message " + message);
        channel.sendMessage(message).queue();
    }
}
