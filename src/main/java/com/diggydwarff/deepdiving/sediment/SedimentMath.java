package com.diggydwarff.deepdiving.sediment;

/** Rendering math with no game dependencies, shared by clouds and checks. */
public final class SedimentMath {

    public static double minimumSpeed(double swimmingSpeed, boolean grounded) {
        return swimmingSpeed * (grounded ? 0.25 : 1);
    }

    public static double movementIntensity(double speed, boolean careful, boolean fins,
        double carefulMultiplier, double finsMultiplier) {
        double intensity = Math.clamp(speed / 0.12, 0.45, 1.5);

        if (careful) {
            intensity *= carefulMultiplier;
        }

        if (fins) {
            intensity *= finsMultiplier;
        }

        return intensity;
    }

    public static double cloudEnvelope(double age, double lifetime) {

        if (age < 0.0 || age >= lifetime) {
            return 0.0;
        }

        double growth = Math.min(1.0, age / 0.6);

        double remaining = 1.0 - age / lifetime;

        // A short bloom avoids a single-frame wall; settling fades gradually.
        return growth * remaining * remaining;
    }

    public static double radialDensity(double distanceSquared, double radius) {

        double radialFraction = distanceSquared / (radius * radius);

        return Math.max(0.0, 1.0 - radialFraction);
    }

    public static double cappedDensity(double density, double limit) {
        return Math.max(0.0, Math.min(limit, density));
    }

    public static double spawnChance(double density) {
        double settling = Math.clamp(density * 20, 0, 1);

        double concentration = Math.clamp(0.1 + density * 0.45, 0, 1);

        return settling * concentration;
    }

    private SedimentMath() {}
}
