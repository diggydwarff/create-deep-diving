package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.diving.DiverService;

/** Edge-triggered alarms: a persistent warning never becomes a repeating beep. */
public final class DiveAlarmState {
    public enum Signal {
        NONE,
        CAUTION,
        URGENT
    }

    private static final int URGENT = DiverService.BLACKOUT
            | DiverService.PRESSURE_SHOCK
            | DiverService.NO_SUPPLY
            | DiverService.HYPOXIA
            | DiverService.RATING
            | DiverService.TOXICITY
            | DiverService.PRESSURE_INJURY;

    private static final int CAUTION = DiverService.LOW_GAS | DiverService.ASCENT | DiverService.DECO;

    private int previousWarnings;

    private int pendingWarnings;

    private int lastPriority;

    private long lastPlayed = Long.MIN_VALUE / 2;

    public Signal update(
        int warnings,
        long tick,
        int cooldownTicks,
        boolean audible
    ) {
        int current = warnings & (URGENT | CAUTION);

        // Remember newly raised warnings while they remain active. Resolved warnings cannot beep
        // later.
        int newlyRaised = current & ~previousWarnings;

        pendingWarnings = (pendingWarnings | newlyRaised) & current;
        previousWarnings = current;

        if (!audible) {
            pendingWarnings = 0;
            return Signal.NONE;
        }

        int priority = warningPriority(pendingWarnings);

        if (priority == 0 || tick - lastPlayed < cooldownTicks && priority <= lastPriority) {
            return Signal.NONE;
        }

        lastPlayed = tick;
        lastPriority = priority;
        pendingWarnings = 0;
        return priority >= 2 ? Signal.URGENT : Signal.CAUTION;
    }

    private static int warningPriority(int warnings) {
        int immediate = DiverService.BLACKOUT | DiverService.PRESSURE_SHOCK | DiverService.NO_SUPPLY;

        if ((warnings & immediate) != 0) {
            return 3;
        }

        if ((warnings & URGENT) != 0) {
            return 2;
        }

        if (warnings != 0) {
            return 1;
        }

        return 0;
    }
}
