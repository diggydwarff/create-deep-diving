package com.diggydwarff.deepdiving.equipment;

import com.diggydwarff.deepdiving.DeepDiving;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** A placed cylinder is the same equipment item, not a second gas container. */
public final class CylinderBlock extends HorizontalDirectionalBlock
        implements EntityBlock, SimpleWaterloggedBlock, IWrenchable {
    public static final MapCodec<CylinderBlock> CODEC = simpleCodec(CylinderBlock::new);

    public CylinderBlock(Properties properties) {
        super(properties);
        registerDefaultState(
            defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(BlockStateProperties.WATERLOGGED, false)
        );
    }

    @Override
    protected MapCodec<CylinderBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BlockStateProperties.WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(
                BlockStateProperties.WATERLOGGED,
                context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER)
            );
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CylinderBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(
        Level level,
        BlockPos pos,
        BlockState state,
        LivingEntity player,
        ItemStack stack
    ) {
        if (level.getBlockEntity(pos) instanceof CylinderBlockEntity cylinder) {
            cylinder.setItem(stack);
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(
        BlockState state,
        BlockGetter level,
        BlockPos pos,
        CollisionContext context
    ) {
        if (asItem() instanceof CylinderItem item && item.emergency) {
            return Block.box(4, 0, 4, 12, 12, 12);
        }

        if (asItem() == DeepDiving.HIGH_CAPACITY.get()) {
            return state.getValue(FACING).getAxis() == Direction.Axis.X
                    ? Block.box(3, 0, 1, 13, 16, 15)
                    : Block.box(1, 0, 3, 15, 16, 13);
        }

        return Block.box(3, 0, 3, 13, 16, 13);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(
        BlockState state,
        Direction side,
        BlockState neighbor,
        LevelAccessor level,
        BlockPos pos,
        BlockPos neighborPos
    ) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof CylinderBlockEntity cylinder
                ? cylinder.item()
                : new ItemStack(this);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder context) {
        BlockEntity entity = context.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

        return List.of(
            entity instanceof CylinderBlockEntity cylinder
                    ? cylinder.item()
                    : new ItemStack(this)
        );
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hit
    ) {
        if (!player.getMainHandItem().isEmpty() || !player.mayBuild()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            pickup(level, pos, state, player, !player.isShiftKeyDown());
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        if (context.getPlayer() == null || !context.getPlayer().mayBuild()) {
            return InteractionResult.PASS;
        }

        if (!context.getLevel().isClientSide) {
            pickup(context.getLevel(), context.getClickedPos(), state, context.getPlayer(), false);
            IWrenchable.playRemoveSound(context.getLevel(), context.getClickedPos());
        }

        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    private void pickup(
        Level level,
        BlockPos pos,
        BlockState state,
        Player player,
        boolean wear
    ) {
        ItemStack stack = getCloneItemStack(level, pos, state);

        level.destroyBlock(pos, false);

        if (wear && player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
            player.setItemSlot(EquipmentSlot.CHEST, stack);
        } else if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
