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

        return (float) Math.clamp(1 - Math.max(0, horizontalSpeed) / .04, 0, 1);
    }

    public static float approach(float previous, float target) {
        return Math.clamp(previous + Math.clamp(target - previous, -.125f, .125f), 0, 1);
    }

    private TreadingMotion() {}
}
