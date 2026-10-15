package com.diggydwarff.deepdiving.compat;

import com.diggydwarff.deepdiving.config.DivingConfig;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

/** Optional public compartment query, isolated from Sable and Deep Seas classloading. */
public final class DeepSeasCompatibility {
    private static Method sealed;

    public static void discover() {
        if (!ModList.get().isLoaded("create_submarine")) {
            return;
        }

        try {
            var tracker = Class.forName("com.maxenonyme.createsubmarine.submarine.compartment.CompartmentTracker");

            sealed = tracker.getMethod("isInSealedExact", Level.class, Vec3.class);
        } catch (ReflectiveOperationException | LinkageError failure) {
            com.mojang.logging.LogUtils.getLogger()
                .warn("Deep Diving: Deep Seas compartment API unavailable; using ordinary water detection.", failure);
        }
    }

    public static boolean inDryCompartment(Player player) {
        return inDryCompartment(player.level(), player.getEyePosition());
    }

    /** Audio sources can sit inside the same dry compartment as the listener. */
    public static boolean inDryCompartment(Level level, Vec3 position) {
        if (sealed == null || !DivingConfig.DEEP_SEAS_COMPARTMENTS.get()) {
            return false;
        }

        try {
            return Boolean.TRUE.equals(sealed.invoke(null, level, position));
        } catch (ReflectiveOperationException | LinkageError failure) {
            sealed = null;
            com.mojang.logging.LogUtils.getLogger()
                .warn("Deep Diving: disabled unavailable Deep Seas compartment query.", failure);
            return false;
        }
    }

    private DeepSeasCompatibility() {}
}
