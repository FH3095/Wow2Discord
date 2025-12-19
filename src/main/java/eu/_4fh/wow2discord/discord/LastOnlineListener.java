package eu._4fh.wow2discord.discord;

import eu._4fh.wow2discord.db.Transaction;
import eu._4fh.wow2discord.util.CronTasks;
import eu._4fh.wow2discord.util.Log;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class LastOnlineListener extends ListenerAdapter {

    private record GuildAndUserId(long guildId, long userId) {
    }

    private final Logger log = Log.getLog(this);
    private final Set<GuildAndUserId> todaySeenUsers = ConcurrentHashMap.newKeySet();

    public LastOnlineListener() {
        CronTasks.i().executeDaily(todaySeenUsers::clear);
    }

    @Override
    public void onUserUpdateOnlineStatus(UserUpdateOnlineStatusEvent event) {
        OnlineStatus newStatus = event.getNewOnlineStatus();
        OnlineStatus oldStatus = event.getOldOnlineStatus();
        if ((newStatus == OnlineStatus.OFFLINE || newStatus == OnlineStatus.UNKNOWN) && (oldStatus == OnlineStatus.OFFLINE || oldStatus == OnlineStatus.UNKNOWN)) {
            // Old and new status are either offline or unknown, don't update database
            return;
        }

        long guildId = event.getGuild().getIdLong();
        long userId = event.getMember().getIdLong();
        String name = event.getMember().getEffectiveName();
        if (todaySeenUsers.contains(new GuildAndUserId(guildId, userId))) {
            // User already seen today -> nothing to do
            return;
        }

        log.fine(
                () -> "Member LastOnline set guild " + guildId + " user " + userId + " with name " + name + " changing status from " + oldStatus + " to " + newStatus);
        synchronized (this) {
            try (Transaction t = new Transaction()) {
                if (t.onlineUsers.exists(guildId, userId)) {
                    t.onlineUsers.update(guildId, userId, name);
                } else {
                    t.onlineUsers.insert(guildId, userId, name);
                }
                t.commit();
            }
            todaySeenUsers.add(new GuildAndUserId(guildId, userId));
        }
    }
}
