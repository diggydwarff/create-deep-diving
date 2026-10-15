package com.diggydwarff.deepdiving.equipment;

import com.diggydwarff.deepdiving.DeepDiving;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** One small real light per diver, shared safely when two lamps occupy the same block. */
public final class DivingLampService {
    private record Lease(ServerLevel level, Map<BlockPos, Integer> points) {}

    private static final Map<UUID, Lease> LIGHTS = new HashMap<>();

    public static void update(ServerPlayer player) {
        boolean carried = BreathingEquipment.carries(player, DeepDiving.LAMP.get());

        if (carried || player.getPersistentData().getBoolean("deepdiving.lamp")) {
            clearLegacyVision(player);
        }

        if (!player.isAlive()
            || player.isSpectator()
            || !carried
            || !player.getPersistentData().getBoolean("deepdiving.lamp")) {
            release(player.getUUID());
            return;
        }

        ServerLevel level = player.serverLevel();

        Map<BlockPos, Integer> points = new HashMap<>();

        boolean held = player.getMainHandItem().is(DeepDiving.LAMP.get())
            || player.getOffhandItem().is(DeepDiving.LAMP.get());

        if (held) {
            Vec3 eye = player.getEyePosition();

            Vec3 direction = player.getLookAngle();

            double reach = loadedBeamReach(level, eye, direction);

            Vec3 end = eye.add(direction.scale(reach));

            var hit = level.clip(
                new ClipContext(
                    eye,
                    end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
                )
            );

            if (hit.getType() != HitResult.Type.MISS) {
                reach = Math.max(0, eye.distanceTo(hit.getLocation()) - .2);
            }

            // A narrow chain of light points projects forwards; solid walls stop the entire beam.
            for (int i = 0; i < 3; i++) {
                double distance = Math.min(3 + i * 4, reach);

                for (; distance >= .4; distance -= .25) {
                    BlockPos pos = BlockPos.containing(eye.add(direction.scale(distance)));

                    if (usable(level, pos)) {
                        points.merge(pos.immutable(), 9 + i, Math::max);
                        break;
                    }
                }
            }
        } else {
            BlockPos center = BlockPos.containing(player.getEyePosition());

            if (usable(level, center)) {
                points.put(center, 10);
            } else if (usable(level, player.blockPosition())) {
                points.put(player.blockPosition(), 10);
            }
        }

        Lease previous = LIGHTS.get(player.getUUID());

        if (points.isEmpty()) {
            release(player.getUUID());
            return;
        }

        if (previous != null && previous.level == level && previous.points.equals(points)) {
            return;
        }

        LIGHTS.put(player.getUUID(), new Lease(level, Map.copyOf(points)));

        if (previous != null) {
            for (BlockPos pos : previous.points.keySet()) {
                refresh(previous.level, pos);
            }
        }

        for (BlockPos pos : points.keySet()) {
            refresh(level, pos);
        }
    }

    private static boolean usable(ServerLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) {
            return false;
        }

        var state = level.getBlockState(pos);

        return state.is(Blocks.AIR)
            || state.is(Blocks.CAVE_AIR)
            || state.is(Blocks.VOID_AIR)
            || state.is(DeepDiving.LAMP_LIGHT.get())
            || state.is(Blocks.WATER) && state.getFluidState().isSource();
    }

    private static double loadedBeamReach(ServerLevel level, Vec3 eye, Vec3 direction) {
        BlockPos start = BlockPos.containing(eye);

        for (double distance = .5; distance <= 12; distance += .5) {
            BlockPos end = BlockPos.containing(eye.add(direction.scale(distance)));

            if (level.isOutsideBuildHeight(end)) {
                return distance - .5;
            }

            for (int x = Math.min(start.getX() >> 4, end.getX() >> 4);
                    x <= Math.max(start.getX() >> 4, end.getX() >> 4);
                    x++) {
                for (int z = Math.min(start.getZ() >> 4, end.getZ() >> 4);
                        z <= Math.max(start.getZ() >> 4, end.getZ() >> 4);
                        z++) {
                    if (!level.hasChunkAt(new BlockPos(x << 4, end.getY(), z << 4))) {
                        return distance - .5;
                    }
                }
            }
        }

        return 12;
    }

    public static boolean leased(ServerLevel level, BlockPos pos) {
        return LIGHTS.values().stream()
            .anyMatch(lease -> lease.level == level && lease.points.containsKey(pos));
    }

    private static void refresh(ServerLevel level, BlockPos pos) {
        int brightness = LIGHTS.values().stream()
            .filter(lease -> lease.level == level)
            .mapToInt(lease -> lease.points.getOrDefault(pos, 0))
            .max()
            .orElse(0);

        if (brightness == 0) {
            restore(level, pos);
            return;
        }

        if (!usable(level, pos)) {
            return;
        }

        var state = level.getBlockState(pos);

        boolean water = state.is(DeepDiving.LAMP_LIGHT.get())
                ? state.getValue(LightBlock.WATERLOGGED)
                : state.is(Blocks.WATER);

        var next = DeepDiving.LAMP_LIGHT.get()
            .defaultBlockState()
            .setValue(LightBlock.WATERLOGGED, water)
            .setValue(LightBlock.LEVEL, brightness);

        if (state != next) {
            level.setBlock(pos, next, 3);
        }
    }

    public static void restore(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) {
            // Its saved expiry tick cleans up when this chunk loads again.
            return;
        }

        var state = level.getBlockState(pos);

        if (!state.is(DeepDiving.LAMP_LIGHT.get())) {
            return;
        }

        level.setBlock(
            pos,
            state.getValue(LightBlock.WATERLOGGED)
                    ? Fluids.WATER.getSource(false).createLegacyBlock()
                    : Blocks.AIR.defaultBlockState(),
            3
        );
    }

    public static void release(UUID id) {
        Lease previous = LIGHTS.remove(id);

        if (previous != null) {
            for (BlockPos pos : previous.points.keySet()) {
                refresh(previous.level, pos);
            }
        }
    }

    /** Remove only the short, hidden Night Vision used by previous addon lamps. */
    private static void clearLegacyVision(ServerPlayer player) {
        var effect = player.getEffect(MobEffects.NIGHT_VISION);

        if (effect != null
            && effect.getAmplifier() == 0
            && effect.getDuration() > 0
            && effect.getDuration() <= 240
            && !effect.isAmbient()
            && !effect.isVisible()
            && !effect.showIcon()) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 5 == 0) {
            update(player);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        release(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        LIGHTS.clear();
    }

    private DivingLampService() {}
}
