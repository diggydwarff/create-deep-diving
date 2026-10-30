package com.diggydwarff.deepdiving.client.lighting;

import com.BreadRes.illuminationapi.client.ClientLightSource;
import com.diggydwarff.deepdiving.client.sediment.SedimentClouds;
import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.config.LampConfig;
import com.diggydwarff.deepdiving.equipment.DivingLampItem;
import com.diggydwarff.deepdiving.equipment.LampProfile;
import com.diggydwarff.deepdiving.network.LampState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds client lights from authoritative equipment state, without registering a second API pass.
 */
public final class DivingLampLights {
    public record Frame(Player owner, ClientLightSource source) {}

    private static final Map<UUID, Entry> STATES = new HashMap<>();

    private static ClientLevel world;

    private static long lastFrame;

    private static List<Frame> visible = List.of();

    private static void bind(ClientLevel level) {
        if (world != level) {
            reset();
            world = level;
        }
    }

    public static void receive(LampState state) {
        ClientLevel level = Minecraft.getInstance().level;

        bind(level);

        if (level == null || !level.dimension().location().equals(state.dimension())) {
            return;
        }

        Entry entry = STATES.computeIfAbsent(state.diver(), ignored -> new Entry(state.held()));

        entry.active = state.active();
        entry.held = state.held();
        entry.profile = LampProfile.byId(state.profile());
        entry.expires = level.getGameTime() + 60;
    }

    public static List<Frame> collect(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();

        bind(minecraft.level);

        if (world == null
                || minecraft.player == null
                || !DivingConfig.CLIENT.isLoaded()
                || !LampConfig.ENABLED.get()
                || LampConfig.BRIGHTNESS.get() == 0) {
            visible = List.of();
            return visible;
        }

        long now = System.nanoTime();

        double seconds = lastFrame == 0 ? .016 : Math.clamp((now - lastFrame) / 1e9, 0, .1);

        lastFrame = now;

        double blend = 1 - Math.exp(-seconds * 4 / LampConfig.TRANSITION.get());

        STATES.values()
                .forEach(
                        entry -> {
                            boolean active = entry.active && entry.expires >= world.getGameTime();

                            entry.weight += ((active ? 1 : 0) - entry.weight) * blend;
                            entry.heldWeight += ((entry.held ? 1 : 0) - entry.heldWeight) * blend;
                        });
        STATES.entrySet()
                .removeIf(
                        entry ->
                                entry.getValue().expires < world.getGameTime()
                                        && entry.getValue().weight < .001);

        List<Player> candidates = new ArrayList<>();

        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();

        for (Player diver : world.players()) {
            Entry state = STATES.get(diver.getUUID());

            if (state == null || state.weight < .001 || !diver.isAlive() || diver.isSpectator()) {
                continue;
            }

            if (diver == minecraft.player
                    || LampConfig.NEARBY.get()
                            && LampConfig.NEARBY_LIMIT.get() > 0
                            && diver.distanceToSqr(camera) < 24 * 24) {
                candidates.add(diver);
            }
        }
        candidates.sort(
                Comparator.comparingDouble(
                        diver -> diver == minecraft.player ? -1 : diver.distanceToSqr(camera)));

        int limit =
                LampConfig.REDUCED.get()
                        ? 1
                        : Math.min(
                                LampConfig.SHADOW_LIMIT.get(),
                                1 + (LampConfig.NEARBY.get() ? LampConfig.NEARBY_LIMIT.get() : 0));

        List<Frame> frames = new ArrayList<>();

        int nearby = 0;

        for (Player diver : candidates) {
            if (frames.size() >= limit) {
                break;
            }

            if (diver != minecraft.player && nearby >= LampConfig.NEARBY_LIMIT.get()) {
                continue;
            }

            Frame frame = frame(diver, STATES.get(diver.getUUID()), partialTick);

            if (frame != null) {
                frames.add(frame);

                if (diver != minecraft.player) {
                    nearby++;
                }
            }
        }
        visible = List.copyOf(frames);
        return visible;
    }

    private static Frame frame(Player diver, Entry state, float partialTick) {
        double held = state.heldWeight;

        Vec3 eye = diver.getEyePosition(partialTick);

        // Spherical rotation also handles looking straight up while moving a lamp
        // into/out of the hand; linear vector blending would flip at its midpoint.
        Quaternionf rotation =
                new Quaternionf()
                        .rotationTo(
                                new Vector3f(0, -1, 0),
                                diver.getViewVector(partialTick).toVector3f());

        Vector3f aim =
                new Vector3f(0, -1, 0).rotate(new Quaternionf().slerp(rotation, (float) held));

        Vec3 direction = new Vec3(aim).normalize();

        Vec3 carried = diver.getPosition(partialTick).add(0, diver.getBbHeight() * .55, 0);

        Vec3 hand = heldOrigin(diver, eye, partialTick);

        Vec3 origin = carried.lerp(hand, held);

        var hit =
                world.clip(
                        new ClipContext(
                                eye,
                                origin,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                diver));

        if (hit.getType() != HitResult.Type.MISS) {
            // Pull the emitter back to our side of the wall, without letting it
            // cross the collision surface or snapping it into the player's face.
            Vec3 offset = origin.subtract(eye);

            double distance = Math.max(0, eye.distanceTo(hit.getLocation()) - .06);

            origin = eye.add(offset.normalize().scale(distance));
        }

        double range = 4 + (LampConfig.RANGE.get() * state.profile.rangeScale - 4) * held;

        if (LampConfig.REDUCED.get()) {
            range = Math.min(range, 12);
        }

        // The light comes from the hand but converges on the view's target.
        // A parallel hand beam would miss the crosshair at short distances.
        Vec3 target = eye.add(diver.getViewVector(partialTick).scale(range));

        var aimHit =
                world.clip(
                        new ClipContext(
                                eye,
                                target,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                diver));

        if (aimHit.getType() != HitResult.Type.MISS) {
            target = aimHit.getLocation();
        }

        direction = direction.lerp(target.subtract(origin).normalize(), held).normalize();

        // Sample the existing transient sediment field; no second cloud simulation.
        double sediment = 0;

        for (int sample = 0; sample < 4; sample++) {
            sediment += SedimentClouds.densityAt(origin.add(direction.scale(range * sample / 4)));
        }
        range *= Math.exp(-sediment * .10);

        double halfAngle =
                state.profile.halfAngle < 0 ? LampConfig.WIDTH.get() * .5 : state.profile.halfAngle;
        float width = (float) (72 + (halfAngle - 72) * held);

        Vector3f forward = direction.toVector3f();

        Vector3f right = new Vector3f(forward).cross(0, 1, 0);

        if (right.lengthSquared() < .0001f) {
            right.set(1, 0, 0);
        }
        right.normalize();

        Vector3f up = new Vector3f(right).cross(forward).normalize();

        float brightness =
                (float)
                        (LampConfig.BRIGHTNESS.get()
                                * state.weight
                                * (.075 + .265 * held)
                                * state.profile.intensity);

        float volume =
                LampConfig.REDUCED.get()
                        ? 0
                        : (float) (LampConfig.VOLUME.get() * .006 * state.weight);

        ClientLightSource source =
                new ClientLightSource(
                        state.key,
                        world,
                        null,
                        origin,
                        origin,
                        origin.add(direction.scale(.03)),
                        forward,
                        right,
                        up,
                        (float) range,
                        .03f,
                        .035f,
                        width,
                        width * .60f,
                        LampConfig.color(),
                        brightness,
                        volume,
                        true,
                        true,
                        "Deep Diving lamp",
                        null);

        return new Frame(diver, source);
    }

    private static Vec3 heldOrigin(Player diver, Vec3 eye, float partialTick) {
        boolean offHand =
                !(diver.getMainHandItem().getItem() instanceof DivingLampItem)
                        && diver.getOffhandItem().getItem() instanceof DivingLampItem;

        HumanoidArm arm = offHand ? diver.getMainArm().getOpposite() : diver.getMainArm();

        Vec3 forward = diver.getViewVector(partialTick).normalize();

        double yaw = Math.toRadians(diver.getViewYRot(partialTick));

        Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));

        Vec3 up = right.cross(forward).normalize();

        double side = arm == HumanoidArm.RIGHT ? .32 : -.32;

        // Keep the emitter below and to the side of the face. Swimming extends
        // the arms forward, so the lamp moves forward with that pose.
        double lower = diver.isVisuallySwimming() ? .12 : .40;

        double reach = diver.isVisuallySwimming() ? .48 : .30;

        return eye.add(right.scale(side)).add(up.scale(-lower)).add(forward.scale(reach));
    }

    /** Local particle light only; does not alter the world light map or sunlight. */
    public static int particleLight(double x, double y, double z, int original) {
        if (world == null
                || visible.isEmpty()
                || !LampRenderer.available()
                || LampShaderPipeline.active() && !LampConfig.SHADERS.get()) {
            return original;
        }

        Vec3 point = new Vec3(x, y, z);

        double illumination = 0;

        for (Frame frame : visible) {
            ClientLightSource source = frame.source;

            Vec3 relative = point.subtract(source.lightPos());

            double axial = relative.dot(new Vec3(source.forward()));

            if (axial <= 0 || axial >= source.range()) {
                continue;
            }

            double radial = Math.sqrt(Math.max(0, relative.lengthSqr() - axial * axial));

            double radius = axial * Math.tan(Math.toRadians(source.outerAngle()));

            if (radial >= radius
                    || world.clip(
                                            new ClipContext(
                                                    source.lightPos(),
                                                    point,
                                                    ClipContext.Block.COLLIDER,
                                                    ClipContext.Fluid.NONE,
                                                    frame.owner))
                                    .getType()
                            != HitResult.Type.MISS) {
                continue;
            }

            illumination =
                    Math.max(
                            illumination,
                            source.surfaceIntensity()
                                    * 5
                                    * Math.pow(1 - radial / radius, 2)
                                    * (1 - axial / source.range()));
        }

        int block = Math.min(9, (int) Math.ceil(illumination * 9));

        return (original & ~0xFFFF) | Math.max(original & 0xFFFF, block * 16);
    }

    public static void reset() {
        STATES.clear();
        visible = List.of();
        lastFrame = 0;
        world = null;
        LampRenderer.reset();
    }

    private static final class Entry {
        private final Object key = new Object();

        private boolean active;

        private boolean held;
        private LampProfile profile = LampProfile.DIVING;

        private long expires;

        private double weight;

        private double heldWeight;

        private Entry(boolean held) {
            // Start at the real carry position, avoiding a forward flash from a stowed lamp.
            heldWeight = held ? 1 : 0;
        }
    }

    private DivingLampLights() {}
}
