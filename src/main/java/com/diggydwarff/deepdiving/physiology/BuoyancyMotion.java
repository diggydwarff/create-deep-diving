package com.diggydwarff.deepdiving.physiology;

/** Subtle idle motion over ten seconds; never bob above a decompression ceiling. */
public final class BuoyancyMotion {
    /**
     * Passive fins settle near the diver instead of chasing an old target after lag or movement.
     */
    public static double restTarget(double currentY, double previousTargetY) {
        return Double.isFinite(previousTargetY) && Math.abs(previousTargetY - currentY) <= .5
                ? previousTargetY
                : currentY;
    }

    public static double bobTarget(double baseY, long time, double roomAbove) {
        return baseY + Math.min(Math.max(0, roomAbove), .08 * Math.sin(time * Math.PI * 2 / 200));
    }

    public static double horizontal(double current, double target) {
        return Double.isFinite(current) && Double.isFinite(target)
                ? Math.clamp((target - current) * .12, -.025, .025)
                : 0;
    }

    private BuoyancyMotion() {}
}
