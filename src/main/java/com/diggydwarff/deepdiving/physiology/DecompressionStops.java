package com.diggydwarff.deepdiving.physiology;

import com.diggydwarff.deepdiving.config.PressureProfile;
import com.diggydwarff.deepdiving.gas.GasMix;

/** The server's rounded stop depth and an estimate for clearing the next three-block step. */
public final class DecompressionStops {
    public static double depth(TissueModel tissues, PressureProfile profile) {
        double raw = profile.depthAtPressure(tissues.ceilingPressure());

        return raw < .05 ? 0 : Math.ceil(raw / 3) * 3;
    }

    public static int secondsToNextStop(
        TissueModel tissues,
        PressureProfile profile,
        GasMix mix,
        double breathingPressure,
        double currentDepth,
        double tissueRate,
        double offGasRate,
        boolean exchangingGas
    ) {
        double stop = depth(tissues, profile);

        if (stop == 0) {
            return 0;
        }

        if (!exchangingGas || currentDepth + .05 < stop) {
            return -1;
        }

        double next = stop - 3;

        // Clearing the final stop uses the same tiny depth tolerance as depth().
        double target = profile.pressure(next == 0 ? .05 - .000001 : next);

        return tissues.secondsToCeiling(mix, breathingPressure, tissueRate, offGasRate, target);
    }

    private DecompressionStops() {}
}
