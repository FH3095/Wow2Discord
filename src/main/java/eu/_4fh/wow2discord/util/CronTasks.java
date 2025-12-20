package eu._4fh.wow2discord.util;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility to execute tasks regularly.
 */
public class CronTasks implements AutoCloseable {

    private static final Singleton<CronTasks> instance = new Singleton<>(CronTasks.class);

    public static CronTasks i() {
        return instance.get();
    }

    private final Logger log = Log.getLog(this);
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

    private void executeAndLog(Runnable command) {
        taskExecutor.execute(() -> {
            try {
                command.run();
            } catch (Throwable t) {
                log.log(Level.SEVERE, "Cant run command " + command.toString(), t);
                throw t;
            }
        });
    }

    public void executeDaily(Runnable command) {
        Instant now = Instant.now();
        Instant nextDayStart = now.plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        long secondsTillNextDay = ChronoUnit.SECONDS.between(now, nextDayStart);
        taskStarter.scheduleAtFixedRate(() -> executeAndLog(command), secondsTillNextDay, TimeUnit.DAYS.toSeconds(1),
                TimeUnit.SECONDS);
    }

    public void executeRegularly(Runnable command, Duration interval) {
        long intervalInSeconds = interval.toSeconds();
        if (intervalInSeconds < 1) {
            throw new IllegalArgumentException("Interval " + interval + " is less than one second");
        }
        // We execute the task directly with some delay
        taskStarter.scheduleWithFixedDelay(() -> executeAndLog(command), rnd.nextInt(60) + 60, intervalInSeconds,
                TimeUnit.SECONDS);
    }
}
