package eu._4fh.wow2discord;

import java.util.logging.Logger;

public class Log {
    private Log() {
    }

    public static Logger getLog(Object object) {
        return Logger.getLogger(object.getClass().getName());
    }
}
