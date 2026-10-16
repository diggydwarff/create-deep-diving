package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.diving.DiverService;
import com.diggydwarff.deepdiving.network.DivingPackets;
import com.diggydwarff.deepdiving.physiology.BuoyancyMotion;
import com.diggydwarff.deepdiving.physiology.DepthHoldMotion;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.tags.FluidTags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import org.lwjgl.glfw.GLFW;

/** Predict the server-approved buoyancy correction locally for smooth multiplayer movement. */
@EventBusSubscriber(
    modid = DeepDiving.MOD_ID,
    value = Dist.CLIENT
)
public final class DepthHoldClient {
    private static final KeyMapping HOLD = new KeyMapping(
        "key.deepdiving.depth_hold",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V,
        "key.categories.deepdiving"
    );

    private static boolean releasePending;

    private static ClientLevel previousLevel;

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(HOLD);
    }

    public static boolean isHolding() {
        return ClientRegistration.snapshot != null
            && ClientRegistration.snapshot.depthHold()
            && !releasePending;
    }

    static String help() {
        return "Press "
                + HOLD.getTranslatedKeyMessage().getString()
                + " to hold your current depth with diving gear and a computer."
                + " Press again, swim up or sneak to release. Horizontal swimming remains available.";
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();

        if (mc.level != previousLevel) {
            previousLevel = mc.level;
            releasePending = false;
            ClientRegistration.snapshot = null;
        }

        boolean pressed = false;

        while (HOLD.consumeClick()) {
            pressed = true;
        }

        if (mc.player == null || mc.level == null) {
            return;
        }

        var state = ClientRegistration.snapshot;

        if (state != null && !state.depthHold()) {
            releasePending = false;
        }

        if (pressed && mc.screen == null) {
            boolean enabled = state == null || !state.depthHold() || releasePending;

            PacketDistributor.sendToServer(new DivingPackets.DepthHoldRequest(enabled));

            if (!enabled) {
                releasePending = true;
            }
        }

        if (state == null || !state.depthHold() || releasePending) {
            return;
        }

        boolean manualVertical = mc.screen == null && (mc.options.keyJump.isDown() || mc.options.keyShift.isDown());

        if (manualVertical
            || !mc.player.isAlive()
            || !DiverService.active(mc.player)
            || mc.player.getAbilities().flying
            || mc.player.isPassenger()
            || mc.player.isFallFlying()
            || !mc.player.isEyeInFluid(FluidTags.WATER)
            || Math.abs(mc.player.getDeltaMovement().y) > .3) {
            releasePending = true;
            PacketDistributor.sendToServer(new DivingPackets.DepthHoldRequest(false));
            return;
        }

        var motion = mc.player.getDeltaMovement();

        motion = BuoyancyClient.horizontal(motion, 0);
        mc.player.setDeltaMovement(
            motion.x,
            DepthHoldMotion.vertical(
                mc.player.getY(),
                BuoyancyMotion.bobTarget(
                    state.holdTargetY(),
                    mc.level.getGameTime(),
                    state.heldDepth() - state.ceiling()
                ),
                state.safeAscent()
            ),
            motion.z
        );
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        previousLevel = null;
        releasePending = false;
        ClientRegistration.snapshot = null;
    }

    private DepthHoldClient() {}
}
