package com.diggydwarff.deepdiving.physiology;

/** Visible wave-like motion over eight seconds; never bob above a decompression ceiling. */
public final class BuoyancyMotion {
    public static double bobTarget(double baseY, long time, double roomAbove) {
        return baseY + Math.min(Math.max(0, roomAbove), .30 * Math.sin(time * Math.PI * 2 / 160));
    }

    public static double horizontal(double current, double target) {
        return Double.isFinite(current) && Double.isFinite(target)
                ? Math.clamp((target - current) * .12, -.025, .025)
                : 0;
    }

    private BuoyancyMotion() {}
}
