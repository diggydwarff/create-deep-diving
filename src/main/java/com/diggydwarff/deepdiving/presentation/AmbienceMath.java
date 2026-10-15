package com.diggydwarff.deepdiving.presentation;

/** Depth curves and budgets stay independent of rendering and sound playback. */
public final class AmbienceMath {
    public static float depthBlend(double depth, double start, double full) {
        double range = Math.max(1, full - start);

        double position = Math.clamp((depth - start) / range, 0, 1);

        return (float) (position * position * (3 - 2 * position));
    }

    public static int particleTarget(int limit, double density, float depthBlend) {
        int boundedLimit = Math.clamp(limit, 0, 256);

        return Math.clamp((int) Math.round(boundedLimit * Math.clamp(density, 0, 1)
            * Math.clamp(depthBlend, 0, 1)), 0, boundedLimit);
    }

    public static int cueDelay(boolean deep, double frequency, double randomUnit) {
        if (frequency <= 0) {
            return Integer.MAX_VALUE;
        }

        double seconds = deep ? 60 + 120 * randomUnit : 30 + 60 * randomUnit;

        return (int) Math.clamp(Math.round(seconds * 20 / Math.min(frequency, 3)), 200, Integer.MAX_VALUE);
    }

    public static int particleLight(int packedLight, boolean luminous) {
        if (!luminous) {
            return packedLight;
        }

        // A low light-map floor reveals the speck itself. This never creates a world light.
        int block = Math.max(packedLight & 0xFFFF, 3 << 4);

        return (packedLight & 0xFFFF0000) | block;
    }

    private AmbienceMath() {}
}
