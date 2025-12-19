package eu._4fh.wow2discord.util;

import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class Log {
    private Log() {
    }

    public static Logger getLog(@NotNull Object object) {
        return Logger.getLogger(object.getClass().getName());
    }
}
