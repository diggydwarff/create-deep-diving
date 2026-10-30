package com.diggydwarff.deepdiving.presentation;

/** A short uneven burst followed by single trailing bubbles within the existing exhale phase. */
public final class ExhaleBubbles {
    public static int count(int age, int duration, int variation) {
        if (age < 0 || age >= duration || duration < 1 || duration > 40) return 0;
        if (age == 0) return 5 + Math.floorMod(variation, 3);
        if (age == 2 + ((variation >>> 1) & 1)) return 2 + ((variation >>> 2) & 1);
        int trailStart = 6 + ((variation >>> 3) & 1);
        int trailSpacing = 4 + ((variation >>> 4) & 3);
        return age >= trailStart && (age - trailStart) % trailSpacing == 0 ? 1 : 0;
    }

    private ExhaleBubbles() {}
}
