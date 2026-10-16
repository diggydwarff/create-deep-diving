package com.diggydwarff.deepdiving.client;

/** Continuous, bounded condition strengths; no render targets or gameplay state are stored here. */
public final class ConditionVisualState {
    public record Levels(
        double hypoxia,
        double narcosis,
        double toxicity,
        double decompression,
        double pressure,
        double shock
    ) {
        public static final Levels NONE = new Levels(0, 0, 0, 0, 0, 0);

        public Levels {
            hypoxia = unit(hypoxia);
            narcosis = unit(narcosis);
            toxicity = unit(toxicity);
            decompression = unit(decompression);
            pressure = unit(pressure);
            shock = unit(shock);
        }

        public double maximum() {
            return Math.max(
                Math.max(Math.max(hypoxia, narcosis), Math.max(toxicity, decompression)),
                Math.max(pressure, shock)
            );
        }
    }

    private Levels previous = Levels.NONE;

    private Levels current = Levels.NONE;

    private double clock;

    public static Levels fromPhysiology(
        double oxygen,
        double narcosis,
        double toxicity,
        double stress,
        double injury,
        double shock
    ) {
        return new Levels(
            (70 - oxygen) / 50,
            (narcosis - .2) / 2.6,
            (toxicity - 15) / 100,
            (stress - 10) / 70,
            (injury - 10) / 90,
            shock / 100
        );
    }

    public void tick(Levels target) {
        previous = current;
        current = new Levels(
            approach(current.hypoxia, target.hypoxia),
            approach(current.narcosis, target.narcosis),
            approach(current.toxicity, target.toxicity),
            approach(current.decompression, target.decompression),
            approach(current.pressure, target.pressure),
            approach(current.shock, target.shock)
        );
        clock = (clock + .05) % 3600;
    }

    public Levels sample(double partial) {
        double p = unit(partial);

        return new Levels(
            lerp(previous.hypoxia, current.hypoxia, p),
            lerp(previous.narcosis, current.narcosis, p),
            lerp(previous.toxicity, current.toxicity, p),
            lerp(previous.decompression, current.decompression, p),
            lerp(previous.pressure, current.pressure, p),
            lerp(previous.shock, current.shock, p)
        );
    }

    public double time(double partial) {
        return clock + unit(partial) / 20;
    }

    public void reset() {
        previous = current = Levels.NONE;
        clock = 0;
    }

    private static double approach(double value, double target) {
        double next = value + (target - value) * (target > value ? .08 : .12);

        return target == 0 && next < .001 ? 0 : next;
    }

    private static double unit(double value) {
        return Double.isFinite(value) ? Math.clamp(value, 0, 1) : 0;
    }

    private static double lerp(double a, double b, double amount) {
        return a + (b - a) * amount;
    }
}
