package com.diggydwarff.deepdiving.physiology;

/** Separates chronic overload from sudden compression, with rated equipment protecting both. */
public final class PressureInjury {
    public record Result(double injury, double shock, boolean catastrophic) {}

    public static Result step(
        double previousInjury,
        double previousPressure,
        double pressure,
        double rating,
        boolean exposed,
        boolean historyValid,
        boolean suddenEnabled,
        double shockDelta,
        boolean fatalEnabled,
        double fatalDelta,
        double fatalExcess,
        double severity
    ) {
        double excess = exposed ? Math.max(0, pressure - rating) : 0;

        double delta = historyValid && exposed ? Math.max(0, pressure - previousPressure) : 0;

        double shock = suddenEnabled && excess > 0 && delta >= shockDelta
                ? Math.min(100, Math.min(delta, excess) * 4 * severity)
                : 0;

        boolean catastrophic = fatalEnabled && shock > 0 && delta >= fatalDelta && excess >= fatalExcess;

        double injury = Math.clamp(
            previousInjury
                    + (excess > 0 ? Math.min(20, excess * 1.4) * severity : -5)
                    + shock,
            0,
            100
        );

        return new Result(injury, shock, catastrophic);
    }

    private PressureInjury() {}
}
