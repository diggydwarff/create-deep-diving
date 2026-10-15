package com.diggydwarff.deepdiving.equipment;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Migration-only registry entry. New lamps never place this block. */
public final class LampLightBlock extends LightBlock {
    public LampLightBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LEVEL, 10));
    }

    @Override
    public void onPlace(
        BlockState state,
        Level level,
        BlockPos pos,
        BlockState old,
        boolean moving
    ) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 20);
        }
    }

    @Override
    public void tick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random
    ) {
        DivingLampService.restore(level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hit
    ) {
        return InteractionResult.PASS;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
