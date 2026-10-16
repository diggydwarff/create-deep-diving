package com.diggydwarff.deepdiving.gas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Locale;

/** Integer percentages keep the filling transaction exact and tooltips readable. */
public record GasMix(int oxygen, int helium) {
    public static final GasMix AIR = new GasMix(21, 0);

    public static final GasMix NITROX = new GasMix(32, 0);

    public static final GasMix TRIMIX = new GasMix(18, 45);

    public static final GasMix HELIOX = new GasMix(21, 79);

    private record Percentages(int oxygen, int helium) {}

    public static final Codec<GasMix> CODEC = RecordCodecBuilder.<Percentages>create(
        i -> i.group(
            Codec.intRange(1, 100)
                .fieldOf("oxygen")
                .forGetter(Percentages::oxygen),
            Codec.intRange(0, 99)
                .fieldOf("helium")
                .forGetter(Percentages::helium)
        )
            .apply(i, Percentages::new)
    )
        .comapFlatMap(
            p -> p.oxygen + p.helium <= 100
                    ? com.mojang.serialization.DataResult.success(new GasMix(p.oxygen, p.helium))
                    : com.mojang.serialization.DataResult.error(() -> "Gas fractions exceed 100%"),
            m -> new Percentages(m.oxygen, m.helium)
        );

    public GasMix {
        if (oxygen < 1 || oxygen > 100 || helium < 0 || helium > 100 - oxygen) {
            throw new IllegalArgumentException("Gas fractions must add to 100%, with some oxygen");
        }
    }

    public int[] constituents(int amount) {
        if (amount < 0 || amount > 1000000) {
            throw new IllegalArgumentException("Invalid gas transaction quantity");
        }

        int[] units = {amount * oxygen / 100, amount * nitrogen() / 100, amount * helium / 100};

        int[] fractions = {amount * oxygen % 100, amount * nitrogen() % 100, amount * helium % 100};

        for (int remaining = amount - units[0] - units[1] - units[2]; remaining > 0; remaining--) {
            int best = 0;

            for (int i = 1; i < 3; i++) {
                if (fractions[i] > fractions[best]) {
                    best = i;
                }
            }

            units[best]++;
            fractions[best] = -1;
        }

        return units;
    }

    public int nitrogen() {
        return 100 - oxygen - helium;
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
            com.diggydwarff.deepdiving.config.DivingConfig.MAX_OXYGEN.get() / oxygenFraction()
        );
    }

    public double minimumDepth(com.diggydwarff.deepdiving.config.PressureProfile profile) {
        return profile.depthAtPressure(
            com.diggydwarff.deepdiving.config.DivingConfig.MIN_OXYGEN.get() / oxygenFraction()
        );
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
            Locale.ROOT,
            "O2 %d%% / N2 %d%% / He %d%%",
            oxygen,
            nitrogen(),
            helium
        );
    }
}
