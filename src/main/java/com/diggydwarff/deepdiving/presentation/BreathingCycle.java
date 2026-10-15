package com.diggydwarff.deepdiving.presentation;

/** Cosmetic timing only. The existing diving service still owns every unit of gas. */
public final class BreathingCycle {
    public static final int STOPPED = 0;

    public static final int INHALE = 1;

    public static final int EXHALE = 2;

    private int age;

    private int length;

    public void restart(double exertion, double oxygenation) {
        // Faster swimming and existing oxygen stress shorten the quiet part of the cycle.
        double effort = Math.clamp(exertion, 0, 1);

        double stress = Math.clamp((80 - oxygenation) / 60, 0, 1);

        length = (int) Math.round(80 - 22 * effort - 10 * stress);
        age = 0;
    }

    public int phase() {
        if (age < inhaleTicks()) {
            return INHALE;
        }

        if (age >= exhaleStart() && age < exhaleStart() + exhaleTicks()) {
            return EXHALE;
        }

        return STOPPED;
    }

    public boolean startsPhase() {
        return age == 0 || age == exhaleStart();
    }

    public int remainingTicks() {
        return switch (phase()) {
            case INHALE -> inhaleTicks() - age;
            case EXHALE -> exhaleStart() + exhaleTicks() - age;
            default -> 0;
        };
    }

    public boolean advance() {
        age++;

        return age >= length;
    }

    private int inhaleTicks() {
        return Math.max(12, length / 4);
    }

    private int exhaleStart() {
        return length * 2 / 5;
    }

    private int exhaleTicks() {
        return Math.max(14, length * 3 / 10);
    }
}
