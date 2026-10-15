package com.diggydwarff.deepdiving.diving;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.equipment.BreathingEquipment;
import com.diggydwarff.deepdiving.physiology.DecompressionStops;
import com.diggydwarff.deepdiving.physiology.DepthHoldMotion;
import com.diggydwarff.deepdiving.physiology.DiverState;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** The client may request a toggle; only the server chooses and validates the held depth. */
public final class DepthHoldService {
    public static boolean eligible(ServerPlayer player, DiverState state) {
        if (!DivingConfig.DEPTH_HOLD.get()
            || !player.isAlive()
            || !DiverService.active(player)
            || player.getAbilities().flying
            || player.isPassenger()
            || player.isFallFlying()
            || state.blackedOut
            || !DiverService.manages(player)
            || !BreathingEquipment.carries(player, DeepDiving.COMPUTER.get())) {
            return false;
        }

        var supply = BreathingEquipment.select(player);

        return supply != null && supply.available();
    }

    public static void request(ServerPlayer player, boolean enabled) {
        DiverState state = DiverService.state(player);

        if (!enabled) {
            if (!state.depthHold) {
                return;
            }

            clear(state); // Releasing must work immediately, including during the toggle cooldown.
            DiverService.send(player, state);
            return;
        }

        long now = player.serverLevel().getGameTime();

        if (state.depthHold || now < state.lastHoldRequest + 8) {
            return;
        }

        state.lastHoldRequest = now;

        if (!eligible(player, state)) {
            player.displayClientMessage(Component.translatable("message.deepdiving.hold_unavailable"), true);
            return;
        }

        var profile = DivingConfig.pressureProfile(player.level().dimension().location());

        double depth = DepthSampler.depth(player);

        double stop = DivingConfig.PHYSIOLOGY.get()
                ? DecompressionStops.depth(state.tissues, profile)
                : 0;

        if (depth + .05 < stop) {
            player.displayClientMessage(Component.translatable("message.deepdiving.hold_below_stop", (int) stop), true);
            return;
        }

        state.depthHold = true;
        state.heldDepth = depth;
        state.holdTargetY = player.getY();
        state.holdDimension = player.level().dimension();
        state.holdProfile = profile;
        state.holdPreviousPosition = player.position();
        state.depth = depth;
        state.pressure = profile.pressure(depth);
        state.underwater = true;
        player.displayClientMessage(Component.translatable("message.deepdiving.hold_enabled"), true);
        DiverService.send(player, state);
    }

    /** Called every tick, and before the once-per-second computer snapshot is published. */
    public static void update(ServerPlayer player, DiverState state) {
        if (!state.depthHold) {
            return;
        }

        var profile = DivingConfig.pressureProfile(player.level().dimension().location());

        if (!eligible(player, state)
            || state.holdDimension != player.level().dimension()
            || !profile.equals(state.holdProfile)
            || player.isShiftKeyDown()
            || state.holdPreviousPosition == null
            || player.position().distanceToSqr(state.holdPreviousPosition) > 64
            || Math.abs(player.getDeltaMovement().y) > .3) {
            clear(state);
            return;
        }

        double currentDepth = DepthSampler.depth(player);

        double stop = DivingConfig.PHYSIOLOGY.get()
                ? DecompressionStops.depth(state.tissues, profile)
                : 0;

        // A newly increased ceiling may move the target deeper, never upward through a stop.
        state.heldDepth = Math.max(state.heldDepth, stop);
        state.holdTargetY = player.getY() + currentDepth - state.heldDepth;
        state.holdPreviousPosition = player.position();

        var motion = player.getDeltaMovement();

        player.setDeltaMovement(
            motion.x,
            DepthHoldMotion.vertical(
                player.getY(),
                com.diggydwarff.deepdiving.physiology.BuoyancyMotion.bobTarget(
                    state.holdTargetY,
                    player.level().getGameTime(),
                    state.heldDepth - stop
                ),
                DivingConfig.SAFE_ASCENT.get()
            ),
            motion.z
        );
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var state = DiverService.state(player);

        boolean held = state.depthHold;

        double target = state.holdTargetY;

        update(player, state);

        if (held && (!state.depthHold || Math.abs(target - state.holdTargetY) > .05)) {
            DiverService.send(player, state);
        }
    }

    public static void clear(DiverState state) {
        state.depthHold = false;
        state.holdTargetY = 0;
        state.heldDepth = 0;
        state.holdDimension = null;
        state.holdProfile = null;
        state.holdPreviousPosition = null;
    }

    private DepthHoldService() {}
}
