package eu._4fh.wow2discord.discord;

import eu._4fh.wow2discord.db.Transaction;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class LastOnlineListener extends ListenerAdapter {
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
