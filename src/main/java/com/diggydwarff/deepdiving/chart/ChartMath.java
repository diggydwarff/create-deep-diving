package com.diggydwarff.deepdiving.chart;

/** Deterministic bathymetry symbols, shared by sampling and the resource preview. */
public final class ChartMath {
    public static final int CONTOUR_INTERVAL = 10;
    public static final int SAMPLES_PER_TICK = 64;
    public static final int PIXEL_COUNT = 128 * 128;

    public static int pixelsPerMap(int heldMaps) {
        return SAMPLES_PER_TICK / Math.clamp(heldMaps, 1, 2);
    }

    public static int surveyStart(long tick, int heldMaps) {
        return (int) (tick * pixelsPerMap(heldMaps) & (PIXEL_COUNT - 1));
    }

    private static final String[] DIGITS = {
        "111101101101111", "010110010010111", "111001111100111", "111001111001111",
                "101101111001001",
        "111100111001111", "111100111101111", "111001010010010", "111101111101111",
                "111101111001111"
    };

    public static int band(int depth) {
        if (depth <= 10) return 0;
        if (depth <= 20) return 1;
        if (depth <= 40) return 2;
        if (depth <= 80) return 3;
        if (depth <= 160) return 4;
        return 5;
    }

    public static boolean contour(int depth, int neighbor) {
        return depth > 0 && neighbor > 0 && depth / CONTOUR_INTERVAL != neighbor / CONTOUR_INTERVAL;
    }

    public static boolean labelPixel(int depth, int x, int y) {
        if (depth <= 0 || y < 0 || y >= 5 || x < 0) return false;
        String value = Integer.toString(Math.min(9999, depth));
        int digit = x / 4;
        return digit < value.length()
                && x % 4 < 3
                && DIGITS[value.charAt(digit) - '0'].charAt(y * 3 + x % 4) == '1';
    }

    private ChartMath() {}
}
