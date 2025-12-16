package eu._4fh.wow2discord;

import eu._4fh.wow2discord.db.Db;
import eu._4fh.wow2discord.db.Transaction;
import eu._4fh.wow2discord.discord.Discord;

import java.time.Duration;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        try (Discord discord = new Discord(); Db db = new Db(); Transaction t = new Transaction()) {
            if (t.onlineUsers.exists(1, 1)) {
                t.onlineUsers.update(1, 1, "FH");
            } else {
                t.onlineUsers.insert(1, 1, "FH2");
            }
            t.commit();
            Thread.sleep(Duration.ofSeconds(20));
        }
    }
}
