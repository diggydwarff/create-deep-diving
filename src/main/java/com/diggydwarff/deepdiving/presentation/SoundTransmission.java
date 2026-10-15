package com.diggydwarff.deepdiving.presentation;

/** Small, deterministic blend shared by the listener and individual sound sources. */
public final class SoundTransmission {
    public static float crossing(float listenerWater, float sourceWater) {
        return listenerWater * (1 - sourceWater) + (1 - listenerWater) * sourceWater;
    }

    public static float approach(float current, float target, double seconds) {
        float step = (float) (1 / Math.max(1, seconds * 20));

        if (Math.abs(current - target) <= step + .000001f) {
            return target;
        }

        if (current < target) {
            return Math.min(target, current + step);
        }

        return Math.max(target, current - step);
    }

    private SoundTransmission() {}
}
