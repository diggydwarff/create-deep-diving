package com.diggydwarff.deepdiving.config;

import net.minecraft.resources.ResourceLocation;

/** A reversible pressure curve shared by breathing, equipment and decompression readouts. */
public record PressureProfile(
    Mode mode,
    double scale,
    double referencePressure,
    double exponent,
    Integer surfaceY
) {
    public enum Mode {
        LINEAR,
        REFERENCE
    }

    public PressureProfile {
        if (mode == null
            || !Double.isFinite(scale)
            || scale <= 0
            || scale > 65536
            || !Double.isFinite(referencePressure)
            || referencePressure <= 1
            || referencePressure > 1000
            || !Double.isFinite(exponent)
            || exponent < 0.25
            || exponent > 4) {
            throw new IllegalArgumentException("Invalid pressure profile");
        }
    }

    public double pressure(double depth) {
        double ratio = Math.max(0, depth) / scale;

        return Math.min(
            1000,
            1
                    + (mode == Mode.LINEAR
                            ? ratio
                            : (referencePressure - 1) * Math.pow(ratio, exponent))
        );
    }

    public double depthAtPressure(double pressure) {
        double excess = Math.max(0, pressure - 1);

        return scale
                * (mode == Mode.LINEAR
                        ? excess
                        : Math.pow(excess / (referencePressure - 1), 1 / exponent));
    }

    public record DimensionOverride(ResourceLocation dimension, PressureProfile profile) {}

    /** dimension|mode|blocks-per-atm-or-reference-depth|reference-atm|curve|AUTO-or-surface-Y */
    public static DimensionOverride parse(String entry) {
        String[] parts = entry.split("\\|", -1);

        if (parts.length != 6) {
            throw new IllegalArgumentException("Expected six profile fields");
        }

        ResourceLocation dimension = ResourceLocation.parse(parts[0].trim());

        Mode mode = Mode.valueOf(parts[1].trim().toUpperCase(java.util.Locale.ROOT));

        Integer surface = parts[5].trim().equalsIgnoreCase("AUTO") ? null : Integer.valueOf(parts[5].trim());

        return new DimensionOverride(
            dimension,
            new PressureProfile(
                mode,
                Double.parseDouble(parts[2].trim()),
                Double.parseDouble(parts[3].trim()),
                Double.parseDouble(parts[4].trim()),
                surface
            )
        );
    }

    public static boolean validEntry(Object value) {
        if (!(value instanceof String entry)) {
            return false;
        }

        try {
            parse(entry);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
