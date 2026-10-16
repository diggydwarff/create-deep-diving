package com.diggydwarff.deepdiving.diving;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.compat.DeepSeasCompatibility;
import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.physiology.*;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.function.Function;

public final class FinsService {
    public static Function<Player, Boolean> clientPose = player -> null;

    private static final ResourceLocation SWIM = DeepDiving.id("fins_swim");

    private static final ResourceLocation WALK = DeepDiving.id("fins_walk");

    public static boolean worn(Player player) {
        return player.getItemBySlot(EquipmentSlot.FEET).is(DeepDiving.FINS.get());
    }

    /** Fins remain usable without a helmet, tank, or active diving simulation. */
    public static boolean eligible(Player player) {
        return worn(player)
            && !player.isSpectator()
            && player.isAlive()
            && !player.getAbilities().flying
            && !player.isPassenger()
            && !player.isFallFlying()
            && player.isInWater()
            && !DeepSeasCompatibility.inDryCompartment(player);
    }

    public static Boolean swimmingOverride(Player player) {
        if (!eligible(player)) {
            return null;
        }

        if (player.level().isClientSide) {
            return clientPose.apply(player);
        }

        DiverState state = DiverService.state(player);

        boolean blackedOut = DiverService.active(player) && state.blackedOut;

        return !blackedOut && state.finPoseControlled ? state.finSwimming : null;
    }

    /** Client input describes intent; eligibility and the held depth remain server-owned. */
    public static void control(
        ServerPlayer player,
        boolean swimming,
        boolean vertical,
        boolean moving
    ) {
        DiverState state = DiverService.state(player);

        if (!eligible(player)) {
            clear(state);
            DiverService.send(player, state);
            return;
        }

        state.finPoseControlled = true;
        state.finSwimming = swimming;
        state.finVertical = vertical;
        state.finMoving = moving;
        update(player, state);
        DiverService.send(player, state);
    }

    public static void clear(DiverState state) {
        state.finPoseControlled = false;
        state.finSwimming = false;
        state.finsFloating = false;
        state.finMoving = false;
        state.finVertical = false;
        state.finDepth = 0;
        state.finTargetY = 0;
        state.finDimension = null;
        state.finPreviousPosition = null;
    }

    private static void modifier(
        Player player,
        Holder<Attribute> attribute,
        ResourceLocation id,
        double amount,
        boolean active
    ) {
        var attributeInstance = player.getAttribute(attribute);

        if (attributeInstance == null) {
            return;
        }

        if (active && !attributeInstance.hasModifier(id)) {
            attributeInstance.addTransientModifier(
                new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            );
        } else if (!active) {
            attributeInstance.removeModifier(id);
        }
    }

    public static void update(ServerPlayer player, DiverState state) {
        boolean diving = DiverService.active(player);

        // Equipment bonuses apply during travel as well as during stationary buoyancy.
        modifier(player, NeoForgeMod.SWIM_SPEED, SWIM, .08, worn(player) && player.isInWater());
        modifier(
            player,
            Attributes.MOVEMENT_SPEED,
            WALK,
            -.25,
            worn(player) && player.onGround() && !player.isSwimming()
        );

        if (!eligible(player)
            || diving && state.blackedOut
            || state.finDimension != null && state.finDimension != player.level().dimension()
            || state.finPreviousPosition != null
                && player.position().distanceToSqr(state.finPreviousPosition) > 64
            || Math.abs(player.getDeltaMovement().y) > .3) {
            clear(state);
            return;
        }

        if (state.finDimension == null) {
            state.finDimension = player.level().dimension();
        }

        // Manual movement and V hold take precedence over the fins' passive rest position.
        boolean floating = state.finPoseControlled
            && state.finSwimming
            && !state.finMoving
            && !state.finVertical
            && !(diving && state.depthHold);

        if (!floating) {
            state.finsFloating = false;
            state.finPreviousPosition = player.position();
            return;
        }

        double depth = DepthSampler.depth(player);

        if (!state.finsFloating) {
            state.finDepth = depth;
            state.finTargetY = player.getY();
        }

        state.finsFloating = true;
        state.finDimension = player.level().dimension();
        state.finPreviousPosition = player.position();

        double stop = diving && DivingConfig.PHYSIOLOGY.get()
                ? DecompressionStops.depth(
                    state.tissues,
                    DivingConfig.pressureProfile(player.level().dimension().location())
                )
                : 0;

        // A rising decompression ceiling can push the target deeper, never through a stop.
        double oldDepth = state.finDepth;

        state.finDepth = Math.max(state.finDepth, stop);

        if (player.isEyeInFluid(FluidTags.WATER)) {
            state.finTargetY = player.getY() + depth - state.finDepth;
        } else {
            // Keep the captured rest height while the diver's face is above the surface.
            state.finTargetY -= state.finDepth - oldDepth;
        }

        var motion = player.getDeltaMovement();

        player.setDeltaMovement(
            motion.x,
            DepthHoldMotion.vertical(
                player.getY(),
                BuoyancyMotion.bobTarget(state.finTargetY, player.level().getGameTime(), state.finDepth - stop),
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

        DiverState state = DiverService.state(player);

        boolean wasFloating = state.finsFloating;

        boolean wasSwimming = state.finSwimming;

        update(player, state);

        if (wasFloating != state.finsFloating || wasSwimming != state.finSwimming) {
            DiverService.send(player, state);
        }
    }

    private FinsService() {}
}
