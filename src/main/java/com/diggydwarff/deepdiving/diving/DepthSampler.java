package com.diggydwarff.deepdiving.diving;

import com.diggydwarff.deepdiving.config.DivingConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Search loaded water columns only; never force a chunk load to measure pressure. */
public final class DepthSampler {
    private record Sample(
        ResourceKey<Level> dimension,
        int x,
        int z,
        int surface,
        long expires
    ) {}

    private static final Map<UUID, Sample> CACHE = new HashMap<>();

    public static double pressure(double depth, double blocksPerAtmosphere) {
        return 1 + Math.max(0, depth) / blocksPerAtmosphere;
    }

    public static double depth(ServerPlayer player) {
        if (!player.isEyeInFluid(FluidTags.WATER)) {
            return 0;
        }

        var level = player.serverLevel();

        var profile = DivingConfig.pressureProfile(level.dimension().location());

        if (profile.surfaceY() != null) {
            return Math.max(0, profile.surfaceY() - player.getEyeY());
        }

        BlockPos eye = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());

        Sample cached = CACHE.get(player.getUUID());

        if (cached != null
            && cached.dimension == level.dimension()
            && cached.x == eye.getX()
            && cached.z == eye.getZ()
            && cached.expires > level.getGameTime()
            && cached.surface > eye.getY()) {
            return Math.max(0, cached.surface - player.getEyeY());
        }

        BlockPos.MutableBlockPos cursor = eye.mutable();

        int searchTop = Math.min(level.getMaxBuildHeight(), eye.getY() + DivingConfig.SURFACE_SEARCH_LIMIT.get());

        // A roofed flooded space inherits the column's pressure head rather than treating its roof
        // as a surface.
        int waterTop = eye.getY() + 1;

        for (int y = eye.getY(); y < searchTop; y++) {
            cursor.setY(y);

            if (!level.hasChunkAt(cursor)) {
                break;
            }

            var fluid = level.getFluidState(cursor);

            if (fluid.is(FluidTags.WATER)) {
                waterTop = y + 1;
            } else if (level.getBlockState(cursor).isAir()) {
                break;
            }
        }

        CACHE.put(
            player.getUUID(),
            new Sample(
                level.dimension(),
                eye.getX(),
                eye.getZ(),
                waterTop,
                level.getGameTime() + 100
            )
        );
        return Math.max(0, waterTop - player.getEyeY());
    }

    public static void forget(UUID id) {
        CACHE.remove(id);
    }

    public static void clear() {
        CACHE.clear();
    }

    private DepthSampler() {}
}
