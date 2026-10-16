package com.diggydwarff.deepdiving.physiology;

/** Game symptom curves; oxygen reserve is not a real pulse-oximeter percentage. */
public final class DiveDynamics {
    public static double hypoxia(double oxygen) {
        return Math.clamp((75 - oxygen) / 65, 0, 1);
    }

    public static double movementScale(double oxygen, boolean blackout) {
        return blackout ? 0 : 1 - 0.65 * hypoxia(oxygen);
    }

    /** Closing becomes slow and sustained as reserve falls, with a brief open interval. */
    public static double eyelidClosure(double seconds, double severity, boolean blackout) {
        if (blackout) {
            return 1;
        }

        if (severity < 0.08) {
            return 0;
        }

        double period = 5.5 - 3 * severity;

        double duration = 0.18 + 2.0 * severity * severity;

        double phase = (seconds % period + period) % period;

        if (phase >= duration) {
            return 0;
        }

        double unit = phase / duration;

        if (unit < 0.42) {
            return smooth(unit / 0.42);
        }

        if (unit < 0.65) {
            return 1;
        }

        return 1 - smooth((unit - 0.65) / 0.35);
    }

    private static double smooth(double v) {
        return v * v * (3 - 2 * v);
    }

    private DiveDynamics() {}
}
