package com.diggydwarff.deepdiving.physiology;

import com.diggydwarff.deepdiving.gas.GasMix;

import java.util.Arrays;

/** Four game-time compartments. Deliberately simplified, not a medical algorithm. */
public final class TissueModel {
    public static final double SURFACE_NITROGEN = 0.79 * (1 - 0.0627);

    private static final double[] HALF_TIMES = {60, 180, 480, 1200};

    public final double[] nitrogen = new double[4];

    public final double[] helium = new double[4];

    public TissueModel() {
        Arrays.fill(nitrogen, SURFACE_NITROGEN);
    }

    public void step(GasMix mix, double pressure, double seconds) {
        step(mix, pressure, seconds, 1);
    }

    /** Speed recovery independently, so shorter stops do not make tissues load faster. */
    public void step(
        GasMix mix,
        double pressure,
        double seconds,
        double offGasRate
    ) {
        double dry = Math.max(0, pressure - 0.0627);

        for (int i = 0; i < 4; i++) {
            double n = dry * mix.nitrogenFraction();

            double h = dry * mix.heliumFraction();

            nitrogen[i] = approach(
                nitrogen[i],
                n,
                seconds * (n < nitrogen[i] ? offGasRate : 1),
                HALF_TIMES[i]
            );
            helium[i] = approach(
                helium[i],
                h,
                seconds * (h < helium[i] ? offGasRate : 1),
                HALF_TIMES[i] * 0.38
            );
        }
    }

    private static double approach(
        double current,
        double target,
        double seconds,
        double halfTime
    ) {
        return target
                + (current - target) * Math.exp(-Math.log(2) * Math.max(0, seconds) / halfTime);
    }

    public double ceilingPressure() {
        double ceiling = 1;

        for (int i = 0; i < 4; i++) {
            ceiling = Math.max(ceiling, (nitrogen[i] + helium[i] - 0.6) / 1.5);
        }

        return ceiling;
    }

    public double ceilingDepth(double blocksPerAtmosphere) {
        double d = (ceilingPressure() - 1) * blocksPerAtmosphere;

        return d < 0.05 ? 0 : Math.ceil(d / 3) * 3;
    }

    public double supersaturation(double pressure) {
        return Math.max(0, ceilingPressure() - pressure);
    }

    public int noDecoSeconds(GasMix mix, double pressure, double tissueRate) {
        return noDecoSeconds(mix, pressure, tissueRate, 1);
    }

    public int noDecoSeconds(
        GasMix mix,
        double pressure,
        double tissueRate,
        double offGasRate
    ) {
        return noDecoSeconds(mix, pressure, tissueRate, offGasRate, 1.005);
    }

    public int noDecoSeconds(
        GasMix mix,
        double pressure,
        double tissueRate,
        double offGasRate,
        double surfaceCeiling
    ) {
        if (!Double.isFinite(surfaceCeiling)
            || surfaceCeiling < 1
            || ceilingPressure() > surfaceCeiling) {
            return 0;
        }

        Projection future = projection(mix, pressure, tissueRate, offGasRate);

        if (future == null) {
            return 0;
        }

        if (future.cannotExceed(surfaceCeiling)) {
            return 3600;
        }

        // A gas switch can load one inert gas while unloading the other. Sample the first crossing
        // instead of assuming that their sum is monotonic.
        for (int second = 1; second <= 3600; second++) {
            future.advance();

            if (future.ceiling() > surfaceCeiling) {
                return second - 1;
            }
        }

        return 3600;
    }

    /** Next stop estimate at this pressure and gas; -1 means no crossing within an hour. */
    public int secondsToCeiling(
        GasMix mix,
        double pressure,
        double tissueRate,
        double offGasRate,
        double targetPressure
    ) {
        if (!Double.isFinite(targetPressure) || targetPressure < 1) {
            return -1;
        }

        Projection future = projection(mix, pressure, tissueRate, offGasRate);

        if (future == null) {
            return -1;
        }

        if (future.ceiling() <= targetPressure) {
            return 0;
        }

        if (future.cannotClear(targetPressure)) {
            return -1;
        }

        for (int second = 1; second <= 3600; second++) {
            future.advance();

            if (future.ceiling() <= targetPressure) {
                return second;
            }
        }

        return -1;
    }

    private Projection projection(
        GasMix mix,
        double pressure,
        double tissueRate,
        double offGasRate
    ) {
        if (mix == null
            || !Double.isFinite(pressure)
            || pressure < 1
            || !Double.isFinite(tissueRate)
            || tissueRate <= 0
            || !Double.isFinite(offGasRate)
            || offGasRate <= 0) {
            return null;
        }

        for (int i = 0; i < 4; i++) {
            if (!Double.isFinite(nitrogen[i])
                || !Double.isFinite(helium[i])
                || nitrogen[i] < 0
                || helium[i] < 0) {
                return null;
            }
        }

        return new Projection(mix, pressure, tissueRate, offGasRate);
    }

    /** Fixed gas/pressure lets us calculate each exponential once, then cheaply advance seconds. */
    private final class Projection {
        final double[] n = nitrogen.clone();

        final double[] h = helium.clone();

        final double[] nDecay = new double[4];

        final double[] hDecay = new double[4];

        final double targetN;

        final double targetH;

        Projection(
            GasMix mix,
            double pressure,
            double tissueRate,
            double offGasRate
        ) {
            double dry = Math.max(0, pressure - 0.0627);

            targetN = dry * mix.nitrogenFraction();
            targetH = dry * mix.heliumFraction();

            for (int i = 0; i < 4; i++) {
                nDecay[i] = Math.exp(
                    -Math.log(2)
                            * tissueRate
                            * (targetN < n[i] ? offGasRate : 1)
                            / HALF_TIMES[i]
                );
                hDecay[i] = Math.exp(
                    -Math.log(2)
                            * tissueRate
                            * (targetH < h[i] ? offGasRate : 1)
                            / (HALF_TIMES[i] * .38)
                );
            }
        }

        void advance() {
            for (int i = 0; i < 4; i++) {
                n[i] = targetN + (n[i] - targetN) * nDecay[i];
                h[i] = targetH + (h[i] - targetH) * hDecay[i];
            }
        }

        double ceiling() {
            double ceiling = 1;

            for (int i = 0; i < 4; i++) {
                ceiling = Math.max(ceiling, (n[i] + h[i] - .6) / 1.5);
            }

            return ceiling;
        }

        boolean cannotExceed(double pressure) {
            double allowed = pressure * 1.5 + .6;

            for (int i = 0; i < 4; i++) {
                if (Math.max(n[i], targetN) + Math.max(h[i], targetH) > allowed) {
                    return false;
                }
            }

            return true;
        }

        boolean cannotClear(double pressure) {
            double allowed = pressure * 1.5 + .6;

            for (int i = 0; i < 4; i++) {
                if (Math.min(n[i], targetN) + Math.min(h[i], targetH) > allowed) {
                    return true;
                }
            }

            return false;
        }
    }

    public double maximumLoading() {
        double v = 0;

        for (int i = 0; i < 4; i++) {
            v = Math.max(v, nitrogen[i] + helium[i]);
        }

        return v;
    }
}
