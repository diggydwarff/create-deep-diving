package com.diggydwarff.deepdiving.presentation;

/** Independent camera angles. No entity rotation or movement belongs to this state. */
public final class OrbitControls {
    private boolean active;

    private float yaw;

    private float pitch;

    private float distance = 4;

    /** Preserve the host's complete camera cycle, including camera types added at runtime. */
    public <T> T next(
        T current,
        T ordinaryNext,
        T firstPerson,
        T rearView,
        boolean eligible,
        float heading,
        float lookPitch
    ) {
        if (active) {
            reset();
            return firstPerson;
        }

        // Add orbit at the wrap to first person, after any other mod's extra views. Identity
        // comparisons deliberately avoid exhaustive switches over an extensible enum.
        if (eligible && current != firstPerson && ordinaryNext == firstPerson) {
            active = true;
            yaw = wrap(heading);
            pitch = clampPitch(lookPitch);
            distance = 4;
            return rearView;
        }

        return ordinaryNext;
    }

    public void turn(double horizontal, double vertical) {
        if (!active || !Double.isFinite(horizontal) || !Double.isFinite(vertical)) {
            return;
        }

        // Match Entity.turn's sensitivity scale, without calling Entity.turn.
        yaw = wrap(yaw + horizontal * .15);
        pitch = clampPitch(pitch + vertical * .15);
    }

    public void zoom(double scroll) {
        if (active && Double.isFinite(scroll)) {
            distance = (float) Math.clamp(distance - scroll * .5, 1.5, 12);
        }
    }

    private static float wrap(double value) {
        if (!Double.isFinite(value)) {
            return 0;
        }

        double result = value % 360;

        if (result >= 180) {
            result -= 360;
        }

        if (result < -180) {
            result += 360;
        }

        return (float) result;
    }

    private static float clampPitch(double value) {
        return Double.isFinite(value) ? (float) Math.clamp(value, -85, 85) : 0;
    }

    public boolean active() {
        return active;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public float distance() {
        return distance;
    }

    public void reset() {
        active = false;
        pitch = 0;
        yaw = 0;
        distance = 4;
    }
}
