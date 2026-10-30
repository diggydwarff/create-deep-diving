package com.diggydwarff.deepdiving.equipment;

/** Angles are beam half angles, in degrees; range scales the player's configured lamp range. */
public enum LampProfile {
    DIVING(1, -1, 1),
    FLOOD(.70, 50, .85),
    LONG_THROW(1.8, 12, 1.40),
    INSPECTION(.45, 76, .72);
    public final double rangeScale, halfAngle, intensity;

    LampProfile(double rangeScale, double halfAngle, double intensity) {
        this.rangeScale = rangeScale;
        this.halfAngle = halfAngle;
        this.intensity = intensity;
    }

    public static LampProfile byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : DIVING;
    }
}
