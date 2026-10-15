package com.diggydwarff.deepdiving.physiology;

/** Small readout calculations shared by the server and client. These never plan a return dive. */
public final class DiveComputerTools {
    public static double gasSeconds(double gas, double consumption) {
        if (!Double.isFinite(gas) || gas < 0 || !Double.isFinite(consumption) || consumption <= 0) {
            return -1;
        }

        return gas / consumption;
    }

    public static double ascentFraction(double ascent, double limit) {
        if (!Double.isFinite(ascent) || !Double.isFinite(limit) || limit <= 0) {
            return 0;
        }

        return Math.clamp(ascent / limit, 0, 2);
    }

    private DiveComputerTools() {}
}
