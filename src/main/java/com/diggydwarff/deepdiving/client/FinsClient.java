package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.diving.DiverService;
import com.diggydwarff.deepdiving.diving.FinsService;
import com.diggydwarff.deepdiving.network.DivingPackets;
import com.diggydwarff.deepdiving.physiology.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(
    modid = DeepDiving.MOD_ID,
    value = Dist.CLIENT
)
public final class FinsClient {
    private static Player previousPlayer;

    private static ClientLevel previousLevel;

    private static Boolean swimming;

    private static int sentFlags = -1;

    public static Boolean pose(Player player) {
        return player == Minecraft.getInstance().player
                ? swimming
                : TreadingAnimation.swimmingPose(player);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();

        if (mc.player != previousPlayer || mc.level != previousLevel) {
            previousPlayer = mc.player;
            previousLevel = mc.level;
            swimming = null;
            sentFlags = -1;
        }

        if (mc.player == null
            || !FinsService.eligible(mc.player)
            || ClientRegistration.snapshot != null
                && (ClientRegistration.snapshot.warnings() & DiverService.BLACKOUT) != 0) {
            swimming = null;
            sentFlags = -1;
            return;
        }

        boolean moving = mc.screen == null
            && (Math.abs(mc.player.input.forwardImpulse) > .01
                || Math.abs(mc.player.input.leftImpulse) > .01);

        boolean rise = mc.screen == null && mc.options.keyJump.isDown();

        boolean sink = mc.screen == null && mc.options.keyShift.isDown();

        if (rise) {
            swimming = false;
        } else if (moving && mc.player.isSprinting()
            || swimming == null && mc.player.isSwimming()) {
            swimming = true;
        }

        if (swimming == null) {
            return;
        }

        // Pack these inputs only to detect changes; the server still receives the three booleans.
        int flags = (swimming ? 1 : 0) | (rise || sink ? 2 : 0) | (moving ? 4 : 0);

        if (flags != sentFlags) {
            sentFlags = flags;
            PacketDistributor.sendToServer(new DivingPackets.FinControl(swimming, rise || sink, moving));
        }

        var state = ClientRegistration.snapshot;

        if (state == null || !state.finsFloating() || state.depthHold() || moving || rise || sink) {
            return;
        }

        var motion = mc.player.getDeltaMovement();

        motion = BuoyancyClient.horizontal(motion, 1);
        mc.player.setDeltaMovement(
            motion.x,
            DepthHoldMotion.vertical(
                mc.player.getY(),
                BuoyancyMotion.bobTarget(state.finTargetY(), mc.level.getGameTime(), state.finDepth() - state.ceiling()),
                state.safeAscent()
            ),
            motion.z
        );
    }

    private FinsClient() {}
}
