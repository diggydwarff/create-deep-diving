package com.diggydwarff.deepdiving.gas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Remaining gas is measured at one atmosphere, independent of the diver's depth. */
public record CylinderSupply(GasMix mix, double remaining, boolean backupEnabled) {
    public static final CylinderSupply EMPTY = new CylinderSupply(GasMix.AIR, 0, false);

    public static final Codec<CylinderSupply> CODEC = RecordCodecBuilder.create(
        i -> i.group(
            GasMix.CODEC.fieldOf("mix")
                .forGetter(CylinderSupply::mix),
            Codec.doubleRange(0, 1000000)
                .fieldOf("remaining")
                .forGetter(CylinderSupply::remaining),
            Codec.BOOL.optionalFieldOf("backup_enabled", false)
                .forGetter(CylinderSupply::backupEnabled)
        )
            .apply(i, CylinderSupply::new)
    );

    public CylinderSupply {
        if (mix == null || !Double.isFinite(remaining) || remaining < 0 || remaining > 1000000) {
            throw new IllegalArgumentException("Invalid gas quantity");
        }
    }

    public CylinderSupply consume(double amount) {
        return new CylinderSupply(mix, Math.max(0, remaining - Math.max(0, amount)), backupEnabled);
    }
}
