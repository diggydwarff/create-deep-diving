package com.diggydwarff.deepdiving.presentation;

/** A half-second exhale followed by a quiet interval; staggered per diver. */
public final class Exhalation {
    public static int particles(long tick, int phaseSeed) {
        int phase = (int) Math.floorMod(Math.floorMod(tick, 72) + Math.floorMod(phaseSeed, 72), 72);

        if (phase >= 12 || phase % 2 != 0) {
            return 0;
        }

        return phase < 6 ? 3 : 2;
    }

    private Exhalation() {}
}
