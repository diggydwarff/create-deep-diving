package com.diggydwarff.deepdiving.equipment;

import com.diggydwarff.deepdiving.DeepDiving;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Store the actual item, including gas, backup preference, name and other components. */
public final class CylinderBlockEntity extends BlockEntity {
    private ItemStack cylinder = ItemStack.EMPTY;

    public CylinderBlockEntity(BlockPos pos, BlockState state) {
        super(DeepDiving.PLACED_CYLINDER.get(), pos, state);
    }

    public ItemStack item() {
        return cylinder.isEmpty() ? new ItemStack(getBlockState().getBlock()) : cylinder.copy();
    }

    public void setItem(ItemStack stack) {
        if (!(stack.getItem() instanceof CylinderItem item)
            || item.getBlock() != getBlockState().getBlock()) {
            return;
        }

        cylinder = stack.copyWithCount(1);
        setChanged();

        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("cylinder", item().save(provider));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);

        ItemStack loaded = ItemStack.parseOptional(provider, tag.getCompound("cylinder"));

        if (loaded.getItem() instanceof CylinderItem item
            && item.getBlock() == getBlockState().getBlock()) {
            cylinder = loaded.copyWithCount(1);
        } else {
            cylinder = ItemStack.EMPTY;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
