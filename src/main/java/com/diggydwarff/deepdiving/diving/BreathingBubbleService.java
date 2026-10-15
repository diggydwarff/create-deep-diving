package com.diggydwarff.deepdiving.diving;

import com.diggydwarff.deepdiving.compat.DeepSeasCompatibility;
import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.equipment.BreathingEquipment;
import com.diggydwarff.deepdiving.network.BreathingPulse;
import com.diggydwarff.deepdiving.presentation.BreathingCycle;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.WeakHashMap;

/** Share cosmetic breath phases. Clients render both the audio and the matching exhaust. */
public final class BreathingBubbleService {
    private static final Map<ServerPlayer, Presentation> breathing = new WeakHashMap<>();

    private static final class Presentation {
        final BreathingCycle cycle = new BreathingCycle();

        int profile;

        long lastTick;

        ResourceKey<Level> dimension;
    }

    private static BreathingEquipment.Supply activeSupply(ServerPlayer player) {
        if (!DiverService.active(player)
            || !player.isAlive()
            || !player.isEyeInFluid(FluidTags.WATER)
            || DeepSeasCompatibility.inDryCompartment(player)) {
            return null;
        }

        if (DivingConfig.WATER_BREATHING_BYPASS.get()
            && (player.hasEffect(MobEffects.WATER_BREATHING)
                || player.hasEffect(MobEffects.CONDUIT_POWER))) {
            return null;
        }

        var supply = BreathingEquipment.select(player);

        return supply != null && supply.available() ? supply : null;
    }

    public static boolean canExhale(ServerPlayer player) {
        return DivingConfig.BREATHING_BUBBLES.get() && activeSupply(player) != null;
    }

    private static void restart(ServerPlayer player, Presentation presentation) {
        // Read existing movement and physiology; there is no second breathing simulation.
        double effort = Math.clamp(player.getDeltaMovement().length() / .18, 0, 1);

        if (player.isSprinting()) {
            effort = 1;
        }

        presentation.cycle.restart(effort, DiverService.state(player).oxygenation);
    }

    private static BreathingPulse pulse(ServerPlayer player, Presentation presentation) {
        return new BreathingPulse(
            player.getId(),
            player.getUUID(),
            presentation.profile,
            presentation.cycle.phase(),
            presentation.cycle.remainingTicks(),
            DivingConfig.BREATHING_BUBBLES.get()
        );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var supply = activeSupply(player);

        if (supply == null) {
            if (breathing.remove(player) != null) {
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                    player,
                    new BreathingPulse(player.getId(), player.getUUID(), 0, BreathingCycle.STOPPED, 0, false)
                );
            }

            return;
        }

        int profile = supply.create() ? BreathingPulse.CREATE_HELMET : BreathingPulse.REGULATOR;

        Presentation presentation = breathing.get(player);

        // A new equipment profile must not resume an old breath.
        if (presentation == null || presentation.profile != profile || presentation.dimension != player.level().dimension()
            || presentation.lastTick != player.level().getGameTime() - 1) {
            presentation = new Presentation();
            presentation.profile = profile;
            presentation.dimension = player.level().dimension();
            restart(player, presentation);
            breathing.put(player, presentation);
        }

        presentation.lastTick = player.level().getGameTime();

        if (presentation.cycle.startsPhase()) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, pulse(player, presentation));
        }

        if (presentation.cycle.advance()) {
            restart(player, presentation);
        }
    }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer observer
            && event.getTarget() instanceof ServerPlayer diver) {
            Presentation presentation = breathing.get(diver);

            if (presentation != null && activeSupply(diver) != null) {
                PacketDistributor.sendToPlayer(observer, pulse(diver, presentation));
            }
        }
    }

    private BreathingBubbleService() {}
}
