package com.diggydwarff.deepdiving.sign;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;

/** Thin waterloggable plate. Mount geometry follows Trailbound's MIT sign implementation. */
public final class DivePanelBlock extends DirectionalBlock
        implements EntityBlock, SimpleWaterloggedBlock {
    public static final MapCodec<DivePanelBlock> CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                            DiveSignDesign.CODEC
                                                    .fieldOf("design")
                                                    .forGetter(block -> block.design),
                                            propertiesCodec())
                                    .apply(instance, DivePanelBlock::new));
    public static final IntegerProperty ROLL = IntegerProperty.create("roll", 0, 3);
    public static final BooleanProperty POST_MOUNTED = BooleanProperty.create("post_mounted");
    private static final VoxelShape[] SHAPES = {
        box(0, 15, 0, 16, 16, 16), box(0, 0, 0, 16, 1, 16), box(0, 0, 15, 16, 16, 16),
        box(0, 0, 0, 16, 16, 1), box(15, 0, 0, 16, 16, 16), box(0, 0, 0, 1, 16, 16)
    };
    public final DiveSignDesign design;

    public DivePanelBlock(DiveSignDesign design, Properties properties) {
        super(properties);
        this.design = design;
        registerDefaultState(
                stateDefinition
                        .any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(ROLL, 0)
                        .setValue(POST_MOUNTED, false)
                        .setValue(BlockStateProperties.WATERLOGGED, false));
    }

    @Override
    public MapCodec<DivePanelBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ROLL, POST_MOUNTED, BlockStateProperties.WATERLOGGED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DivePanelBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        BlockPos support = context.getClickedPos().relative(face.getOpposite());
        int roll =
                face.getAxis() == Direction.Axis.Y && context.getPlayer() != null
                        ? context.getPlayer().getDirection().get2DDataValue()
                        : 0;
        BlockState state =
                defaultBlockState()
                        .setValue(FACING, face)
                        .setValue(ROLL, roll)
                        .setValue(
                                POST_MOUNTED,
                                DiveSignSupports.inset(
                                        context.getLevel().getBlockState(support), face))
                        .setValue(
                                BlockStateProperties.WATERLOGGED,
                                context.getLevel().getFluidState(context.getClickedPos()).getType()
                                        == Fluids.WATER);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction face = state.getValue(FACING);
        BlockPos support = pos.relative(face.getOpposite());
        return DiveSignSupports.supports(level.getBlockState(support), level, support, face);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction side,
            BlockState neighbor,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos) {
        if (side == state.getValue(FACING).getOpposite()) {
            if (!state.canSurvive(level, pos))
                return state.getValue(BlockStateProperties.WATERLOGGED)
                        ? Blocks.WATER.defaultBlockState()
                        : Blocks.AIR.defaultBlockState();
            state =
                    state.setValue(
                            POST_MOUNTED, DiveSignSupports.inset(neighbor, state.getValue(FACING)));
        }
        if (state.getValue(BlockStateProperties.WATERLOGGED))
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction face = state.getValue(FACING);
        VoxelShape shape = SHAPES[face.ordinal()];
        return state.getValue(POST_MOUNTED)
                ? shape.move(-face.getStepX() * 6 / 16.0, 0, -face.getStepZ() * 6 / 16.0)
                : shape;
    }

    @Override
    public void setPlacedBy(
            Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack item) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DivePanelBlockEntity panel)
            panel.loadItem(item);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof DivePanelBlockEntity panel
                ? panel.itemWithSettings()
                : new ItemStack(this);
    }

    private boolean mayChange(Level level, BlockPos pos, Player player, Direction face) {
        return player.mayBuild()
                && level.mayInteract(player, pos)
                && !EventHooks.onBlockPlace(
                        player, BlockSnapshot.create(level.dimension(), level, pos), face);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack item,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        boolean glow = item.is(Items.GLOW_INK_SAC), ink = item.is(Items.INK_SAC);
        if (!glow && !ink && !(item.getItem() instanceof DyeItem))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof DivePanelBlockEntity panel))
            return ItemInteractionResult.FAIL;
        if (!level.isClientSide) {
            if (!mayChange(level, pos, player, hit.getDirection()))
                return ItemInteractionResult.FAIL;
            if (item.getItem() instanceof DyeItem dye) {
                if (panel.color() == dye.getDyeColor().getId())
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                panel.setSettings(panel.customCaption(), dye.getDyeColor().getId(), panel.arrow());
            } else {
                if (panel.glowing() == glow)
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                panel.setGlowing(glow);
            }
            item.consume(1, player);
            level.playSound(
                    null,
                    pos,
                    glow
                            ? SoundEvents.GLOW_INK_SAC_USE
                            : ink ? SoundEvents.INK_SAC_USE : SoundEvents.DYE_USE,
                    SoundSource.BLOCKS,
                    1,
                    1);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.mayBuild()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (!mayChange(level, pos, player, hit.getDirection())) return InteractionResult.FAIL;
            if (player.isShiftKeyDown()) level.setBlock(pos, state.cycle(ROLL), 3);
            else if (player instanceof ServerPlayer server
                    && level.getBlockEntity(pos) instanceof DivePanelBlockEntity panel)
                PacketDistributor.sendToPlayer(server, panel.beginEditing(server));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        Direction face = state.getValue(FACING);
        if (face.getAxis() != Direction.Axis.Y)
            return state.setValue(FACING, rotation.rotate(face));
        int quarter =
                switch (rotation) {
                    case NONE -> 0;
                    case CLOCKWISE_90 -> 1;
                    case CLOCKWISE_180 -> 2;
                    case COUNTERCLOCKWISE_90 -> 3;
                };
        return state.setValue(
                ROLL, (state.getValue(ROLL) + (face == Direction.UP ? quarter : 4 - quarter)) % 4);
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        Direction before = state.getValue(FACING), after = mirror.mirror(before);
        int roll = state.getValue(ROLL);
        if (before.getAxis() == Direction.Axis.Y && mirror != Mirror.NONE)
            roll = (4 - roll + (mirror == Mirror.LEFT_RIGHT ? 2 : 0)) % 4;
        else if (before != after) roll = (4 - roll) % 4;
        return state.setValue(FACING, after).setValue(ROLL, roll);
    }
}
