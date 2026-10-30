package com.diggydwarff.deepdiving.chart;

import com.diggydwarff.deepdiving.DeepDiving;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;

/** A metal site sign retaining vanilla editing, ink, wax and placement behavior. */
public final class DiveSiteSignBlock extends StandingSignBlock {
    public static final WoodType METAL =
            WoodType.register(
                    new WoodType(
                            "deepdiving:dive_site",
                            net.minecraft.world.level.block.state.properties.BlockSetType.IRON));
    public static final MapCodec<StandingSignBlock> CODEC = simpleCodec(DiveSiteSignBlock::new);

    public DiveSiteSignBlock(Properties properties) {
        super(METAL, properties);
    }

    @Override
    public MapCodec<StandingSignBlock> codec() {
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
