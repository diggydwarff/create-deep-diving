package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.compat.DeepSeasCompatibility;
import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.presentation.OrbitControls;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/**
 * Fourth underwater perspective; the vanilla camera still handles eye interpolation and collision.
 */
@EventBusSubscriber(
    modid = DeepDiving.MOD_ID,
    value = Dist.CLIENT
)
public final class OrbitCamera {
    private static final OrbitControls controls = new OrbitControls();

    private static LocalPlayer owner;

    private static ClientLevel level;

    private static boolean eligible() {
        var mc = Minecraft.getInstance();

        return DivingConfig.ENABLED.get()
            && DivingConfig.ORBIT_CAMERA.get()
            && mc.player != null
            && mc.level != null
            && mc.getCameraEntity() == mc.player
            && mc.player.isAlive()
            && mc.player.isInWater()
            && !mc.player.isSpectator()
            && !mc.player.isPassenger()
            && !mc.player.isFallFlying()
            && !DeepSeasCompatibility.inDryCompartment(mc.player);
    }

    private static void refresh() {
        if (!controls.active()) {
            return;
        }

        var mc = Minecraft.getInstance();

        if (owner == mc.player
            && level == mc.level
            && eligible()
            && mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK) {
            return;
        }

        reset();
    }

    private static void reset() {
        var mc = Minecraft.getInstance();

        boolean restore = controls.active() && mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK;

        controls.reset();
        owner = null;
        level = null;

        if (restore) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.gameRenderer.checkEntityPostEffect(mc.getCameraEntity());
            mc.levelRenderer.needsUpdate();
        }
    }

    public static CameraType nextPerspective(CameraType ordinaryNext) {
        var mc = Minecraft.getInstance();

        var result = controls.next(
            mc.options.getCameraType(),
            ordinaryNext,
            CameraType.FIRST_PERSON,
            CameraType.THIRD_PERSON_BACK,
            eligible(),
            mc.player == null ? 0 : mc.player.getYRot(),
            mc.player == null ? 0 : mc.player.getXRot()
        );

        if (controls.active()) {
            owner = mc.player;
            level = mc.level;
            mc.player.displayClientMessage(
                Component.translatable(
                    "message.deepdiving.orbit_camera",
                    mc.options.keyTogglePerspective.getTranslatedKeyMessage()
                ),
                true
            );
        } else {
            owner = null;
            level = null;
        }

        return result;
    }

    public static boolean turn(double horizontal, double vertical) {
        refresh();

        if (!controls.active()) {
            return false;
        }

        controls.turn(horizontal, vertical);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void angles(ViewportEvent.ComputeCameraAngles event) {
        refresh();

        if (!controls.active() || event.getCamera().getEntity() != owner) {
            return;
        }

        float partial = (float) event.getPartialTick();

        // Add the independent angle offset while retaining other mods' roll/sway contributions.
        event.setYaw(event.getYaw() + controls.yaw() - owner.getViewYRot(partial));
        event.setPitch(event.getPitch() + controls.pitch() - owner.getViewXRot(partial));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void distance(CalculateDetachedCameraDistanceEvent event) {
        refresh();

        if (controls.active()
            && event.getCamera().getEntity() == owner
            && !event.isCameraFlipped()) {
            event.setDistance(controls.distance());
        }
    }

    @SubscribeEvent
    public static void scroll(InputEvent.MouseScrollingEvent event) {
        refresh();

        var mc = Minecraft.getInstance();

        if (controls.active() && mc.screen == null && mc.mouseHandler.isMouseGrabbed()) {
            controls.zoom(event.getScrollDeltaY());
            event.setCanceled(true); // Zoom does not switch the selected hotbar item.
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        refresh();
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    private OrbitCamera() {}
}
