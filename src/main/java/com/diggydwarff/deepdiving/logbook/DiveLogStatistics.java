package com.diggydwarff.deepdiving.logbook;

import com.diggydwarff.deepdiving.physiology.DiverState;

import net.minecraft.nbt.CompoundTag;

/** Integrates the computer's last authoritative reading, including the final partial second. */
public final class DiveLogStatistics {
    public static final long MAX_TICKS = 72 * 60 * 60 * 20L;
    private static final long SAMPLE_INTERVAL = 20;

    public static void sample(CompoundTag session, DiverState state, long now) {
        long previous = session.getLong("last");
        long elapsed = Math.clamp(now - previous, 0, SAMPLE_INTERVAL);
        long recorded = Math.clamp(session.getLong("ticks"), 0, MAX_TICKS);
        elapsed = Math.min(elapsed, MAX_TICKS - recorded);

        session.putLong("ticks", recorded + elapsed);
        session.putLong("last", now);
        session.putDouble(
                "maxPressure",
                Math.max(session.getDouble("maxPressure"), finite(state.pressure, 1)));
        session.putDouble(
                "maxCeiling", Math.max(session.getDouble("maxCeiling"), finite(state.ceiling, 0)));
        session.putInt("warnings", session.getInt("warnings") | state.warnings);

        if (!state.underwater) {
            return;
        }

        double depth = finite(state.depth, 0);
        session.putDouble("maxDepth", Math.max(session.getDouble("maxDepth"), depth));
        session.putDouble(
                "maxAscent", Math.max(session.getDouble("maxAscent"), finite(state.ascentRate, 0)));
        session.putLong("wetTicks", session.getLong("wetTicks") + elapsed);
        session.putDouble("depthSum", session.getDouble("depthSum") + depth * elapsed);
    }

    private static double finite(double value, double fallback) {
        return Double.isFinite(value) ? Math.max(0, value) : fallback;
    }

    private DiveLogStatistics() {}
}
