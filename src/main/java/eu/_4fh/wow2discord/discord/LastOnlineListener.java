package eu._4fh.wow2discord.discord;

import eu._4fh.wow2discord.Log;
import eu._4fh.wow2discord.db.Transaction;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.logging.Logger;

public class LastOnlineListener extends ListenerAdapter {

    private final Logger log = Log.getLog(this);

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
        log.info(
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
        }
    }
}
