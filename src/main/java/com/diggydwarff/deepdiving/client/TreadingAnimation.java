package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.diving.DiverService;
import com.diggydwarff.deepdiving.network.DivingPackets;
import com.diggydwarff.deepdiving.physiology.TreadingMotion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Ease idle buoyancy into a small flutter. Vanilla travel, attacks and held items keep their poses.
 */
@EventBusSubscriber(
    modid = DeepDiving.MOD_ID,
    value = Dist.CLIENT
)
public final class TreadingAnimation {
    private static final Map<Integer, Pose> poses = new HashMap<>();

    private static ClientLevel previousLevel;

    private static final class Pose {
        boolean active;

        Boolean swimming;

        float previous;

        float current;
    }

    private static void level() {
        var current = Minecraft.getInstance().level;

        if (current != previousLevel) {
            poses.clear();
            previousLevel = current;
        }
    }

    public static void receive(DivingPackets.TreadingPose packet) {
        level();

        if (previousLevel == null) {
            return;
        }

        Pose pose = poses.computeIfAbsent(packet.entityId(), id -> new Pose());

        pose.active = packet.active();
        pose.swimming = packet.finPoseControlled() ? packet.finSwimming() : null;
    }

    public static Boolean swimmingPose(net.minecraft.world.entity.player.Player player) {
        level();

        Pose pose = poses.get(player.getId());

        return pose != null ? pose.swimming : null;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        level();

        var mc = Minecraft.getInstance();

        if (mc.level == null) {
            return;
        }

        for (var player : mc.level.players()) {
            Pose pose = poses.computeIfAbsent(player.getId(), id -> new Pose());

            boolean active = pose.active;

            double speed = Math.hypot(player.getX() - player.xo, player.getZ() - player.zo);

            if (player == mc.player) {
                var state = ClientRegistration.snapshot;

                active = state != null
                    && (DepthHoldClient.isHolding() || state.finsFloating())
                    && (state.warnings() & DiverService.BLACKOUT) == 0;

                if (mc.screen == null
                    && (mc.options.keyJump.isDown()
                        || mc.options.keyShift.isDown()
                        || Math.abs(mc.player.input.forwardImpulse) > .01
                        || Math.abs(mc.player.input.leftImpulse) > .01)) {
                    active = false;
                }
            }

            active &= player.isAlive()
                && player.isInWater()
                && !player.isPassenger()
                && !player.isFallFlying()
                && !player.isSpectator();
            pose.previous = pose.current;
            pose.current = TreadingMotion.approach(pose.current, active ? TreadingMotion.restWeight(speed) : 0);
        }

        // Remove departed players while leaving newly announced tracking poses time to spawn.
        if (mc.level.getGameTime() % 100 == 0) {
            poses.keySet().removeIf(id -> mc.level.getEntity(id) == null);
        }
    }

    public static void apply(HumanoidModel<?> model, AbstractClientPlayer player, float age) {
        Pose pose = poses.get(player.getId());

        if (pose == null) {
            return;
        }

        float strength = Mth.lerp(Mth.clamp(age - player.tickCount, 0, 1), pose.previous, pose.current);

        if (strength <= 0) {
            return;
        }

        var stroke = TreadingMotion.stroke(age + (player.getUUID().hashCode() & 63));

        model.leftLeg.xRot = Mth.lerp(strength, model.leftLeg.xRot, stroke.kick());
        model.rightLeg.xRot = Mth.lerp(strength, model.rightLeg.xRot, -stroke.kick());
        model.leftLeg.zRot = Mth.lerp(strength, model.leftLeg.zRot, .035f);
        model.rightLeg.zRot = Mth.lerp(strength, model.rightLeg.zRot, -.035f);

        if (player.isUsingItem() || player.swinging || model.attackTime > 0) {
            return;
        }

        boolean leftMain = player.getMainArm() == HumanoidArm.LEFT;

        if ((leftMain ? player.getMainHandItem() : player.getOffhandItem()).isEmpty()
            && model.leftArmPose == HumanoidModel.ArmPose.EMPTY) {
            arm(model.leftArm, strength, player.isSwimming(), stroke, 1);
        }

        if ((leftMain ? player.getOffhandItem() : player.getMainHandItem()).isEmpty()
            && model.rightArmPose == HumanoidModel.ArmPose.EMPTY) {
            arm(model.rightArm, strength, player.isSwimming(), stroke, -1);
        }
    }

    private static void arm(
        ModelPart arm,
        float strength,
        boolean swimming,
        TreadingMotion.Stroke stroke,
        int side
    ) {
        arm.xRot = Mth.lerp(strength, arm.xRot, (swimming ? .1f : -.4f) + stroke.sweep() * side);
        arm.yRot = Mth.lerp(strength, arm.yRot, stroke.sweep() * side);
        arm.zRot = Mth.lerp(strength, arm.zRot, stroke.spread() * side);
    }

    private TreadingAnimation() {}
}
