package chess.model;

/**
 * A chess clock for one player. Time is measured from the system clock while
 * running, so it stays accurate even if the UI timer fires late.
 */
public class Clock {

    private long remainingMillis;
    private long runningSince = -1;

    public Clock(long totalMillis) {
        this.remainingMillis = totalMillis;
    }

    public void start() {
        if (!isRunning()) {
            runningSince = System.currentTimeMillis();
        }
    }

    public void stop() {
        if (isRunning()) {
            remainingMillis -= System.currentTimeMillis() - runningSince;
            runningSince = -1;
        }
    }

    public boolean isRunning() {
        return runningSince >= 0;
    }

    public long getRemainingMillis() {
        long remaining = remainingMillis;
        if (isRunning()) {
            remaining -= System.currentTimeMillis() - runningSince;
        }
        return Math.max(0, remaining);
    }

    public boolean isOutOfTime() {
        return getRemainingMillis() == 0;
    }

    /** "1:05:00", "4:59", or "0:09.3" when under ten seconds. */
    public String format() {
        long millis = getRemainingMillis();
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        if (millis < 10_000) {
            return String.format("0:%02d.%d", seconds, (millis % 1000) / 100);
        }
        return String.format("%d:%02d", minutes, seconds);
    }
}
