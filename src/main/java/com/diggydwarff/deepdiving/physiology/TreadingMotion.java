package com.diggydwarff.deepdiving.physiology;

/** Render-only, slow limb motion. It never changes a diver's position or view. */
public final class TreadingMotion {
    public record Stroke(float kick, float sweep, float spread) {}

    public static Stroke stroke(double ticks) {
        if (!Double.isFinite(ticks)) {
            return new Stroke(0, 0, .2f);
        }

        return new Stroke(
            (float) (.22 * Math.sin(ticks * Math.PI * 2 / 44)),
            (float) (.1 * Math.sin(ticks * Math.PI * 2 / 72)),
            (float) (.2 + .06 * Math.cos(ticks * Math.PI * 2 / 72))
        );
    }

    public static float restWeight(double horizontalSpeed) {
        if (!Double.isFinite(horizontalSpeed)) {
            return 0;
        }

        // Passive buoyancy and small network corrections are not swim input.
        // Keep the full resting pose until the player is actually travelling.
        double movement = Math.max(0, Math.min(1, (horizontalSpeed - .02) / .06));

        return (float) (1 - movement * movement * (3 - 2 * movement));
    }

    public static float approach(float previous, float target) {
        float step = Math.max(-.125f, Math.min(.125f, target - previous));

        return Math.max(0, Math.min(1, previous + step));
    }

    /** Vanilla swimming can cross the same angle at either end of a full turn. */
    public static float blendAngle(float weight, float current, float target) {
        float turn = (float) (Math.PI * 2);

        float difference = (target - current) % turn;

        if (difference > Math.PI) {
            difference -= turn;
        } else if (difference < -Math.PI) {
            difference += turn;
        }

        // A normal lerp would take the long route at the wrap and jerk the arm.
        return current + difference * weight;
    }

    private TreadingMotion() {}
}
