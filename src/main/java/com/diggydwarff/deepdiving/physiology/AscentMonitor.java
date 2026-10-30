package com.diggydwarff.deepdiving.physiology;

/** A brief depth adjustment is not a sustained rapid ascent. Never persisted between sessions. */
public final class AscentMonitor {
    private double fastSeconds;
    private double fastRise;
    private boolean warning;

    public boolean update(
        double rate,
        double seconds,
        boolean tracking,
        double limit,
        double graceSeconds,
        double minimumRise
    ) {
        if (!tracking
            || !Double.isFinite(rate)
            || !Double.isFinite(seconds)
            || seconds <= 0
            || !Double.isFinite(limit)
            || limit <= 0
            || !Double.isFinite(graceSeconds)
            || graceSeconds < 0
            || !Double.isFinite(minimumRise)
            || minimumRise < 0
            || rate <= limit) {
            reset();
            return false;
        }

        // Keep only enough history to satisfy the configured thresholds; no unbounded counters.
        fastSeconds = Math.min(graceSeconds, fastSeconds + seconds);
        fastRise = Math.min(minimumRise, fastRise + rate * seconds);
        warning = fastSeconds >= graceSeconds && fastRise >= minimumRise;
        return warning;
    }

    public boolean warning() {
        return warning;
    }

    public void reset() {
        fastSeconds = 0;
        fastRise = 0;
        warning = false;
    }
}
