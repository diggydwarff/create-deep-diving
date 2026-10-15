package com.diggydwarff.deepdiving.diving;

import com.diggydwarff.deepdiving.compat.DeepSeasCompatibility;
import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.equipment.BreathingEquipment;
import com.diggydwarff.deepdiving.presentation.Exhalation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Cosmetic exhaust reads the current supply; it never consumes gas or changes breathing. */
public final class BreathingBubbleService {
    public static boolean canExhale(ServerPlayer player) {
        if (!DivingConfig.ENABLED.get()
            || !DivingConfig.BREATHING_BUBBLES.get()
            || !player.isAlive()
            || player.isSpectator()
            || !player.isEyeInFluid(FluidTags.WATER)
            || DeepSeasCompatibility.inDryCompartment(player)) {
            return false;
        }

        if (DivingConfig.WATER_BREATHING_BYPASS.get()
            && (player.hasEffect(MobEffects.WATER_BREATHING)
                || player.hasEffect(MobEffects.CONDUIT_POWER))) {
            return false;
        }

        var supply = BreathingEquipment.select(player);

        return supply != null && supply.available();
    }

    private static boolean water(ServerPlayer player, Vec3 position) {
        BlockPos pos = BlockPos.containing(position);

        return player.serverLevel().hasChunkAt(pos)
            && player.level().getFluidState(pos).is(FluidTags.WATER);
    }

    public static int emit(ServerPlayer player, long tick) {
        int count = Exhalation.particles(tick, player.getUUID().hashCode());

        if (count == 0 || !canExhale(player)) {
            return 0;
        }

        Vec3 eye = player.getEyePosition();

        Vec3 origin = eye.add(player.getLookAngle().scale(.20)).add(0, -.10, 0);

        if (!water(player, origin)) {
            origin = eye.add(0, -.06, 0);
        }

        if (!water(player, origin)) {
            return 0;
        }

        // Vanilla bubbles rise and remain in world space, leaving a trail as the diver moves away.
        player.serverLevel()
            .sendParticles(
                ParticleTypes.BUBBLE,
                origin.x,
                origin.y,
                origin.z,
                count,
                .045,
                .025,
                .045,
                .035
            );
        return count;
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            emit(player, player.tickCount);
        }
    }

    private BreathingBubbleService() {}
}
