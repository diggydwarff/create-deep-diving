package com.diggydwarff.deepdiving.diving;

import com.diggydwarff.deepdiving.network.DivingPackets;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.WeakHashMap;

/** Share only the active buoyancy pose with observers, not private computer telemetry. */
public final class TreadingService {
    private static final Map<ServerPlayer, DivingPackets.TreadingPose> previous = new WeakHashMap<>();

    public static DivingPackets.TreadingPose pose(ServerPlayer player) {
        var state = DiverService.state(player);

        boolean active = player.isAlive()
            && player.isInWater()
            && !state.blackedOut
            && (state.depthHold || state.finsFloating);

        Boolean swimming = FinsService.swimmingOverride(player);

        return new DivingPackets.TreadingPose(
            player.getId(),
            active,
            swimming != null,
            Boolean.TRUE.equals(swimming)
        );
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        var pose = pose(player);

        if (pose.equals(previous.put(player, pose))) {
            return;
        }

        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, pose);
    }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer observer
            && event.getTarget() instanceof ServerPlayer diver) {
            PacketDistributor.sendToPlayer(observer, pose(diver));
        }
    }

    private TreadingService() {}
}
