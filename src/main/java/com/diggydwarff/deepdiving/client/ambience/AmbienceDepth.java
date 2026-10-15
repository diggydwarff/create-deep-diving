package com.diggydwarff.deepdiving.client.ambience;

import com.diggydwarff.deepdiving.client.ClientRegistration;
import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.diving.DepthSampler;
import com.diggydwarff.deepdiving.network.DivingPackets;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

/** Prefer computer telemetry, with a cached copy of its existing search for unequipped Creative. */
final class AmbienceDepth {
    private DivingPackets.Snapshot snapshot;

    private double receivedEyeY;

    private long receivedTick;

    private BlockPos column;

    private int surface;

    private long surfaceExpires;

    double sample(Player player) {
        long now = player.level().getGameTime();

        var current = ClientRegistration.snapshot;

        if (current != snapshot) {
            snapshot = current;
            receivedEyeY = player.getEyeY();
            receivedTick = now;
        }

        if (snapshot != null && snapshot.underwater() && now - receivedTick < 60) {
            // Telemetry arrives once a second. Follow vertical movement between packets.
            return Math.max(0, snapshot.depth() + receivedEyeY - player.getEyeY());
        }

        var profile = DivingConfig.pressureProfile(player.level().dimension().location());

        if (profile.surfaceY() != null) {
            return Math.max(0, profile.surfaceY() - player.getEyeY());
        }

        BlockPos eye = BlockPos.containing(player.getEyePosition());

        if (column == null || column.getX() != eye.getX() || column.getZ() != eye.getZ()
            || surface <= eye.getY() || now >= surfaceExpires) {
            column = eye;
            surface = DepthSampler.findSurface(player.level(), eye);
            surfaceExpires = now + 100;
        }

        return Math.max(0, surface - player.getEyeY());
    }
}
