package eu._4fh.wow2discord.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CronTasks implements AutoCloseable {

    private static final Singleton<CronTasks> instance = new Singleton<>(CronTasks.class);

    public static CronTasks i() {
        return instance.get();
    }

    public CronTasks() {
        instance.set(this);
    }

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public void executeDaily(Runnable command) {
        Instant now = Instant.now();
        Instant nextDayStart = now.plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        long secondsTillNextDay = ChronoUnit.SECONDS.between(now, nextDayStart);
        executor.scheduleAtFixedRate(command, secondsTillNextDay, TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        executor.shutdown();
        instance.unset(this);
    }
}
