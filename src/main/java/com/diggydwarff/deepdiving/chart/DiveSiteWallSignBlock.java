package com.diggydwarff.deepdiving.chart;

import com.diggydwarff.deepdiving.DeepDiving;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Wall placement of the same site-sign item, sharing text, drops and map behavior. */
public final class DiveSiteWallSignBlock extends WallSignBlock {
    public static final MapCodec<WallSignBlock> CODEC = simpleCodec(DiveSiteWallSignBlock::new);

    public DiveSiteWallSignBlock(Properties properties) {
        super(DiveSiteSignBlock.METAL, properties);
    }

    @Override
    public MapCodec<WallSignBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DiveSiteSignBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, DeepDiving.SITE_SIGN_ENTITY.get(), SignBlockEntity::tick);
    }
}
