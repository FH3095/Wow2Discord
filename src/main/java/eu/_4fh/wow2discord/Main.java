package eu._4fh.wow2discord;

import eu._4fh.wow2discord.db.Db;
import eu._4fh.wow2discord.discord.Discord;
import eu._4fh.wow2discord.discord.LastOnlineListener;
import eu._4fh.wow2discord.util.CronTasks;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        try (CronTasks tasks = new CronTasks(); Db db = new Db(); Discord discord = new Discord(
                new LastOnlineListener())) {
            System.in.read();
        }
    }
}
