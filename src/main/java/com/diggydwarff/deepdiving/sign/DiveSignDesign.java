package com.diggydwarff.deepdiving.sign;

import com.mojang.serialization.Codec;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** Catalog identifiers are checked against the editable artwork manifest. */
public enum DiveSignDesign implements StringRepresentable {
    BLANK,
    DIVE_SITE,
    DIVER_DOWN,
    CAVE_ENTRANCE,
    WRECK,
    TRAINING_POOL,
    SNORKELING_AREA,
    SWIMMING_AREA,
    SKULL_AND_CROSSBONES,
    GRIM_REAPER,
    OVERHEAD_ENVIRONMENT,
    TIGHT_RESTRICTION,
    STRONG_CURRENT,
    POOR_VISIBILITY,
    DO_NOT_ENTER,
    NO_SWIMMING,
    WATER_ENTRY,
    EXIT_SURFACE,
    DECOMPRESSION_STOP,
    GAS_FILLING,
    EQUIPMENT_AREA,
    BUOYANCY_PRACTICE,
    BUDDY_CHECK,
    GUIDELINE_PRACTICE,
    MAXIMUM_DEPTH;

    public static final Codec<DiveSignDesign> CODEC =
            StringRepresentable.fromEnum(DiveSignDesign::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String registryName() {
        return this == BLANK ? "blank_dive_sign" : "dive_sign_" + getSerializedName();
    }

    public Component defaultCaption() {
        return Component.translatable("sign.deepdiving." + getSerializedName());
    }

    /** Retired registrations remain loadable, but do not clutter crafting or the creative tab. */
    public boolean available() {
        return switch (this) {
            case BLANK,
                    DIVE_SITE,
                    DIVER_DOWN,
                    CAVE_ENTRANCE,
                    WRECK,
                    TRAINING_POOL,
                    GRIM_REAPER,
                    STRONG_CURRENT,
                    DO_NOT_ENTER,
                    EXIT_SURFACE,
                    GAS_FILLING ->
                    true;
            default -> false;
        };
    }
}
