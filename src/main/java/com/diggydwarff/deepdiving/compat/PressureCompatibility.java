package com.diggydwarff.deepdiving.compat;

import com.diggydwarff.deepdiving.config.DivingConfig;

import net.neoforged.fml.ModList;

/** Metadata-only detection; optional mods never become classloading dependencies. */
public final class PressureCompatibility {
    private static boolean crushDepthPresent;

    public static void discover() {
        crushDepthPresent = ModList.get().getMods().stream()
            .anyMatch(
                m -> m.getDisplayName()
                    .toLowerCase(java.util.Locale.ROOT)
                    .replaceAll("[^a-z]", "")
                    .contains("crushdepth")
            );
    }

    public static boolean suppressPressureDamage() {
        if (DivingConfig.AUTO_PRESSURE_COMPAT.get() && crushDepthPresent) {
            return true;
        }

        return DivingConfig.EXTERNAL_PRESSURE_OWNERS.get().stream()
            .anyMatch(ModList.get()::isLoaded);
    }

    private PressureCompatibility() {}
}
