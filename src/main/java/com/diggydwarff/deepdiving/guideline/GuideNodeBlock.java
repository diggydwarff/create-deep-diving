package com.diggydwarff.deepdiving.guideline;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.shapes.*;

/** Tiny waterloggable metal pin on floors, walls or ceilings. */
public final class GuideNodeBlock extends DirectionalBlock
        implements EntityBlock, SimpleWaterloggedBlock {
    public static final MapCodec<GuideNodeBlock> CODEC = simpleCodec(GuideNodeBlock::new);

    public GuideNodeBlock(Properties properties) {
        super(properties);
        registerDefaultState(
            defaultBlockState()
                .setValue(FACING, Direction.UP)
                .setValue(BlockStateProperties.WATERLOGGED, false)
        );
    }

    @Override
    protected MapCodec<GuideNodeBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BlockStateProperties.WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState()
            .setValue(FACING, context.getClickedFace())
            .setValue(
                BlockStateProperties.WATERLOGGED,
                context.getLevel()
                    .getFluidState(context.getClickedPos())
                    .is(Fluids.WATER)
            );

        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction face = state.getValue(FACING);

        BlockPos support = pos.relative(face.getOpposite());

        return level.getBlockState(support).isFaceSturdy(level, support, face);
    }

    @Override
    protected BlockState updateShape(
        BlockState state,
        Direction side,
        BlockState other,
        LevelAccessor level,
        BlockPos pos,
        BlockPos otherPos
    ) {
        if (!state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }

        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        return super.updateShape(state, side, other, level, pos, otherPos);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    protected VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        return switch (state.getValue(FACING)) {
            case UP -> box(6.5, 0, 6.5, 9.5, 3, 9.5);
            case DOWN -> box(6.5, 13, 6.5, 9.5, 16, 9.5);
            case NORTH -> box(6.5, 6.5, 13, 9.5, 9.5, 16);
            case SOUTH -> box(6.5, 6.5, 0, 9.5, 9.5, 3);
            case EAST -> box(0, 6.5, 6.5, 3, 9.5, 9.5);
            case WEST -> box(13, 6.5, 6.5, 16, 9.5, 9.5);
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GuideNodeBlockEntity(pos, state);
    }

    private void detach(Level level, BlockPos pos, boolean refund) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        for (GuideLine line : GuideNetwork.get(server).removeNode(server, pos)) {
            if (refund) {
                popResource(level, pos, GuideLineItem.refund(line));
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(
        Level level,
        BlockPos pos,
        BlockState state,
        Player player
    ) {
        detach(level, pos, !player.isCreative());
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(
        BlockState state,
        Level level,
        BlockPos pos,
        BlockState next,
        boolean moving
    ) {
        if (state.getBlock() != next.getBlock()) {
            detach(level, pos, true);
        }

        super.onRemove(state, level, pos, next, moving);
    }
}
