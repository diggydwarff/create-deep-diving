package com.diggydwarff.deepdiving.equipment;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.network.LampState;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayDeque;

/** Activation telemetry plus bounded migration of old saved light blocks. */
public final class DivingLampService {
    private static final ArrayDeque<Cleanup> CLEANUP = new ArrayDeque<>();

    private static LampState state(ServerPlayer player) {
        boolean active = player.isAlive() && !player.isSpectator()
            && player.getPersistentData().getBoolean("deepdiving.lamp")
            && BreathingEquipment.carries(player, DeepDiving.LAMP.get());

        boolean held = player.getMainHandItem().is(DeepDiving.LAMP.get())
            || player.getOffhandItem().is(DeepDiving.LAMP.get());

        return new LampState(player.getUUID(), player.level().dimension().location(), active, held);
    }

    public static void update(ServerPlayer player) {
        LampState state = state(player);

        if (state.active()) {
            clearLegacyVision(player);
        }

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, state);
    }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer
            && event.getTarget() instanceof ServerPlayer diver) {
            PacketDistributor.sendToPlayer(viewer, state(diver));
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 5 == 0) {
            update(player);
        }
    }

    /** Keep the registry entry so saved worlds can deserialize and remove old lights. */
    public static void restore(ServerLevel level, BlockPos position) {
        if (!level.hasChunkAt(position)) {
            return;
        }

        var previous = level.getBlockState(position);

        if (previous.is(DeepDiving.LAMP_LIGHT.get())) {
            level.setBlock(position, previous.getValue(LightBlock.WATERLOGGED)
                ? Fluids.WATER.getSource(false).createLegacyBlock() : Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @SubscribeEvent
    public static void loaded(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            ChunkPos position = chunk.getPos();

            // Notifications may arrive away from the server thread. Inspect and
            // mutate the saved blocks only after returning to that thread.
            level.getServer().execute(() -> CLEANUP.addLast(new Cleanup(level, position)));
        }
    }

    @SubscribeEvent
    public static void migrate(ServerTickEvent.Post event) {
        int scanned = 0;

        int restored = 0;

        int sections = 0;

        while (!CLEANUP.isEmpty() && scanned < 4096 && restored < 128 && sections < 64) {
            Cleanup task = CLEANUP.peekFirst();

            LevelChunk chunk = task.level.getChunkSource().getChunkNow(task.position.x, task.position.z);

            if (chunk == null || task.section >= chunk.getSections().length) {
                CLEANUP.removeFirst();
                continue;
            }

            var section = chunk.getSections()[task.section];

            if (task.cell == 0) {
                sections++;
            }

            if (!section.maybeHas(state -> state.is(DeepDiving.LAMP_LIGHT.get()))) {
                task.section++;
                task.cell = 0;
                continue;
            }

            int index = task.cell++;

            int x = index & 15;

            int z = index >> 4 & 15;

            int y = index >> 8;

            scanned++;

            if (section.getBlockState(x, y, z).is(DeepDiving.LAMP_LIGHT.get())) {
                restore(task.level, new BlockPos(task.position.getMinBlockX() + x,
                    chunk.getSectionYFromSectionIndex(task.section) * 16 + y,
                    task.position.getMinBlockZ() + z));
                restored++;
            }

            if (task.cell == 4096) {
                task.section++;
                task.cell = 0;
            }
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        CLEANUP.clear();
    }

    private static void clearLegacyVision(ServerPlayer player) {
        var effect = player.getEffect(MobEffects.NIGHT_VISION);

        if (effect != null && effect.getAmplifier() == 0 && effect.getDuration() > 0
            && effect.getDuration() <= 240 && !effect.isAmbient()
            && !effect.isVisible() && !effect.showIcon()) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    private static final class Cleanup {
        private final ServerLevel level;

        private final ChunkPos position;

        private int section;

        private int cell;

        private Cleanup(ServerLevel level, ChunkPos position) {
            this.level = level;
            this.position = position;
        }
    }

    private DivingLampService() {}
}
