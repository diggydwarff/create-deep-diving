package com.diggydwarff.deepdiving.sediment;

import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.network.SedimentPulse;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.Map;

/** Server observations only. No particles, fog, terrain changes or persistent cloud state. */
public final class SedimentService {
    private record Position(ResourceKey<Level> dimension, BlockPos block) {}

    private static final Map<Position, BlockState> pendingBreaks = new LinkedHashMap<>();

    private static final SedimentMovement movement = new SedimentMovement();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void broken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !SedimentRules.enabled()
            || !DivingConfig.SEDIMENT_MINING.get() || !SedimentRules.loose(event.getState())
            || pendingBreaks.size() >= 256 || !hasViewer(level, event.getPos())
            || SedimentRules.exposedWater(level, event.getPos()).isEmpty()) {
            return;
        }

        pendingBreaks.put(new Position(level.dimension(), event.getPos().immutable()), event.getState());
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        if (SedimentRules.enabled() && DivingConfig.SEDIMENT_MINING.get()) {
            pendingBreaks.forEach((position, previousState) -> {
                ServerLevel level = event.getServer().getLevel(position.dimension());

                // Verify removal after all break listeners have run. A denied or canceled
                // break must never create a cloud, including in Creative.
                if (level != null && level.hasChunkAt(position.block())
                    && !level.getBlockState(position.block()).equals(previousState)) {
                    SedimentRules.exposedWater(level, position.block())
                        .ifPresent(water -> broadcast(level, water, false, 1));
                }
            });
        }

        pendingBreaks.clear();
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !SedimentRules.enabled()) {
            return;
        }

        var motion = movement.sample(player);

        if (!motion.landed()
            && (player.tickCount + player.getId()) % DivingConfig.SEDIMENT_INTERVAL.get() != 0) {
            return;
        }

        if (!hasViewer(player.serverLevel(), player.blockPosition())) {
            return;
        }

        SedimentRules.disturbedBed(player, motion.travel()).ifPresent(water ->
            broadcast(player.serverLevel(), water, true, SedimentRules.intensity(player, motion.travel())));
    }

    private static boolean hasViewer(ServerLevel level, BlockPos position) {
        double range = DivingConfig.SEDIMENT_RANGE.get();

        return level.players().stream().anyMatch(viewer -> viewer.distanceToSqr(position.getCenter()) <= range * range);
    }

    private static void broadcast(ServerLevel level, BlockPos position, boolean moving, float intensity) {
        if (intensity <= 0) {
            return;
        }

        SedimentPulse pulse = new SedimentPulse(level.dimension().location(), position, moving, intensity);

        double range = DivingConfig.SEDIMENT_RANGE.get();

        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(position.getCenter()) <= range * range) {
                PacketDistributor.sendToPlayer(viewer, pulse);
            }
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        pendingBreaks.clear();
        movement.clear();
    }

    private SedimentService() {}
}
