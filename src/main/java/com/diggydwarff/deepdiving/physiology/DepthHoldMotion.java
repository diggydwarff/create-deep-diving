package com.diggydwarff.deepdiving.physiology;

/** Gentle vertical correction; neither horizontal movement nor flight is changed. */
public final class DepthHoldMotion {
    public static double vertical(double currentY, double targetY, double ascentLimit) {
        if (!Double.isFinite(currentY)
            || !Double.isFinite(targetY)
            || !Double.isFinite(ascentLimit)
            || ascentLimit <= 0) {
            return 0;
        }

        double error = targetY - currentY;

        if (Math.abs(error) < .015) {
            return 0;
        }

        return Math.clamp(error * .15, -.05, Math.min(.05, ascentLimit * .8 / 20));
    }

    private DepthHoldMotion() {}
}
