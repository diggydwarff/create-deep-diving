package com.diggydwarff.deepdiving.machine;

import com.diggydwarff.deepdiving.DeepDiving;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class MachineBlock extends HorizontalKineticBlock
        implements IBE<GasMachine>, IWrenchable {
    public enum Kind {
        COMPRESSOR,
        SEPARATOR,
        BLENDER,
        STORAGE
    }

    public final Kind kind;

    public MachineBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public Class<GasMachine> getBlockEntityClass() {
        return GasMachine.class;
    }

    @Override
    public BlockEntityType<? extends GasMachine> getBlockEntityType() {
        return DeepDiving.MACHINE.get();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(
        LevelReader level,
        BlockPos pos,
        BlockState state,
        Direction face
    ) {
        return kind != Kind.STORAGE && face == state.getValue(HORIZONTAL_FACING).getOpposite();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);

        Direction drive = getPreferredHorizontalFacing(context);

        // Point the rear socket at an existing motor/shaft, as other Create machines do.
        if (kind != Kind.STORAGE
            && drive != null
            && (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown())) {
            state = state.setValue(HORIZONTAL_FACING, drive.getOpposite());
        }

        return state;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        InteractionResult result = super.onWrenched(state, context);

        if (!context.getLevel().isClientSide && result.consumesAction()) {
            context.getLevel().invalidateCapabilities(context.getClickedPos());
            context.getLevel().updateNeighborsAt(context.getClickedPos(), this);
        }

        return result;
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hit
    ) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof GasMachine machine) {
            player.openMenu(machine, machine::writeMenuData);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onPlace(
        BlockState state,
        Level level,
        BlockPos pos,
        BlockState old,
        boolean moving
    ) {
        super.onPlace(state, level, pos, old, moving);

        if (!level.isClientSide) {
            level.invalidateCapabilities(pos);
        }
    }

    @Override
    public void onRemove(
        BlockState state,
        Level level,
        BlockPos pos,
        BlockState next,
        boolean moving
    ) {
        if (state.getBlock() != next.getBlock()
            && level.getBlockEntity(pos) instanceof GasMachine machine) {
            for (int slot = 0; slot < machine.inventory.getSlots(); slot++) {
                popResource(level, pos, machine.inventory.getStackInSlot(slot));
                machine.inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }

        super.onRemove(state, level, pos, next, moving);
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (context.getPlayer() == null || !context.getPlayer().mayBuild()) {
            return InteractionResult.PASS;
        }

        BlockPos pos = context.getClickedPos();

        if (context.getLevel().getBlockEntity(pos) instanceof GasMachine machine) {
            ItemStack item = new ItemStack(this);

            var cargo = machine.saveCargo(context.getLevel().registryAccess());

            cargo.putString("id", "deepdiving:gas_machine");
            item.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(cargo));

            for (int slot = 0; slot < machine.inventory.getSlots(); slot++) {
                machine.inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }

            context.getLevel().destroyBlock(pos, false);

            if (!context.getPlayer().getInventory().add(item)) {
                context.getPlayer().drop(item, false);
            }

            IWrenchable.playRemoveSound(context.getLevel(), pos);
        }

        return InteractionResult.SUCCESS;
    }
}
