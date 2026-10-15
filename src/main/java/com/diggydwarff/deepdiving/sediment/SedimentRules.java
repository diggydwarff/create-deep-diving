package com.diggydwarff.deepdiving.sediment;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.compat.DeepSeasCompatibility;
import com.diggydwarff.deepdiving.config.DivingConfig;
import com.diggydwarff.deepdiving.diving.FinsService;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Read-only disturbance rules. Gas use, movement and terrain are owned by their existing systems. */
public final class SedimentRules {
    public static final TagKey<Block> LOOSE_SEDIMENT = TagKey.create(Registries.BLOCK, DeepDiving.id("loose_sediment"));

    public static boolean enabled() {
        return DivingConfig.SERVER.isLoaded() && DivingConfig.ENABLED.get() && DivingConfig.SEDIMENT_DISTURBANCES.get();
    }

    public static boolean loose(BlockState state) {
        return state.is(LOOSE_SEDIMENT)
            || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL)
            || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)
            || state.is(Blocks.MUD) || state.is(Blocks.CLAY)
            || state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL);
    }

    public static boolean water(Level level, BlockPos position) {
        return level.hasChunkAt(position) && level.getFluidState(position).is(FluidTags.WATER)
            && !DeepSeasCompatibility.inDryCompartment(level, position.getCenter());
    }

    public static Optional<BlockPos> exposedWater(Level level, BlockPos position) {
        // A mined seabed block sits below the water. Start the plume in exposed water,
        // rather than waiting for the removed block's center to fill on a later tick.
        if (water(level, position.above())) {
            return Optional.of(position.above());
        }

        if (water(level, position)) {
            return Optional.of(position);
        }

        for (Direction direction : Direction.values()) {
            BlockPos neighbor = position.relative(direction);

            if (water(level, neighbor)) {
                return Optional.of(neighbor);
            }
        }

        return Optional.empty();
    }

    public static Optional<BlockPos> disturbedBed(Player player, Vec3 travel) {
        if (!DivingConfig.SEDIMENT_MOVEMENT.get() || !player.isAlive() || player.isSpectator()
            || player.isPassenger() || !player.isInWater() || DeepSeasCompatibility.inDryCompartment(player)) {
            return Optional.empty();
        }

        boolean grounded = player.onGround() || player.verticalCollisionBelow;

        double minimum = SedimentMath.minimumSpeed(DivingConfig.SEDIMENT_MINIMUM_SPEED.get(), grounded);

        if (travel.lengthSqr() < minimum * minimum) {
            return Optional.empty();
        }

        Level level = player.level();

        BlockPos start = BlockPos.containing(player.getX(), player.getY() + .15, player.getZ());

        int searchDepth = (int) Math.ceil(DivingConfig.SEDIMENT_HEIGHT.get()) + 1;

        for (int offset = 0; offset <= searchDepth; offset++) {
            BlockPos position = start.below(offset);

            if (!level.hasChunkAt(position)) {
                return Optional.empty();
            }

            BlockState state = level.getBlockState(position);

            if (loose(state)) {
                double clearance = player.getY() - (position.getY() + 1);

                if (clearance >= -.5 && clearance <= DivingConfig.SEDIMENT_HEIGHT.get()
                    && water(level, position.above())) {
                    return Optional.of(position.above());
                }

                return Optional.empty();
            }

            // A solid floor shields loose material below it. Plants do not.
            if (!state.getCollisionShape(level, position).isEmpty()) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    public static float intensity(Player player, Vec3 travel) {
        return (float) SedimentMath.movementIntensity(
            travel.length(),
            player.isShiftKeyDown() || !player.isSprinting(),
            FinsService.worn(player),
            DivingConfig.SEDIMENT_CAREFUL_MULTIPLIER.get(),
            DivingConfig.SEDIMENT_FINS_MULTIPLIER.get()
        );
    }

    private SedimentRules() {}
}
