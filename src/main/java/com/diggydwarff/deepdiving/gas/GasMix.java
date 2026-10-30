package com.diggydwarff.deepdiving.gas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Integer percentages keep the filling transaction exact and tooltips readable. */
public record GasMix(int oxygen, int helium) {
    public static final GasMix AIR = new GasMix(21, 0);

    public static final GasMix NITROX = new GasMix(32, 0);

    public static final GasMix TRIMIX = new GasMix(18, 45);

    public static final GasMix HELIOX = new GasMix(21, 79);

    private record Percentages(int oxygen, int helium) {}

    public static final Codec<GasMix> CODEC =
            RecordCodecBuilder.<Percentages>create(
                            i ->
                                    i.group(
                                                    Codec.intRange(1, 100)
                                                            .fieldOf("oxygen")
                                                            .forGetter(Percentages::oxygen),
                                                    Codec.intRange(0, 99)
                                                            .fieldOf("helium")
                                                            .forGetter(Percentages::helium))
                                            .apply(i, Percentages::new))
                    .comapFlatMap(
                            p ->
                                    p.oxygen + p.helium <= 100
                                            ? com.mojang.serialization.DataResult.success(
                                                    new GasMix(p.oxygen, p.helium))
                                            : com.mojang.serialization.DataResult.error(
                                                    () -> "Gas fractions exceed 100%"),
                            m -> new Percentages(m.oxygen, m.helium));

    public GasMix {
        if (oxygen < 1 || oxygen > 100 || helium < 0 || helium > 100 - oxygen) {
            throw new IllegalArgumentException("Gas fractions must add to 100%, with some oxygen");
        }
    }

    // The percentage domain contains only 5,050 possible mixtures; the cache is bounded by that
    // domain.
    private static final Map<GasMix, int[][]> FLOW_PREFIXES = new ConcurrentHashMap<>();

    public int[] constituents(int amount) {
        return constituents(amount, 0);
    }

    /**
     * A monotonic 100-unit flow cycle preserves every constituent across repeated small batches.
     */
    public int[] constituents(int amount, int phase) {
        if (amount < 0 || amount > 1000000 || phase < 0 || phase >= 100) {
            throw new IllegalArgumentException("Invalid gas transaction quantity or phase");
        }
        int[][] prefix = FLOW_PREFIXES.computeIfAbsent(this, GasMix::flowPrefix);
        int end = phase + amount;
        int[] percentages = {oxygen, nitrogen(), helium};
        int[] units = new int[3];
        for (int gas = 0; gas < 3; gas++) {
            units[gas] = end / 100 * percentages[gas] + prefix[gas][end % 100] - prefix[gas][phase];
        }
        return units;
    }

    private int[][] flowPrefix() {
        int[] percentages = {oxygen, nitrogen(), helium};
        int[][] prefix = new int[3][101];
        for (int unit = 1; unit <= 100; unit++) {
            int best = 0;
            for (int gas = 0; gas < 3; gas++) {
                prefix[gas][unit] = prefix[gas][unit - 1];
                if (unit * percentages[gas] - prefix[gas][unit - 1] * 100
                        > unit * percentages[best] - prefix[best][unit - 1] * 100) best = gas;
            }
            prefix[best][unit]++;
        }
        return prefix;
    }

    public int nitrogen() {
        return 100 - oxygen - helium;
    }

    public int availableAmount(int requested, int[] available) {
        return availableAmount(requested, available, 0);
    }

    /** Largest producible partial batch at the current position in the conserved flow cycle. */
    public int availableAmount(int requested, int[] available, int phase) {
        if (requested < 0
                || requested > 1000000
                || available.length != 3
                || phase < 0
                || phase >= 100) throw new IllegalArgumentException("Invalid gas batch");
        for (int quantity : available)
            if (quantity < 0) throw new IllegalArgumentException("Negative gas buffer");
        int low = 0, high = requested;
        while (low < high) {
            int candidate = (low + high + 1) / 2;
            int[] needed = constituents(candidate, phase);
            if (needed[0] <= available[0] && needed[1] <= available[1] && needed[2] <= available[2])
                low = candidate;
            else high = candidate - 1;
        }
        return low;
    }

    public double oxygenFraction() {
        return oxygen / 100.0;
    }

    public double nitrogenFraction() {
        return nitrogen() / 100.0;
    }

    public double heliumFraction() {
        return helium / 100.0;
    }

    public double oxygenPartialPressure(double pressure) {
        return oxygenFraction() * pressure;
    }

    public double narcoticPressure(double pressure) {
        return nitrogenFraction() * pressure;
    }

    public double maximumDepth(double blocksPerAtmosphere) {
        return Math.max(0, (1.4 / oxygenFraction() - 1) * blocksPerAtmosphere);
    }

    public double minimumDepth(double blocksPerAtmosphere) {
        return Math.max(0, (0.16 / oxygenFraction() - 1) * blocksPerAtmosphere);
    }

    public double maximumDepth(com.diggydwarff.deepdiving.config.PressureProfile profile) {
        return profile.depthAtPressure(
                com.diggydwarff.deepdiving.config.DivingConfig.MAX_OXYGEN.get() / oxygenFraction());
    }

    public double minimumDepth(com.diggydwarff.deepdiving.config.PressureProfile profile) {
        return profile.depthAtPressure(
                com.diggydwarff.deepdiving.config.DivingConfig.MIN_OXYGEN.get() / oxygenFraction());
    }

    public String name() {
        if (equals(AIR)) {
            return "Air 21/79";
        }

        if (helium == 0) {
            return "Nitrox " + oxygen;
        }

        return (nitrogen() == 0 ? "Heliox " : "Trimix ") + oxygen + "/" + helium;
    }

    public String summary() {
        return String.format(
                Locale.ROOT, "O2 %d%% / N2 %d%% / He %d%%", oxygen, nitrogen(), helium);
    }
}
