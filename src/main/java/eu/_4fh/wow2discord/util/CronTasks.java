package eu._4fh.wow2discord.util;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CronTasks implements AutoCloseable {

    private static final Singleton<CronTasks> instance = new Singleton<>(CronTasks.class);

    public static CronTasks i() {
        return instance.get();
    }

    private final ScheduledExecutorService taskStarter = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService taskExecutor = Executors.newCachedThreadPool();
    private final Random rnd = new Random();

    public CronTasks() {
        instance.set(this);
    }

    @Override
    public void close() {
        taskStarter.shutdown();
        taskExecutor.shutdown();
        instance.unset(this);
    }

    public void executeDaily(Runnable command) {
        Instant now = Instant.now();
        Instant nextDayStart = now.plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        long secondsTillNextDay = ChronoUnit.SECONDS.between(now, nextDayStart);
        taskStarter.scheduleAtFixedRate(() -> taskExecutor.execute(command), secondsTillNextDay,
                TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
    }

    public void executeRegularly(Runnable command, Duration interval) {
        long intervalInSeconds = interval.toSeconds();
        if (intervalInSeconds < 1) {
            throw new IllegalArgumentException("Interval " + interval + " is less than one second");
        }
        // We execute the task directly with some delay
        taskStarter.scheduleWithFixedDelay(() -> taskExecutor.execute(command), rnd.nextInt(60) + 60, intervalInSeconds,
                TimeUnit.SECONDS);
    }
}
