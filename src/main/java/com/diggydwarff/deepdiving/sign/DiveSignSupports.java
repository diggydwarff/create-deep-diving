package com.diggydwarff.deepdiving.sign;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Mounting rules adapted from Trailbound; see META-INF/licenses/trailbound-signs-MIT.txt. */
public final class DiveSignSupports {
    public static boolean fence(BlockState state) {
        return state.getBlock() instanceof FenceBlock || state.is(BlockTags.FENCES);
    }

    public static boolean supports(
            BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return fence(state) || state.isFaceSturdy(level, pos, face);
    }

    public static boolean inset(BlockState state, Direction face) {
        return fence(state) && face.getAxis() != Direction.Axis.Y;
    }

    private DiveSignSupports() {}
}
